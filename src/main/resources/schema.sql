CREATE TABLE IF NOT EXISTS product (
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS bom_item (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    material   VARCHAR(150) NOT NULL,
    quantity   INT NOT NULL CHECK (quantity > 0),
    CONSTRAINT fk_bom_product FOREIGN KEY (product_id) REFERENCES product(id),
    CONSTRAINT uq_product_material UNIQUE (product_id, material)
);
