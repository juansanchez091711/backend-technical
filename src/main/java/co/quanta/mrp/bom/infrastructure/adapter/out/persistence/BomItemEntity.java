package co.quanta.mrp.bom.infrastructure.adapter.out.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("bom_item")
public record BomItemEntity(@Id Long id, @Column("product_id") Long productId, String material, int quantity) {
}
