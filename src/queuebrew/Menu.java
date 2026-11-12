package queuebrew;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * The shop menu: which ingredients exist and which ingredients make each drink.
 * Static data only, no Swing.
 */
public class Menu {

    private static final List<String> INGREDIENTS = List.of("Coffee",
        "Espresso", "Milk", "Foam", "Chocolate", "Vanilla", "Caramel");

    private static final Map<String, List<String>> RECIPES =
        new LinkedHashMap<>();

    static {
        RECIPES.put("Black Coffee", List.of("Coffee"));
        RECIPES.put("Espresso", List.of("Espresso"));
        RECIPES.put("Latte", List.of("Espresso", "Milk"));
        RECIPES.put("Cappuccino", List.of("Espresso", "Milk", "Foam"));
        RECIPES.put("Mocha", List.of("Espresso", "Chocolate", "Milk"));
        RECIPES.put("Vanilla Caramel Latte",
            List.of("Espresso", "Milk", "Vanilla", "Caramel"));
    }

    private Menu() {
        // static data only
    }

    /**
     * @return every ingredient the player can add to a cup
     */
    public static List<String> getIngredients() {
        return INGREDIENTS;
    }

    /**
     * @return the names of the drinks on the menu
     */
    public static List<String> getDrinks() {
        return new ArrayList<>(RECIPES.keySet());
    }

    /**
     * @param ingredient
     *            a possible ingredient name
     * @return true if it is on the ingredient list
     */
    public static boolean isIngredient(String ingredient) {
        return ingredient != null && INGREDIENTS.contains(ingredient);
    }

    /**
     * @param drink
     *            a drink name
     * @return the ingredients for that drink, or null if it is not on the menu
     */
    public static List<String> recipeFor(String drink) {
        List<String> recipe = RECIPES.get(drink);
        return recipe == null ? null : new ArrayList<>(recipe);
    }

    /**
     * @param random
     *            where the choice of drink comes from
     * @return an order for a randomly chosen menu drink
     * @throws IllegalArgumentException
     *             if random is null
     */
    public static Order randomOrder(Random random) {
        if (random == null) {
            throw new IllegalArgumentException("random must not be null");
        }
        List<String> drinks = getDrinks();
        return new Order(drinks.get(random.nextInt(drinks.size())));
    }

    /**
     * @return one line per drink such as "Latte: Espresso + Milk"
     */
    public static List<String> getRecipeCard() {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, List<String>> e : RECIPES.entrySet()) {
            lines.add(e.getKey() + ": " + String.join(" + ", e.getValue()));
        }
        return lines;
    }
}
