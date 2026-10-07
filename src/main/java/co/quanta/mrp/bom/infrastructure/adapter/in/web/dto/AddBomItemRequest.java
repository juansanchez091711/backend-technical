package co.quanta.mrp.bom.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AddBomItemRequest(@NotBlank @Size(max = 150) String material, @NotNull @Positive Integer quantity) {
}
