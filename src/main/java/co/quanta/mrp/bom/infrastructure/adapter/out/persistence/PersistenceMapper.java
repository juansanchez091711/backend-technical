package co.quanta.mrp.bom.infrastructure.adapter.out.persistence;

import co.quanta.mrp.bom.domain.model.BomItem;
import co.quanta.mrp.bom.domain.model.Product;

/** Maps between R2DBC entities and domain records. */
final class PersistenceMapper {

    private PersistenceMapper() {
    }

    static ProductEntity toEntity(Product product) {
        return new ProductEntity(product.id(), product.name());
    }

    static Product toDomain(ProductEntity entity) {
        return new Product(entity.id(), entity.name());
    }

    static BomItemEntity toEntity(BomItem item) {
        return new BomItemEntity(item.id(), item.productId(), item.material(), item.quantity());
    }

    static BomItem toDomain(BomItemEntity entity) {
        return new BomItem(entity.id(), entity.productId(), entity.material(), entity.quantity());
    }
}
