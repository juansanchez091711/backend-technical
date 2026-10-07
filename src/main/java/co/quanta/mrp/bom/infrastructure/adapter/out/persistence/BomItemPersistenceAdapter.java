package co.quanta.mrp.bom.infrastructure.adapter.out.persistence;

import co.quanta.mrp.bom.domain.exception.DuplicateResourceException;
import co.quanta.mrp.bom.domain.model.BomItem;
import co.quanta.mrp.bom.domain.port.out.BomItemRepositoryPort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class BomItemPersistenceAdapter implements BomItemRepositoryPort {

    private final BomItemR2dbcRepository repository;

    public BomItemPersistenceAdapter(BomItemR2dbcRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<BomItem> save(BomItem bomItem) {
        // The unique constraint is the final guard against concurrent inserts of the same material.
        return repository.save(PersistenceMapper.toEntity(bomItem))
                .map(PersistenceMapper::toDomain)
                .onErrorMap(DataIntegrityViolationException.class, e -> new DuplicateResourceException(
                        "Material " + bomItem.material() + " already exists in BOM of product " + bomItem.productId(), e));
    }

    @Override
    public Flux<BomItem> findByProductId(Long productId) {
        return repository.findByProductIdOrderByIdAsc(productId).map(PersistenceMapper::toDomain);
    }

    @Override
    public Mono<Boolean> existsByProductIdAndMaterial(Long productId, String material) {
        return repository.existsByProductIdAndMaterialIgnoreCase(productId, material);
    }
}
