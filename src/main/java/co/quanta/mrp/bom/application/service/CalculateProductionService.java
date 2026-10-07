package co.quanta.mrp.bom.application.service;

import co.quanta.mrp.bom.domain.exception.InvalidQuantityException;
import co.quanta.mrp.bom.domain.exception.ProductNotFoundException;
import co.quanta.mrp.bom.domain.model.ProductionCalculation;
import co.quanta.mrp.bom.domain.port.in.CalculateProductionUseCase;
import co.quanta.mrp.bom.domain.port.out.BomItemRepositoryPort;
import co.quanta.mrp.bom.domain.port.out.ProductRepositoryPort;
import reactor.core.publisher.Mono;

public final class CalculateProductionService implements CalculateProductionUseCase {

    private final ProductRepositoryPort productRepository;
    private final BomItemRepositoryPort bomItemRepository;

    public CalculateProductionService(ProductRepositoryPort productRepository, BomItemRepositoryPort bomItemRepository) {
        this.productRepository = productRepository;
        this.bomItemRepository = bomItemRepository;
    }

    @Override
    public Mono<ProductionCalculation> calculate(Long productId, int quantity) {
        if (quantity <= 0) {
            return Mono.error(new InvalidQuantityException("Production quantity must be greater than 0"));
        }
        return productRepository.findById(productId)
                .switchIfEmpty(Mono.error(() -> new ProductNotFoundException(productId)))
                .flatMap(product -> bomItemRepository.findByProductId(productId)
                        .collectList()
                        .map(bom -> ProductionCalculation.of(product, quantity, bom)));
    }
}
