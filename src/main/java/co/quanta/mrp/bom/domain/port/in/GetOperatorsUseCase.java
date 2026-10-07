package co.quanta.mrp.bom.domain.port.in;

import co.quanta.mrp.bom.domain.model.Operator;
import reactor.core.publisher.Flux;

public interface GetOperatorsUseCase {

    Flux<Operator> getOperators();
}
