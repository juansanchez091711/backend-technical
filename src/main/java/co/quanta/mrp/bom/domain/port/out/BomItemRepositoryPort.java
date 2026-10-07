package co.quanta.mrp.bom.domain.port.out;

import co.quanta.mrp.bom.domain.model.BomItem;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface BomItemRepositoryPort {

    Mono<BomItem> save(BomItem bomItem);

    Flux<BomItem> findByProductId(Long productId);

    Mono<Boolean> existsByProductIdAndMaterial(Long productId, String material);
}
