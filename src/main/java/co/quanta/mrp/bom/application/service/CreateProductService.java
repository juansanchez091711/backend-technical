package co.quanta.mrp.bom.application.service;

import co.quanta.mrp.bom.domain.model.Product;
import co.quanta.mrp.bom.domain.port.in.CreateProductUseCase;
import co.quanta.mrp.bom.domain.port.out.ProductRepositoryPort;
import reactor.core.publisher.Mono;

public final class CreateProductService implements CreateProductUseCase {

    private final ProductRepositoryPort productRepository;

    public CreateProductService(ProductRepositoryPort productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public Mono<Product> createProduct(String name) {
        // Deferred so invariant violations surface as error signals, not thrown exceptions.
        return Mono.fromSupplier(() -> Product.create(name))
                .flatMap(productRepository::save);
    }
}
