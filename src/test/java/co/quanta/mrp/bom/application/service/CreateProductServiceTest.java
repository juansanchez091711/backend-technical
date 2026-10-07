package co.quanta.mrp.bom.application.service;

import co.quanta.mrp.bom.domain.exception.DuplicateResourceException;
import co.quanta.mrp.bom.domain.exception.InvalidDomainDataException;
import co.quanta.mrp.bom.domain.model.Product;
import co.quanta.mrp.bom.domain.port.out.ProductRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateProductServiceTest {

    @Mock
    private ProductRepositoryPort productRepository;

    @InjectMocks
    private CreateProductService service;

    @Test
    void createsProductWithTrimmedName() {
        when(productRepository.save(any())).thenAnswer(inv -> {
            Product product = inv.getArgument(0);
            return Mono.just(new Product(1L, product.name()));
        });

        StepVerifier.create(service.createProduct("  Zapato "))
                .expectNext(new Product(1L, "Zapato"))
                .verifyComplete();
    }

    @Test
    void rejectsBlankNameWithoutTouchingRepository() {
        StepVerifier.create(service.createProduct(" "))
                .expectError(InvalidDomainDataException.class)
                .verify();
        verifyNoInteractions(productRepository);
    }

    @Test
    void propagatesDuplicateError() {
        when(productRepository.save(any())).thenReturn(Mono.error(new DuplicateResourceException("duplicated")));

        StepVerifier.create(service.createProduct("Zapato"))
                .expectError(DuplicateResourceException.class)
                .verify();
    }
}
