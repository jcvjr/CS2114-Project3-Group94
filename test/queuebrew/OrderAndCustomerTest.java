package queuebrew;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Tests for Order and Customer.
 */
public class OrderAndCustomerTest {

    @Test
    public void orderStoresDrink() {
        assertEquals("Latte", new Order("Latte").getDrink());
    }

    @Test
    public void orderRejectsNullEmptyAndBlank() {
        assertThrows(IllegalArgumentException.class, () -> new Order(null));
        assertThrows(IllegalArgumentException.class, () -> new Order(""));
        assertThrows(IllegalArgumentException.class, () -> new Order("   "));
    }

    @Test
    public void customerStoresNameAndOrder() {
        Order latte = new Order("Latte");
        Customer alex = new Customer("Alex", latte);
        assertEquals("Alex", alex.getName());
        assertSame(latte, alex.getOrder());
    }

    @Test
    public void customerRejectsBadName() {
        Order latte = new Order("Latte");
        assertThrows(IllegalArgumentException.class,
            () -> new Customer(null, latte));
        assertThrows(IllegalArgumentException.class,
            () -> new Customer("", latte));
        assertThrows(IllegalArgumentException.class,
            () -> new Customer("  ", latte));
    }

    @Test
    public void customerRejectsNullOrder() {
        assertThrows(IllegalArgumentException.class,
            () -> new Customer("Alex", null));
    }
}
