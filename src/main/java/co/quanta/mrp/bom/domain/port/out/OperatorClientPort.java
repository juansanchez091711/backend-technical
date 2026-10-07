package co.quanta.mrp.bom.domain.port.out;

import co.quanta.mrp.bom.domain.model.Operator;
import reactor.core.publisher.Flux;

public interface OperatorClientPort {

    Flux<Operator> fetchOperators();
}
