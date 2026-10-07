package co.quanta.mrp.bom.infrastructure.adapter.out.external;

import co.quanta.mrp.bom.domain.exception.DomainException;
import co.quanta.mrp.bom.domain.exception.ExternalServiceException;
import co.quanta.mrp.bom.domain.exception.ExternalServiceTimeoutException;
import co.quanta.mrp.bom.domain.model.Operator;
import co.quanta.mrp.bom.domain.port.out.OperatorClientPort;
import co.quanta.mrp.bom.infrastructure.config.OperatorApiProperties;
import io.netty.handler.timeout.ReadTimeoutException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Component
public class JsonPlaceholderOperatorClient implements OperatorClientPort {

    private static final String USERS_PATH = "/users";

    private final WebClient webClient;
    private final Duration timeout;

    public JsonPlaceholderOperatorClient(@Qualifier("operatorsWebClient") WebClient webClient,
                                         OperatorApiProperties properties) {
        this.webClient = webClient;
        this.timeout = properties.readTimeout();
    }

    @Override
    public Flux<Operator> fetchOperators() {
        return webClient.get()
                .uri(USERS_PATH)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> Mono.error(new ExternalServiceException(
                        "Operators API responded with status " + response.statusCode().value())))
                .bodyToFlux(ExternalUserDto.class)
                .map(JsonPlaceholderOperatorClient::toDomain)
                .timeout(timeout)
                .onErrorMap(error -> !(error instanceof DomainException), JsonPlaceholderOperatorClient::translate);
    }

    private static Operator toDomain(ExternalUserDto dto) {
        return new Operator(dto.id(), dto.name(), dto.email(), dto.phone());
    }

    private static DomainException translate(Throwable error) {
        return isTimeout(error)
                ? new ExternalServiceTimeoutException("Operators API did not respond in time", error)
                : new ExternalServiceException("Operators API is unavailable: " + error.getMessage(), error);
    }

    private static boolean isTimeout(Throwable error) {
        for (var cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof TimeoutException || cause instanceof ReadTimeoutException) {
                return true;
            }
        }
        return false;
    }
}
