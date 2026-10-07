package co.quanta.mrp.bom.domain.exception;

public final class ProductNotFoundException extends DomainException {

    public ProductNotFoundException(Long productId) {
        super("Product with id " + productId + " not found");
    }
}
