package co.quanta.mrp.bom.domain.model;

import co.quanta.mrp.bom.domain.exception.InvalidDomainDataException;
import co.quanta.mrp.bom.domain.exception.InvalidQuantityException;

/** Shared invariant checks for domain records. */
final class DomainValidation {

    private DomainValidation() {
    }

    static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidDomainDataException(field + " must not be blank");
        }
        return value.strip();
    }

    static int requirePositive(int value, String field) {
        if (value <= 0) {
            throw new InvalidQuantityException(field + " must be greater than 0");
        }
        return value;
    }
}
