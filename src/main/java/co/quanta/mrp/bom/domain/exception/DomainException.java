package co.quanta.mrp.bom.domain.exception;

/**
 * Root of the domain exception hierarchy. Sealed so every domain failure is known
 * at compile time and can be mapped exhaustively to an HTTP status by the web adapter.
 */
public abstract sealed class DomainException extends RuntimeException
        permits ProductNotFoundException, InvalidQuantityException, InvalidDomainDataException,
                DuplicateResourceException, ExternalServiceException {

    protected DomainException(String message) {
        super(message);
    }

    protected DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
