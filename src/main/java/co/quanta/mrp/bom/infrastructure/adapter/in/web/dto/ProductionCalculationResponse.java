package co.quanta.mrp.bom.infrastructure.adapter.in.web.dto;

import java.util.List;

public record ProductionCalculationResponse(String product, int quantity, List<MaterialRequirementResponse> materials) {
}
