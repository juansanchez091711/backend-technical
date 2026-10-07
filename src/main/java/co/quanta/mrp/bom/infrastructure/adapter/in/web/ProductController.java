package co.quanta.mrp.bom.infrastructure.adapter.in.web;

import co.quanta.mrp.bom.domain.port.in.AddBomItemUseCase;
import co.quanta.mrp.bom.domain.port.in.CreateProductUseCase;
import co.quanta.mrp.bom.infrastructure.adapter.in.web.dto.AddBomItemRequest;
import co.quanta.mrp.bom.infrastructure.adapter.in.web.dto.BomItemResponse;
import co.quanta.mrp.bom.infrastructure.adapter.in.web.dto.CreateProductRequest;
import co.quanta.mrp.bom.infrastructure.adapter.in.web.dto.ProductResponse;
import co.quanta.mrp.bom.infrastructure.adapter.in.web.mapper.WebMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.net.URI;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final CreateProductUseCase createProductUseCase;
    private final AddBomItemUseCase addBomItemUseCase;

    public ProductController(CreateProductUseCase createProductUseCase, AddBomItemUseCase addBomItemUseCase) {
        this.createProductUseCase = createProductUseCase;
        this.addBomItemUseCase = addBomItemUseCase;
    }

    @PostMapping
    public Mono<ResponseEntity<ProductResponse>> createProduct(@Valid @RequestBody CreateProductRequest request) {
        return createProductUseCase.createProduct(request.name())
                .map(WebMapper::toResponse)
                .map(body -> ResponseEntity.created(URI.create("/products/" + body.id())).body(body));
    }

    @PostMapping("/{productId}/materials")
    public Mono<ResponseEntity<BomItemResponse>> addMaterial(@PathVariable Long productId,
                                                             @Valid @RequestBody AddBomItemRequest request) {
        return addBomItemUseCase.addMaterial(productId, request.material(), request.quantity())
                .map(WebMapper::toResponse)
                .map(body -> ResponseEntity
                        .created(URI.create("/products/" + productId + "/materials/" + body.id()))
                        .body(body));
    }
}
