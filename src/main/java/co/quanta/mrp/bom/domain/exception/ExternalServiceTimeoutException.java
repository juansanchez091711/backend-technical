package co.quanta.mrp.bom.domain.exception;

public final class ExternalServiceTimeoutException extends ExternalServiceException {

    public ExternalServiceTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
