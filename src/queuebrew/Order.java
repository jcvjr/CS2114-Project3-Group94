package queuebrew;

/**
 * One drink order. Immutable.
 */
public class Order {

    private final String drink;

    /**
     * Creates an order for the given drink.
     *
     * @param drink
     *            the name of the drink, for example "Latte"
     * @throws IllegalArgumentException
     *             if drink is null or blank
     */
    public Order(String drink) {
        if (drink == null || drink.isBlank()) {
            throw new IllegalArgumentException("drink must not be empty");
        }
        this.drink = drink;
    }

    /**
     * @return the drink name
     */
    public String getDrink() {
        return drink;
    }
}
