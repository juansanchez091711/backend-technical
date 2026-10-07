package co.quanta.mrp.bom.domain.model;

import java.util.List;

public record ProductionCalculation(String product, int quantity, List<MaterialRequirement> materials) {

    public ProductionCalculation {
        product = DomainValidation.requireText(product, "Product name");
        DomainValidation.requirePositive(quantity, "Production quantity");
        materials = materials == null ? List.of() : List.copyOf(materials);
    }

    /** Builds the calculation for {@code units} of {@code product} from its BOM lines. */
    public static ProductionCalculation of(Product product, int units, List<BomItem> bom) {
        return new ProductionCalculation(product.name(), units,
                bom.stream().map(item -> item.requiredFor(units)).toList());
    }
}
