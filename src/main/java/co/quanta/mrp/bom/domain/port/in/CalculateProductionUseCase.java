package co.quanta.mrp.bom.domain.port.in;

import co.quanta.mrp.bom.domain.model.ProductionCalculation;
import reactor.core.publisher.Mono;

public interface CalculateProductionUseCase {

    Mono<ProductionCalculation> calculate(Long productId, int quantity);
}
