package co.quanta.mrp.bom.domain.model;

import co.quanta.mrp.bom.domain.exception.InvalidQuantityException;

public record MaterialRequirement(String material, long required) {

    public MaterialRequirement {
        material = DomainValidation.requireText(material, "Material");
        if (required <= 0) {
            throw new InvalidQuantityException("Required quantity must be greater than 0");
        }
    }
}
