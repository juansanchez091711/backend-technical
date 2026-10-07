package co.quanta.mrp.bom.application.service;

import co.quanta.mrp.bom.domain.exception.ExternalServiceException;
import co.quanta.mrp.bom.domain.model.Operator;
import co.quanta.mrp.bom.domain.port.out.OperatorClientPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetOperatorsServiceTest {

    @Mock
    private OperatorClientPort operatorClient;

    @InjectMocks
    private GetOperatorsService service;

    @Test
    void returnsOperatorsFromClient() {
        var leanne = new Operator(1L, "Leanne Graham", "Sincere@april.biz", "1-770-736-8031");
        var ervin = new Operator(2L, "Ervin Howell", "Shanna@melissa.tv", "010-692-6593");
        when(operatorClient.fetchOperators()).thenReturn(Flux.just(leanne, ervin));

        StepVerifier.create(service.getOperators())
                .expectNext(leanne, ervin)
                .verifyComplete();
    }

    @Test
    void propagatesExternalServiceError() {
        when(operatorClient.fetchOperators()).thenReturn(Flux.error(new ExternalServiceException("down")));

        StepVerifier.create(service.getOperators())
                .expectError(ExternalServiceException.class)
                .verify();
    }
}
