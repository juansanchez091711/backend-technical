package co.quanta.mrp.bom.infrastructure.adapter.in.web;

import co.quanta.mrp.bom.domain.exception.DuplicateResourceException;
import co.quanta.mrp.bom.domain.exception.ExternalServiceException;
import co.quanta.mrp.bom.domain.exception.ExternalServiceTimeoutException;
import co.quanta.mrp.bom.domain.exception.ProductNotFoundException;
import co.quanta.mrp.bom.domain.model.BomItem;
import co.quanta.mrp.bom.domain.model.MaterialRequirement;
import co.quanta.mrp.bom.domain.model.Operator;
import co.quanta.mrp.bom.domain.model.Product;
import co.quanta.mrp.bom.domain.model.ProductionCalculation;
import co.quanta.mrp.bom.domain.port.in.AddBomItemUseCase;
import co.quanta.mrp.bom.domain.port.in.CalculateProductionUseCase;
import co.quanta.mrp.bom.domain.port.in.CreateProductUseCase;
import co.quanta.mrp.bom.domain.port.in.GetOperatorsUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = {ProductController.class, ProductionController.class, OperatorController.class})
class ControllersWebTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private CreateProductUseCase createProductUseCase;

    @MockBean
    private AddBomItemUseCase addBomItemUseCase;

    @MockBean
    private CalculateProductionUseCase calculateProductionUseCase;

    @MockBean
    private GetOperatorsUseCase getOperatorsUseCase;

    @Test
    void createProductReturns201WithLocation() {
        when(createProductUseCase.createProduct("Zapato")).thenReturn(Mono.just(new Product(1L, "Zapato")));

        webTestClient.post().uri("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"name\":\"Zapato\"}")
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().location("/products/1")
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.name").isEqualTo("Zapato");
    }

    @Test
    void createProductWithBlankNameReturns400() {
        webTestClient.post().uri("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"name\":\"  \"}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.error").isEqualTo("Bad Request")
                .jsonPath("$.message").value(message -> assertThat((String) message).contains("name"))
                .jsonPath("$.path").isEqualTo("/products")
                .jsonPath("$.timestamp").exists();
        verifyNoInteractions(createProductUseCase);
    }

    @Test
    void createDuplicateProductReturns409() {
        when(createProductUseCase.createProduct("Zapato"))
                .thenReturn(Mono.error(new DuplicateResourceException("Product Zapato already exists")));

        webTestClient.post().uri("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"name\":\"Zapato\"}")
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.message").isEqualTo("Product Zapato already exists");
    }

    @Test
    void malformedJsonReturns400() {
        webTestClient.post().uri("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{not json")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void addMaterialReturns201() {
        when(addBomItemUseCase.addMaterial(1L, "Cuero", 2)).thenReturn(Mono.just(new BomItem(5L, 1L, "Cuero", 2)));

        webTestClient.post().uri("/products/1/materials")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"material\":\"Cuero\",\"quantity\":2}")
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo(5)
                .jsonPath("$.productId").isEqualTo(1)
                .jsonPath("$.material").isEqualTo("Cuero")
                .jsonPath("$.quantity").isEqualTo(2);
    }

    @Test
    void addMaterialWithNonPositiveQuantityReturns400() {
        webTestClient.post().uri("/products/1/materials")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"material\":\"Cuero\",\"quantity\":0}")
                .exchange()
                .expectStatus().isBadRequest();
        verifyNoInteractions(addBomItemUseCase);
    }

    @Test
    void addMaterialToMissingProductReturns404() {
        when(addBomItemUseCase.addMaterial(anyLong(), any(), anyInt()))
                .thenReturn(Mono.error(new ProductNotFoundException(99L)));

        webTestClient.post().uri("/products/99/materials")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"material\":\"Cuero\",\"quantity\":2}")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody().jsonPath("$.message").isEqualTo("Product with id 99 not found");
    }

    @Test
    void calculateReturnsRequiredMaterials() {
        when(calculateProductionUseCase.calculate(1L, 100)).thenReturn(Mono.just(new ProductionCalculation(
                "Zapato", 100, List.of(
                        new MaterialRequirement("Cuero", 200),
                        new MaterialRequirement("Suela", 100),
                        new MaterialRequirement("Cordones", 100)))));

        webTestClient.get().uri("/production/calculate?productId=1&quantity=100")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.product").isEqualTo("Zapato")
                .jsonPath("$.quantity").isEqualTo(100)
                .jsonPath("$.materials.length()").isEqualTo(3)
                .jsonPath("$.materials[0].material").isEqualTo("Cuero")
                .jsonPath("$.materials[0].required").isEqualTo(200)
                .jsonPath("$.materials[1].required").isEqualTo(100)
                .jsonPath("$.materials[2].required").isEqualTo(100);
    }

    @Test
    void calculateWithNonPositiveQuantityReturns400() {
        webTestClient.get().uri("/production/calculate?productId=1&quantity=0")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.message").value(message ->
                        assertThat((String) message).contains("quantity"));
        verifyNoInteractions(calculateProductionUseCase);
    }

    @Test
    void calculateWithMissingParameterReturns400() {
        webTestClient.get().uri("/production/calculate?productId=1")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void calculateWithNonNumericParameterReturns400() {
        webTestClient.get().uri("/production/calculate?productId=abc&quantity=5")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void calculateForMissingProductReturns404() {
        when(calculateProductionUseCase.calculate(99L, 10)).thenReturn(Mono.error(new ProductNotFoundException(99L)));

        webTestClient.get().uri("/production/calculate?productId=99&quantity=10")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void operatorsReturnsList() {
        when(getOperatorsUseCase.getOperators()).thenReturn(Flux.just(
                new Operator(1L, "Leanne Graham", "Sincere@april.biz", "1-770-736-8031")));

        webTestClient.get().uri("/operators")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].id").isEqualTo(1)
                .jsonPath("$[0].name").isEqualTo("Leanne Graham")
                .jsonPath("$[0].email").isEqualTo("Sincere@april.biz")
                .jsonPath("$[0].phone").isEqualTo("1-770-736-8031");
    }

    @Test
    void operatorsUpstreamErrorReturns502() {
        when(getOperatorsUseCase.getOperators()).thenReturn(Flux.error(new ExternalServiceException("down")));

        webTestClient.get().uri("/operators")
                .exchange()
                .expectStatus().isEqualTo(502);
    }

    @Test
    void operatorsUpstreamTimeoutReturns504() {
        when(getOperatorsUseCase.getOperators()).thenReturn(Flux.error(
                new ExternalServiceTimeoutException("slow", new TimeoutException())));

        webTestClient.get().uri("/operators")
                .exchange()
                .expectStatus().isEqualTo(504);
    }
}
