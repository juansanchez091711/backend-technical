package co.quanta.mrp.bom.domain.model;

public record Product(Long id, String name) {

    public Product {
        name = DomainValidation.requireText(name, "Product name");
    }

    public static Product create(String name) {
        return new Product(null, name);
    }
}
