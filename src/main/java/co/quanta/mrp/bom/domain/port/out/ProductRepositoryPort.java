package co.quanta.mrp.bom.domain.port.out;

import co.quanta.mrp.bom.domain.model.Product;
import reactor.core.publisher.Mono;

public interface ProductRepositoryPort {

    Mono<Product> save(Product product);

    Mono<Product> findById(Long id);

    Mono<Boolean> existsById(Long id);
}
