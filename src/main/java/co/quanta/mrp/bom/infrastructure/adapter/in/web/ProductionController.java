package co.quanta.mrp.bom.infrastructure.adapter.in.web;

import co.quanta.mrp.bom.domain.port.in.CalculateProductionUseCase;
import co.quanta.mrp.bom.infrastructure.adapter.in.web.dto.ProductionCalculationResponse;
import co.quanta.mrp.bom.infrastructure.adapter.in.web.mapper.WebMapper;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/production")
public class ProductionController {

    private final CalculateProductionUseCase calculateProductionUseCase;

    public ProductionController(CalculateProductionUseCase calculateProductionUseCase) {
        this.calculateProductionUseCase = calculateProductionUseCase;
    }

    @GetMapping("/calculate")
    public Mono<ProductionCalculationResponse> calculate(@RequestParam @Positive Long productId,
                                                         @RequestParam @Positive Integer quantity) {
        return calculateProductionUseCase.calculate(productId, quantity)
                .map(WebMapper::toResponse);
    }
}
