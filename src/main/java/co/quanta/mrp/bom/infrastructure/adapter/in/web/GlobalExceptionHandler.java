package co.quanta.mrp.bom.infrastructure.adapter.in.web;

import co.quanta.mrp.bom.domain.exception.DomainException;
import co.quanta.mrp.bom.domain.exception.DuplicateResourceException;
import co.quanta.mrp.bom.domain.exception.ExternalServiceException;
import co.quanta.mrp.bom.domain.exception.ExternalServiceTimeoutException;
import co.quanta.mrp.bom.domain.exception.InvalidDomainDataException;
import co.quanta.mrp.bom.domain.exception.InvalidQuantityException;
import co.quanta.mrp.bom.domain.exception.ProductNotFoundException;
import co.quanta.mrp.bom.infrastructure.adapter.in.web.dto.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebInputException;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomain(DomainException ex, ServerHttpRequest request) {
        // Exhaustive over the sealed hierarchy: a new domain exception will not compile until mapped here.
        var status = switch (ex) {
            case ProductNotFoundException e -> HttpStatus.NOT_FOUND;
            case DuplicateResourceException e -> HttpStatus.CONFLICT;
            case InvalidQuantityException e -> HttpStatus.BAD_REQUEST;
            case InvalidDomainDataException e -> HttpStatus.BAD_REQUEST;
            case ExternalServiceTimeoutException e -> HttpStatus.GATEWAY_TIMEOUT;
            case ExternalServiceException e -> HttpStatus.BAD_GATEWAY;
        };
        if (ex instanceof ExternalServiceException) {
            log.warn("External service failure: {}", ex.getMessage());
        }
        return build(status, ex.getMessage(), request);
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ErrorResponse> handleBodyValidation(WebExchangeBindException ex, ServerHttpRequest request) {
        var message = ex.getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleParameterValidation(HandlerMethodValidationException ex,
                                                                   ServerHttpRequest request) {
        var message = ex.getAllValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> result.getMethodParameter().getParameterName() + ": " + error.getDefaultMessage()))
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
                                                                   ServerHttpRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<ErrorResponse> handleInput(ServerWebInputException ex, ServerHttpRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getReason(), request);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex, ServerHttpRequest request) {
        return build(ex.getStatusCode(), ex.getReason(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, ServerHttpRequest request) {
        log.error("Unexpected error on {}", request.getPath(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", request);
    }

    private static ResponseEntity<ErrorResponse> build(HttpStatusCode status, String message, ServerHttpRequest request) {
        var reason = status instanceof HttpStatus known ? known.getReasonPhrase() : String.valueOf(status.value());
        var body = new ErrorResponse(Instant.now(), status.value(), reason, message, request.getPath().value());
        return ResponseEntity.status(status).body(body);
    }
}
