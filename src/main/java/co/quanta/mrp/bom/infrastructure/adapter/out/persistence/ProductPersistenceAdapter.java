package co.quanta.mrp.bom.infrastructure.adapter.out.persistence;

import co.quanta.mrp.bom.domain.exception.DuplicateResourceException;
import co.quanta.mrp.bom.domain.model.Product;
import co.quanta.mrp.bom.domain.port.out.ProductRepositoryPort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class ProductPersistenceAdapter implements ProductRepositoryPort {

    private final ProductR2dbcRepository repository;

    public ProductPersistenceAdapter(ProductR2dbcRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<Product> save(Product product) {
        return repository.save(PersistenceMapper.toEntity(product))
                .map(PersistenceMapper::toDomain)
                .onErrorMap(DataIntegrityViolationException.class,
                        e -> new DuplicateResourceException("Product " + product.name() + " already exists", e));
    }

    @Override
    public Mono<Product> findById(Long id) {
        return repository.findById(id).map(PersistenceMapper::toDomain);
    }

    @Override
    public Mono<Boolean> existsById(Long id) {
        return repository.existsById(id);
    }
}
