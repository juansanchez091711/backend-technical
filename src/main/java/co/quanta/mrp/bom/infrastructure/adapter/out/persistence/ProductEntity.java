package co.quanta.mrp.bom.infrastructure.adapter.out.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("product")
public record ProductEntity(@Id Long id, String name) {
}
