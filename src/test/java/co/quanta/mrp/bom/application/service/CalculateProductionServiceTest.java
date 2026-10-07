package co.quanta.mrp.bom.application.service;

import co.quanta.mrp.bom.domain.exception.InvalidQuantityException;
import co.quanta.mrp.bom.domain.exception.ProductNotFoundException;
import co.quanta.mrp.bom.domain.model.BomItem;
import co.quanta.mrp.bom.domain.model.MaterialRequirement;
import co.quanta.mrp.bom.domain.model.Product;
import co.quanta.mrp.bom.domain.model.ProductionCalculation;
import co.quanta.mrp.bom.domain.port.out.BomItemRepositoryPort;
import co.quanta.mrp.bom.domain.port.out.ProductRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalculateProductionServiceTest {

    @Mock
    private ProductRepositoryPort productRepository;

    @Mock
    private BomItemRepositoryPort bomItemRepository;

    @InjectMocks
    private CalculateProductionService service;

    @Test
    void calculatesShoeExampleForHundredUnits() {
        when(productRepository.findById(1L)).thenReturn(Mono.just(new Product(1L, "Zapato")));
        when(bomItemRepository.findByProductId(1L)).thenReturn(Flux.just(
                new BomItem(1L, 1L, "Cuero", 2),
                new BomItem(2L, 1L, "Suela", 1),
                new BomItem(3L, 1L, "Cordones", 1)));

        StepVerifier.create(service.calculate(1L, 100))
                .expectNext(new ProductionCalculation("Zapato", 100, List.of(
                        new MaterialRequirement("Cuero", 200),
                        new MaterialRequirement("Suela", 100),
                        new MaterialRequirement("Cordones", 100))))
                .verifyComplete();
    }

    @Test
    void productWithoutBomYieldsEmptyMaterials() {
        when(productRepository.findById(1L)).thenReturn(Mono.just(new Product(1L, "Zapato")));
        when(bomItemRepository.findByProductId(1L)).thenReturn(Flux.empty());

        StepVerifier.create(service.calculate(1L, 5))
                .expectNext(new ProductionCalculation("Zapato", 5, List.of()))
                .verifyComplete();
    }

    @Test
    void failsWhenProductDoesNotExist() {
        when(productRepository.findById(99L)).thenReturn(Mono.empty());

        StepVerifier.create(service.calculate(99L, 100))
                .expectError(ProductNotFoundException.class)
                .verify();
        verifyNoInteractions(bomItemRepository);
    }

    @Test
    void rejectsNonPositiveQuantity() {
        StepVerifier.create(service.calculate(1L, 0))
                .expectError(InvalidQuantityException.class)
                .verify();
        verifyNoInteractions(productRepository, bomItemRepository);
    }
}
