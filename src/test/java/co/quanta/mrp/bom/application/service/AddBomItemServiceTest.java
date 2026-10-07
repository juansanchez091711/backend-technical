package co.quanta.mrp.bom.application.service;

import co.quanta.mrp.bom.domain.exception.DuplicateResourceException;
import co.quanta.mrp.bom.domain.exception.InvalidQuantityException;
import co.quanta.mrp.bom.domain.exception.ProductNotFoundException;
import co.quanta.mrp.bom.domain.model.BomItem;
import co.quanta.mrp.bom.domain.port.out.BomItemRepositoryPort;
import co.quanta.mrp.bom.domain.port.out.ProductRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddBomItemServiceTest {

    @Mock
    private ProductRepositoryPort productRepository;

    @Mock
    private BomItemRepositoryPort bomItemRepository;

    @InjectMocks
    private AddBomItemService service;

    @Test
    void addsMaterialToExistingProduct() {
        when(productRepository.existsById(1L)).thenReturn(Mono.just(true));
        when(bomItemRepository.existsByProductIdAndMaterial(1L, "Cuero")).thenReturn(Mono.just(false));
        when(bomItemRepository.save(any())).thenAnswer(inv -> {
            BomItem item = inv.getArgument(0);
            return Mono.just(new BomItem(10L, item.productId(), item.material(), item.quantity()));
        });

        StepVerifier.create(service.addMaterial(1L, " Cuero ", 2))
                .expectNext(new BomItem(10L, 1L, "Cuero", 2))
                .verifyComplete();
    }

    @Test
    void failsWhenProductDoesNotExist() {
        when(productRepository.existsById(99L)).thenReturn(Mono.just(false));

        StepVerifier.create(service.addMaterial(99L, "Cuero", 2))
                .expectError(ProductNotFoundException.class)
                .verify();
        verifyNoInteractions(bomItemRepository);
    }

    @Test
    void failsWhenMaterialAlreadyInBom() {
        when(productRepository.existsById(1L)).thenReturn(Mono.just(true));
        when(bomItemRepository.existsByProductIdAndMaterial(1L, "Cuero")).thenReturn(Mono.just(true));

        StepVerifier.create(service.addMaterial(1L, "Cuero", 2))
                .expectError(DuplicateResourceException.class)
                .verify();
        verify(bomItemRepository, never()).save(any());
    }

    @Test
    void rejectsNonPositiveQuantity() {
        StepVerifier.create(service.addMaterial(1L, "Cuero", 0))
                .expectError(InvalidQuantityException.class)
                .verify();
        verifyNoInteractions(productRepository, bomItemRepository);
    }
}
