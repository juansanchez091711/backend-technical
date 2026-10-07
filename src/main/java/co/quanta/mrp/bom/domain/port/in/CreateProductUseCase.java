package co.quanta.mrp.bom.domain.port.in;

import co.quanta.mrp.bom.domain.model.Product;
import reactor.core.publisher.Mono;

public interface CreateProductUseCase {

    Mono<Product> createProduct(String name);
}
