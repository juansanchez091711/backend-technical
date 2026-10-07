package co.quanta.mrp.bom.domain.exception;

public sealed class ExternalServiceException extends DomainException
        permits ExternalServiceTimeoutException {

    public ExternalServiceException(String message) {
        super(message);
    }

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
