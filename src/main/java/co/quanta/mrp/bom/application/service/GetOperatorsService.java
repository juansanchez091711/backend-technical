package co.quanta.mrp.bom.application.service;

import co.quanta.mrp.bom.domain.model.Operator;
import co.quanta.mrp.bom.domain.port.in.GetOperatorsUseCase;
import co.quanta.mrp.bom.domain.port.out.OperatorClientPort;
import reactor.core.publisher.Flux;

public final class GetOperatorsService implements GetOperatorsUseCase {

    private final OperatorClientPort operatorClient;

    public GetOperatorsService(OperatorClientPort operatorClient) {
        this.operatorClient = operatorClient;
    }

    @Override
    public Flux<Operator> getOperators() {
        return operatorClient.fetchOperators();
    }
}
