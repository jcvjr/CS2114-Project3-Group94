package queuebrew;

/**
 * A customer waiting in line: a name and what they ordered. Immutable.
 */
public class Customer {

    private final String name;
    private final Order order;

    /**
     * Creates a customer.
     *
     * @param name
     *            the customer name
     * @param order
     *            what the customer ordered
     * @throws IllegalArgumentException
     *             if name is null or blank, or order is null
     */
    public Customer(String name, Order order) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be empty");
        }
        if (order == null) {
            throw new IllegalArgumentException("order must not be null");
        }
        this.name = name;
        this.order = order;
    }

    /**
     * @return the customer name
     */
    public String getName() {
        return name;
    }

    /**
     * @return what the customer ordered
     */
    public Order getOrder() {
        return order;
    }
}
