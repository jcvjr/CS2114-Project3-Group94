package queuebrew;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One drink order: the drink name and the ingredients that make it. Immutable.
 */
public class Order {

    private final String drink;
    private final List<String> ingredients;

    /**
     * Creates an order for a drink on the {@link Menu}, using its recipe.
     *
     * @param drink
     *            the name of the drink, for example "Latte"
     * @throws IllegalArgumentException
     *             if drink is null, blank, or not on the menu
     */
    public Order(String drink) {
        this(drink, recipeOrFail(drink));
    }

    /**
     * Creates an order with an explicit recipe.
     *
     * @param drink
     *            the name of the drink
     * @param ingredients
     *            what the cup must contain, in any order
     * @throws IllegalArgumentException
     *             if drink is null or blank, or ingredients is null, empty, or
     *             holds a null or blank entry
     */
    public Order(String drink, List<String> ingredients) {
        if (drink == null || drink.isBlank()) {
            throw new IllegalArgumentException("drink must not be empty");
        }
        if (ingredients == null || ingredients.isEmpty()) {
            throw new IllegalArgumentException("ingredients must not be empty");
        }
        for (String item : ingredients) {
            if (item == null || item.isBlank()) {
                throw new IllegalArgumentException(
                    "ingredient must not be empty");
            }
        }
        this.drink = drink;
        this.ingredients = new ArrayList<>(ingredients);
    }

    private static List<String> recipeOrFail(String drink) {
        if (drink == null || drink.isBlank()) {
            throw new IllegalArgumentException("drink must not be empty");
        }
        List<String> recipe = Menu.recipeFor(drink);
        if (recipe == null) {
            throw new IllegalArgumentException("not on the menu: " + drink);
        }
        return recipe;
    }

    /**
     * @return the drink name
     */
    public String getDrink() {
        return drink;
    }

    /**
     * @return a copy of the ingredients this drink needs
     */
    public List<String> getIngredients() {
        return new ArrayList<>(ingredients);
    }

    /**
     * Checks a cup against this recipe. Order does not matter; each ingredient
     * must appear the same number of times.
     *
     * @param cup
     *            the ingredients in the cup
     * @return true if the cup holds exactly this recipe
     */
    public boolean matches(List<String> cup) {
        if (cup == null) {
            return false;
        }
        List<String> a = new ArrayList<>(ingredients);
        List<String> b = new ArrayList<>(cup);
        Collections.sort(a);
        Collections.sort(b);
        return a.equals(b);
    }
}
