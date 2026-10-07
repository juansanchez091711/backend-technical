package co.quanta.mrp.bom.infrastructure.adapter.out.external;

import co.quanta.mrp.bom.domain.exception.ExternalServiceException;
import co.quanta.mrp.bom.domain.exception.ExternalServiceTimeoutException;
import co.quanta.mrp.bom.domain.model.Operator;
import co.quanta.mrp.bom.infrastructure.config.OperatorApiProperties;
import co.quanta.mrp.bom.infrastructure.config.WebClientConfig;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class JsonPlaceholderOperatorClientTest {

    private static final String USERS_JSON = """
            [
              {"id":1,"name":"Leanne Graham","username":"Bret","email":"Sincere@april.biz",
               "phone":"1-770-736-8031 x56442","address":{"city":"Gwenborough"},"company":{"name":"Romaguera-Crona"}},
              {"id":2,"name":"Ervin Howell","username":"Antonette","email":"Shanna@melissa.tv",
               "phone":"010-692-6593 x09125"}
            ]
            """;

    private MockWebServer server;
    private JsonPlaceholderOperatorClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        var properties = new OperatorApiProperties(
                server.url("/").toString(), Duration.ofSeconds(1), Duration.ofMillis(500));
        var webClient = new WebClientConfig().operatorsWebClient(WebClient.builder(), properties);
        client = new JsonPlaceholderOperatorClient(webClient, properties);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void mapsUsersToOperators() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(USERS_JSON));

        StepVerifier.create(client.fetchOperators())
                .expectNext(new Operator(1L, "Leanne Graham", "Sincere@april.biz", "1-770-736-8031 x56442"))
                .expectNext(new Operator(2L, "Ervin Howell", "Shanna@melissa.tv", "010-692-6593 x09125"))
                .verifyComplete();

        assertThat(server.takeRequest().getPath()).isEqualTo("/users");
    }

    @Test
    void serverErrorMapsToExternalServiceException() {
        server.enqueue(new MockResponse().setResponseCode(500));

        StepVerifier.create(client.fetchOperators())
                .expectErrorSatisfies(error -> assertThat(error)
                        .isExactlyInstanceOf(ExternalServiceException.class)
                        .hasMessageContaining("500"))
                .verify();
    }

    @Test
    void slowResponseMapsToTimeoutException() {
        server.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(USERS_JSON)
                .setHeadersDelay(2, TimeUnit.SECONDS));

        StepVerifier.create(client.fetchOperators())
                .expectError(ExternalServiceTimeoutException.class)
                .verify(Duration.ofSeconds(5));
    }

    @Test
    void unreachableServerMapsToExternalServiceException() throws IOException {
        server.shutdown();

        StepVerifier.create(client.fetchOperators())
                .expectError(ExternalServiceException.class)
                .verify(Duration.ofSeconds(5));
    }
}
