package co.quanta.mrp.bom.domain.port.in;

import co.quanta.mrp.bom.domain.model.BomItem;
import reactor.core.publisher.Mono;

public interface AddBomItemUseCase {

    Mono<BomItem> addMaterial(Long productId, String material, int quantity);
}
