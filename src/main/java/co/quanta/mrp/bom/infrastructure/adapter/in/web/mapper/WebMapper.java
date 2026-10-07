package co.quanta.mrp.bom.infrastructure.adapter.in.web.mapper;

import co.quanta.mrp.bom.domain.model.BomItem;
import co.quanta.mrp.bom.domain.model.MaterialRequirement;
import co.quanta.mrp.bom.domain.model.Operator;
import co.quanta.mrp.bom.domain.model.Product;
import co.quanta.mrp.bom.domain.model.ProductionCalculation;
import co.quanta.mrp.bom.infrastructure.adapter.in.web.dto.BomItemResponse;
import co.quanta.mrp.bom.infrastructure.adapter.in.web.dto.MaterialRequirementResponse;
import co.quanta.mrp.bom.infrastructure.adapter.in.web.dto.OperatorResponse;
import co.quanta.mrp.bom.infrastructure.adapter.in.web.dto.ProductResponse;
import co.quanta.mrp.bom.infrastructure.adapter.in.web.dto.ProductionCalculationResponse;

/** Maps domain records to REST response DTOs. */
public final class WebMapper {

    private WebMapper() {
    }

    public static ProductResponse toResponse(Product product) {
        return new ProductResponse(product.id(), product.name());
    }

    public static BomItemResponse toResponse(BomItem item) {
        return new BomItemResponse(item.id(), item.productId(), item.material(), item.quantity());
    }

    public static ProductionCalculationResponse toResponse(ProductionCalculation calculation) {
        return new ProductionCalculationResponse(calculation.product(), calculation.quantity(),
                calculation.materials().stream().map(WebMapper::toResponse).toList());
    }

    public static MaterialRequirementResponse toResponse(MaterialRequirement requirement) {
        return new MaterialRequirementResponse(requirement.material(), requirement.required());
    }

    public static OperatorResponse toResponse(Operator operator) {
        return new OperatorResponse(operator.id(), operator.name(), operator.email(), operator.phone());
    }
}
