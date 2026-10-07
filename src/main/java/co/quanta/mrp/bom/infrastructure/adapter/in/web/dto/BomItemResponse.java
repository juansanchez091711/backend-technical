package co.quanta.mrp.bom.infrastructure.adapter.in.web.dto;

public record BomItemResponse(Long id, Long productId, String material, int quantity) {
}
