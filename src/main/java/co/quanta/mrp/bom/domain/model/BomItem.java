package co.quanta.mrp.bom.domain.model;

import co.quanta.mrp.bom.domain.exception.InvalidDomainDataException;
import co.quanta.mrp.bom.domain.exception.InvalidQuantityException;

/** A Bill of Materials line: {@code quantity} units of {@code material} per unit of product. */
public record BomItem(Long id, Long productId, String material, int quantity) {

    public BomItem {
        if (productId == null) {
            throw new InvalidDomainDataException("Product id must not be null");
        }
        material = DomainValidation.requireText(material, "Material");
        DomainValidation.requirePositive(quantity, "Material quantity");
    }

    public static BomItem create(Long productId, String material, int quantity) {
        return new BomItem(null, productId, material, quantity);
    }

    /** Material required to produce {@code units} products; exact arithmetic fails fast on overflow. */
    public MaterialRequirement requiredFor(int units) {
        DomainValidation.requirePositive(units, "Production quantity");
        try {
            return new MaterialRequirement(material, Math.multiplyExact((long) quantity, (long) units));
        } catch (ArithmeticException e) {
            throw new InvalidQuantityException("Required quantity for " + material + " overflows");
        }
    }
}
