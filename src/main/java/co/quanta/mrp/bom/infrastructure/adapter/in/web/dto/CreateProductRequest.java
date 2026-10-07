package co.quanta.mrp.bom.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProductRequest(@NotBlank @Size(max = 150) String name) {
}
