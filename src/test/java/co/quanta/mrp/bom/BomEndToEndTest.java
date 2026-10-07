package co.quanta.mrp.bom;

import co.quanta.mrp.bom.infrastructure.adapter.in.web.dto.ProductResponse;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BomEndToEndTest {

    private static final MockWebServer OPERATORS_API = new MockWebServer();

    static {
        try {
            OPERATORS_API.start();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @DynamicPropertySource
    static void operatorsApi(DynamicPropertyRegistry registry) {
        registry.add("quanta.operators-api.base-url", () -> OPERATORS_API.url("/").toString());
        registry.add("quanta.operators-api.read-timeout", () -> "1s");
    }

    @AfterAll
    static void stopServer() throws IOException {
        OPERATORS_API.shutdown();
    }

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shoeBomForHundredUnits() {
        var product = webTestClient.post().uri("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "Zapato E2E"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ProductResponse.class)
                .returnResult().getResponseBody();
        assertThat(product).isNotNull();

        addMaterial(product.id(), "Cuero", 2);
        addMaterial(product.id(), "Suela", 1);
        addMaterial(product.id(), "Cordones", 1);

        webTestClient.post().uri("/products/{id}/materials", product.id())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("material", "cuero", "quantity", 5))
                .exchange()
                .expectStatus().isEqualTo(409);

        webTestClient.get().uri("/production/calculate?productId={id}&quantity=100", product.id())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.product").isEqualTo("Zapato E2E")
                .jsonPath("$.quantity").isEqualTo(100)
                .jsonPath("$.materials[0].material").isEqualTo("Cuero")
                .jsonPath("$.materials[0].required").isEqualTo(200)
                .jsonPath("$.materials[1].material").isEqualTo("Suela")
                .jsonPath("$.materials[1].required").isEqualTo(100)
                .jsonPath("$.materials[2].material").isEqualTo("Cordones")
                .jsonPath("$.materials[2].required").isEqualTo(100);
    }

    @Test
    void duplicateProductReturns409() {
        var body = Map.of("name", "Bolso E2E");
        webTestClient.post().uri("/products").contentType(MediaType.APPLICATION_JSON).bodyValue(body)
                .exchange().expectStatus().isCreated();
        webTestClient.post().uri("/products").contentType(MediaType.APPLICATION_JSON).bodyValue(body)
                .exchange().expectStatus().isEqualTo(409);
    }

    @Test
    void unknownProductReturns404() {
        webTestClient.get().uri("/production/calculate?productId=987654&quantity=1")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody().jsonPath("$.path").isEqualTo("/production/calculate");
    }

    @Test
    void operatorsAreFetchedFromExternalApi() {
        OPERATORS_API.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("""
                        [{"id":1,"name":"Leanne Graham","username":"Bret","email":"Sincere@april.biz",
                          "phone":"1-770-736-8031","company":{"name":"Romaguera-Crona"}}]
                        """));

        webTestClient.get().uri("/operators")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].name").isEqualTo("Leanne Graham")
                .jsonPath("$[0].username").doesNotExist();
    }

    @Test
    void externalApiFailureReturns502() {
        OPERATORS_API.enqueue(new MockResponse().setResponseCode(503));

        webTestClient.get().uri("/operators")
                .exchange()
                .expectStatus().isEqualTo(502)
                .expectBody().jsonPath("$.status").isEqualTo(502);
    }

    private void addMaterial(Long productId, String material, int quantity) {
        webTestClient.post().uri("/products/{id}/materials", productId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("material", material, "quantity", quantity))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.productId").isEqualTo(productId)
                .jsonPath("$.material").isEqualTo(material)
                .jsonPath("$.quantity").isEqualTo(quantity);
    }
}
