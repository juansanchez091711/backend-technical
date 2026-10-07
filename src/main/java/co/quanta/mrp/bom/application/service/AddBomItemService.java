package co.quanta.mrp.bom.application.service;

import co.quanta.mrp.bom.domain.exception.DuplicateResourceException;
import co.quanta.mrp.bom.domain.exception.ProductNotFoundException;
import co.quanta.mrp.bom.domain.model.BomItem;
import co.quanta.mrp.bom.domain.port.in.AddBomItemUseCase;
import co.quanta.mrp.bom.domain.port.out.BomItemRepositoryPort;
import co.quanta.mrp.bom.domain.port.out.ProductRepositoryPort;
import reactor.core.publisher.Mono;

public final class AddBomItemService implements AddBomItemUseCase {

    private final ProductRepositoryPort productRepository;
    private final BomItemRepositoryPort bomItemRepository;

    public AddBomItemService(ProductRepositoryPort productRepository, BomItemRepositoryPort bomItemRepository) {
        this.productRepository = productRepository;
        this.bomItemRepository = bomItemRepository;
    }

    @Override
    public Mono<BomItem> addMaterial(Long productId, String material, int quantity) {
        return Mono.fromSupplier(() -> BomItem.create(productId, material, quantity))
                .flatMap(item -> productRepository.existsById(productId)
                        .filter(Boolean::booleanValue)
                        .switchIfEmpty(Mono.error(() -> new ProductNotFoundException(productId)))
                        .flatMap(found -> bomItemRepository.existsByProductIdAndMaterial(productId, item.material()))
                        .flatMap(duplicated -> duplicated
                                ? Mono.error(new DuplicateResourceException(
                                        "Material " + item.material() + " already exists in BOM of product " + productId))
                                : bomItemRepository.save(item)));
    }
}
