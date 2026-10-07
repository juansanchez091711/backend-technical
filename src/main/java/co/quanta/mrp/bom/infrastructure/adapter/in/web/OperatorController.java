package co.quanta.mrp.bom.infrastructure.adapter.in.web;

import co.quanta.mrp.bom.domain.port.in.GetOperatorsUseCase;
import co.quanta.mrp.bom.infrastructure.adapter.in.web.dto.OperatorResponse;
import co.quanta.mrp.bom.infrastructure.adapter.in.web.mapper.WebMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/operators")
public class OperatorController {

    private final GetOperatorsUseCase getOperatorsUseCase;

    public OperatorController(GetOperatorsUseCase getOperatorsUseCase) {
        this.getOperatorsUseCase = getOperatorsUseCase;
    }

    @GetMapping
    public Flux<OperatorResponse> getOperators() {
        return getOperatorsUseCase.getOperators().map(WebMapper::toResponse);
    }
}
