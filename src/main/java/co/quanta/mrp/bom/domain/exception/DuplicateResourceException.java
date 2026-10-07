package co.quanta.mrp.bom.domain.exception;

public final class DuplicateResourceException extends DomainException {

    public DuplicateResourceException(String message) {
        super(message);
    }

    public DuplicateResourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
