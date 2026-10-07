package co.quanta.mrp.bom.domain.model;

import co.quanta.mrp.bom.domain.exception.InvalidDomainDataException;
import co.quanta.mrp.bom.domain.exception.InvalidQuantityException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BomItemTest {

    @Test
    void requiredForMultipliesQuantityByUnits() {
        var item = BomItem.create(1L, "Cuero", 2);

        assertThat(item.requiredFor(100)).isEqualTo(new MaterialRequirement("Cuero", 200));
    }

    @Test
    void requiredForDoesNotOverflowInt() {
        var item = BomItem.create(1L, "Cuero", Integer.MAX_VALUE);

        assertThat(item.requiredFor(2).required()).isEqualTo(2L * Integer.MAX_VALUE);
    }

    @Test
    void requiredForRejectsNonPositiveUnits() {
        var item = BomItem.create(1L, "Cuero", 2);

        assertThatThrownBy(() -> item.requiredFor(0)).isInstanceOf(InvalidQuantityException.class);
    }

    @Test
    void rejectsInvalidData() {
        assertThatThrownBy(() -> BomItem.create(1L, "Cuero", 0)).isInstanceOf(InvalidQuantityException.class);
        assertThatThrownBy(() -> BomItem.create(1L, " ", 1)).isInstanceOf(InvalidDomainDataException.class);
        assertThatThrownBy(() -> BomItem.create(null, "Cuero", 1)).isInstanceOf(InvalidDomainDataException.class);
        assertThatThrownBy(() -> Product.create("")).isInstanceOf(InvalidDomainDataException.class);
    }

    @Test
    void productionCalculationForShoeExample() {
        var shoe = new Product(1L, "Zapato");
        var bom = List.of(
                BomItem.create(1L, "Cuero", 2),
                BomItem.create(1L, "Suela", 1),
                BomItem.create(1L, "Cordones", 1));

        var calculation = ProductionCalculation.of(shoe, 100, bom);

        assertThat(calculation.product()).isEqualTo("Zapato");
        assertThat(calculation.materials()).containsExactly(
                new MaterialRequirement("Cuero", 200),
                new MaterialRequirement("Suela", 100),
                new MaterialRequirement("Cordones", 100));
    }
}
