package queuebrew;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Tests for Menu and for Order's recipe handling.
 */
public class MenuAndOrderTest {

    @Test
    public void everyMenuDrinkUsesOnlyKnownIngredients() {
        for (String drink : Menu.getDrinks()) {
            for (String item : Menu.recipeFor(drink)) {
                assertTrue(Menu.isIngredient(item), drink + " uses " + item);
            }
        }
    }

    @Test
    public void recipeForUnknownDrinkIsNull() {
        assertNull(Menu.recipeFor("Soup"));
        assertNull(Menu.recipeFor(null));
    }

    @Test
    public void recipeCardHasOneLinePerDrink() {
        assertEquals(Menu.getDrinks().size(), Menu.getRecipeCard().size());
        assertTrue(Menu.getRecipeCard().contains("Latte: Espresso + Milk"));
    }

    @Test
    public void randomOrderIsOnTheMenuAndRejectsNullRandom() {
        Order order = Menu.randomOrder(new Random(5));
        assertNotNull(Menu.recipeFor(order.getDrink()));
        assertThrows(IllegalArgumentException.class,
            () -> Menu.randomOrder(null));
    }

    @Test
    public void orderFromMenuGetsItsRecipe() {
        assertEquals(List.of("Espresso", "Milk"),
            new Order("Latte").getIngredients());
    }

    @Test
    public void orderRejectsDrinkNotOnMenu() {
        assertThrows(IllegalArgumentException.class, () -> new Order("Soup"));
    }

    @Test
    public void orderRejectsBadRecipes() {
        assertThrows(IllegalArgumentException.class,
            () -> new Order("Latte", null));
        assertThrows(IllegalArgumentException.class,
            () -> new Order("Latte", List.of()));
        assertThrows(IllegalArgumentException.class,
            () -> new Order("Latte", List.of("Milk", " ")));
    }

    @Test
    public void matchesIgnoresOrderButNotCounts() {
        Order latte = new Order("Latte");
        assertTrue(latte.matches(List.of("Milk", "Espresso")));
        assertFalse(latte.matches(List.of("Espresso")));
        assertFalse(latte.matches(List.of("Espresso", "Milk", "Milk")));
        assertFalse(latte.matches(List.of()));
        assertFalse(latte.matches(null));
    }

    @Test
    public void getIngredientsIsACopy() {
        Order latte = new Order("Latte");
        latte.getIngredients().clear();
        assertEquals(2, latte.getIngredients().size());
    }
}
