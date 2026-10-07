package co.quanta.mrp.bom.infrastructure.adapter.out.persistence;

import co.quanta.mrp.bom.domain.exception.DuplicateResourceException;
import co.quanta.mrp.bom.domain.model.BomItem;
import co.quanta.mrp.bom.domain.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Import({ProductPersistenceAdapter.class, BomItemPersistenceAdapter.class})
class PersistenceAdaptersTest {

    @Autowired
    private ProductPersistenceAdapter productAdapter;

    @Autowired
    private BomItemPersistenceAdapter bomItemAdapter;

    @Autowired
    private DatabaseClient databaseClient;

    @BeforeEach
    void cleanDatabase() {
        StepVerifier.create(databaseClient.sql("DELETE FROM bom_item").then()
                        .then(databaseClient.sql("DELETE FROM product").then()))
                .verifyComplete();
    }

    @Test
    void savesAndFindsProduct() {
        StepVerifier.create(productAdapter.save(Product.create("Zapato"))
                        .flatMap(saved -> productAdapter.findById(saved.id())))
                .assertNext(found -> {
                    assertThat(found.id()).isNotNull();
                    assertThat(found.name()).isEqualTo("Zapato");
                })
                .verifyComplete();
    }

    @Test
    void missingProductIsEmptyAndDoesNotExist() {
        StepVerifier.create(productAdapter.findById(999L)).verifyComplete();
        StepVerifier.create(productAdapter.existsById(999L)).expectNext(false).verifyComplete();
    }

    @Test
    void duplicateProductNameMapsToDomainConflict() {
        StepVerifier.create(productAdapter.save(Product.create("Zapato"))
                        .then(productAdapter.save(Product.create("Zapato"))))
                .expectError(DuplicateResourceException.class)
                .verify();
    }

    @Test
    void savesBomItemsAndListsThemInInsertionOrder() {
        StepVerifier.create(productAdapter.save(Product.create("Zapato"))
                        .flatMapMany(p -> bomItemAdapter.save(BomItem.create(p.id(), "Cuero", 2))
                                .then(bomItemAdapter.save(BomItem.create(p.id(), "Suela", 1)))
                                .thenMany(bomItemAdapter.findByProductId(p.id())))
                        .map(BomItem::material))
                .expectNext("Cuero", "Suela")
                .verifyComplete();
    }

    @Test
    void existsByMaterialIgnoresCase() {
        StepVerifier.create(productAdapter.save(Product.create("Zapato"))
                        .flatMap(p -> bomItemAdapter.save(BomItem.create(p.id(), "Cuero", 2))
                                .then(bomItemAdapter.existsByProductIdAndMaterial(p.id(), "cuero"))))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void duplicateMaterialMapsToDomainConflict() {
        StepVerifier.create(productAdapter.save(Product.create("Zapato"))
                        .flatMap(p -> bomItemAdapter.save(BomItem.create(p.id(), "Cuero", 2))
                                .then(bomItemAdapter.save(BomItem.create(p.id(), "Cuero", 3)))))
                .expectError(DuplicateResourceException.class)
                .verify();
    }
}
