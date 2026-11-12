package queuebrew;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for GameState. Nothing here touches Swing: the model plays whole games
 * on its own. Arrivals come every 8 seconds on day 1 and 7 seconds on day 2.
 */
public class GameStateTest {

    private GameState game;

    @BeforeEach
    public void setUp() {
        game = new GameState(new Random(1));
    }

    private void tick(int times) {
        for (int i = 0; i < times; i++) {
            game.tick();
        }
    }

    /** Makes the front customer's drink correctly and completes it. */
    private boolean serveFront() {
        Customer[] waiting = game.getWaitingCustomers();
        if (waiting.length == 0) {
            return false;
        }
        for (String ingredient : waiting[0].getOrder().getIngredients()) {
            game.addIngredient(ingredient);
        }
        return game.completeFrontOrder();
    }

    /** Ticks one second at a time, serving the front order after each tick. */
    private void tickAndServe(int times) {
        for (int i = 0; i < times; i++) {
            game.tick();
            serveFront();
        }
    }

    // ---- constructors and listeners ----

    @Test
    public void newGameStartsAsSpecified() {
        assertEquals(3, game.getLives());
        assertEquals(1, game.getDay());
        assertEquals(60, game.getSecondsLeftInDay());
        assertEquals(0, game.getQueueSize());
        assertEquals(6, game.getQueueCapacity());
        assertEquals(GameState.Status.PLAYING, game.getStatus());
        assertEquals("", game.getMessage());
    }

    @Test
    public void constructorRejectsNullRandom() {
        assertThrows(IllegalArgumentException.class, () -> new GameState(null));
    }

    @Test
    public void listenerIsToldOncePerTick() {
        int[] calls = { 0 };
        game.addListener(() -> calls[0]++);
        game.tick();
        assertEquals(1, calls[0]);
    }

    @Test
    public void addListenerRejectsNull() {
        assertThrows(IllegalArgumentException.class,
            () -> game.addListener(null));
    }

    // ---- tick: clock and arrivals ----

    @Test
    public void firstCustomerArrivesAfterEightSeconds() {
        tick(7);
        assertEquals(53, game.getSecondsLeftInDay());
        assertEquals(0, game.getQueueSize());
        tick(1);
        assertEquals(52, game.getSecondsLeftInDay());
        assertEquals(1, game.getQueueSize());
    }

    @Test
    public void sameSeedGivesSameArrivals() {
        GameState other = new GameState(new Random(1));
        tick(24);
        for (int i = 0; i < 24; i++) {
            other.tick();
        }
        Customer[] a = game.getWaitingCustomers();
        Customer[] b = other.getWaitingCustomers();
        assertEquals(3, a.length);
        for (int i = 0; i < a.length; i++) {
            assertEquals(a[i].getName(), b[i].getName());
            assertEquals(a[i].getOrder().getDrink(), b[i].getOrder().getDrink());
        }
    }

    @Test
    public void fullQueueCostsALifeWhenAnotherCustomerArrives() {
        tick(48);
        assertEquals(6, game.getQueueSize());
        assertEquals(3, game.getLives());
        tick(8);
        assertEquals(6, game.getQueueSize());
        assertEquals(2, game.getLives());
        assertEquals(GameState.TURNED_AWAY_MESSAGE, game.getMessage());
    }

    @Test
    public void thirdLostLifeEndsTheGame() {
        // Never serve anyone: line fills at 48 s, then 56 s (day 1 arrivals
        // every 8 s), then day 2 arrivals come every 7 s.
        tick(60); // day 1: arrivals at 8..48 fill, 56 costs a life
        assertEquals(2, game.getLives());
        tick(14); // day 2: arrivals at 7 and 14 cost two more lives
        assertEquals(0, game.getLives());
        assertEquals(GameState.Status.LOST, game.getStatus());
    }

    @Test
    public void messageClearsOnNextTick() {
        tick(56);
        assertEquals(GameState.TURNED_AWAY_MESSAGE, game.getMessage());
        tick(1);
        assertEquals("", game.getMessage());
    }

    // ---- tick: days, win, lose ----

    @Test
    public void dayRollsOverAfterSixtySeconds() {
        tickAndServe(60);
        assertEquals(2, game.getDay());
        assertEquals(60, game.getSecondsLeftInDay());
    }

    @Test
    public void dayTwoArrivalsCometEverySevenSeconds() {
        tickAndServe(60);
        while (serveFront()) {
            // empty the line
        }
        tick(6);
        assertEquals(0, game.getQueueSize());
        tick(1);
        assertEquals(1, game.getQueueSize());
    }

    @Test
    public void survivingSevenDaysWins() {
        tickAndServe(7 * 60);
        assertEquals(GameState.Status.WON, game.getStatus());
        assertEquals(7, game.getDay());
        assertTrue(game.getLives() > 0);
    }

    @Test
    public void dayNeverPassesSevenAndClockNeverGoesNegative() {
        tickAndServe(7 * 60 + 50);
        assertEquals(7, game.getDay());
        assertTrue(game.getSecondsLeftInDay() >= 0);
    }

    @Test
    public void nothingChangesAfterTheGameEnds() {
        tick(74); // lost, see thirdLostLifeEndsTheGame
        assertEquals(GameState.Status.LOST, game.getStatus());
        int[] calls = { 0 };
        game.addListener(() -> calls[0]++);
        int seconds = game.getSecondsLeftInDay();
        int day = game.getDay();
        tick(5);
        assertFalse(game.completeFrontOrder());
        assertEquals(seconds, game.getSecondsLeftInDay());
        assertEquals(day, game.getDay());
        assertEquals(0, calls[0]);
    }

    // ---- completeFrontOrder ----

    @Test
    public void completeFrontOrderServesInArrivalOrder() {
        tick(16);
        Customer first = game.getWaitingCustomers()[0];
        Customer second = game.getWaitingCustomers()[1];
        assertTrue(serveFront());
        assertEquals(1, game.getQueueSize());
        assertEquals(second, game.getWaitingCustomers()[0]);
        assertFalse(first == game.getWaitingCustomers()[0]);
    }

    @Test
    public void completeOnEmptyLineGivesMessageAndKeepsLives() {
        assertFalse(game.completeFrontOrder());
        assertEquals(GameState.EMPTY_MESSAGE, game.getMessage());
        assertEquals(3, game.getLives());
        assertEquals(GameState.Status.PLAYING, game.getStatus());
    }

    @Test
    public void completeNotifiesListeners() {
        int[] calls = { 0 };
        game.addListener(() -> calls[0]++);
        game.completeFrontOrder();
        assertEquals(1, calls[0]);
    }

    // ---- making the drink: cup ----

    @Test
    public void addIngredientPutsItInTheCupAndNotifies() {
        int[] calls = { 0 };
        game.addListener(() -> calls[0]++);
        assertTrue(game.addIngredient("Espresso"));
        assertTrue(game.addIngredient("Milk"));
        assertEquals(List.of("Espresso", "Milk"), game.getCup());
        assertEquals(2, calls[0]);
    }

    @Test
    public void addIngredientRejectsUnknownAndNull() {
        assertThrows(IllegalArgumentException.class,
            () -> game.addIngredient("Motor Oil"));
        assertThrows(IllegalArgumentException.class,
            () -> game.addIngredient(null));
        assertEquals(0, game.getCup().size());
    }

    @Test
    public void fullCupRefusesMoreAndSaysSo() {
        for (int i = 0; i < GameState.CUP_CAPACITY; i++) {
            assertTrue(game.addIngredient("Milk"));
        }
        assertFalse(game.addIngredient("Milk"));
        assertEquals(GameState.CUP_CAPACITY, game.getCup().size());
        assertEquals(GameState.CUP_FULL_MESSAGE, game.getMessage());
    }

    @Test
    public void clearCupEmptiesIt() {
        game.addIngredient("Milk");
        game.clearCup();
        assertTrue(game.getCup().isEmpty());
    }

    @Test
    public void getCupIsACopy() {
        game.addIngredient("Milk");
        game.getCup().clear();
        assertEquals(1, game.getCup().size());
    }

    @Test
    public void completeWithEmptyCupGivesMessageAndKeepsCustomer() {
        tick(8);
        assertFalse(game.completeFrontOrder());
        assertEquals(GameState.EMPTY_CUP_MESSAGE, game.getMessage());
        assertEquals(1, game.getQueueSize());
        assertEquals(3, game.getLives());
    }

    @Test
    public void wrongCupIsDumpedAndCustomerKeepsWaiting() {
        tick(8);
        Customer front = game.getWaitingCustomers()[0];
        // Three Foam matches no recipe on the menu.
        game.addIngredient("Foam");
        game.addIngredient("Foam");
        game.addIngredient("Foam");
        assertFalse(game.completeFrontOrder());
        assertEquals(GameState.WRONG_ORDER_MESSAGE, game.getMessage());
        assertTrue(game.getCup().isEmpty());
        assertEquals(front, game.getWaitingCustomers()[0]);
        assertEquals(3, game.getLives());
    }

    @Test
    public void wrongOrderLocksThePlayerOutForThreeSeconds() {
        tick(8);
        game.addIngredient("Foam");
        game.addIngredient("Foam");
        game.addIngredient("Foam");
        game.completeFrontOrder();
        assertEquals(GameState.WRONG_ORDER_PENALTY_SECONDS,
            game.getPenaltySecondsLeft());
        assertFalse(game.addIngredient("Milk"));
        assertFalse(game.completeFrontOrder());
        assertTrue(game.getCup().isEmpty());
        tick(2);
        assertEquals(1, game.getPenaltySecondsLeft());
        assertFalse(game.addIngredient("Milk"));
        tick(1);
        assertEquals(0, game.getPenaltySecondsLeft());
        assertTrue(game.addIngredient("Milk"));
    }

    @Test
    public void clearCupIsBlockedDuringPenaltyAndItNeverGoesNegative() {
        tick(8);
        game.addIngredient("Foam");
        game.addIngredient("Foam");
        game.addIngredient("Foam");
        game.completeFrontOrder();
        game.clearCup();
        assertEquals(GameState.WRONG_ORDER_PENALTY_SECONDS,
            game.getPenaltySecondsLeft());
        tick(10);
        assertEquals(0, game.getPenaltySecondsLeft());
    }

    @Test
    public void newGameHasNoPenalty() {
        assertEquals(0, game.getPenaltySecondsLeft());
    }

    @Test
    public void emptyCupOrEmptyLineDoesNotStartAPenalty() {
        game.completeFrontOrder();
        assertEquals(0, game.getPenaltySecondsLeft());
        tick(8);
        game.completeFrontOrder();
        assertEquals(0, game.getPenaltySecondsLeft());
    }

    @Test
    public void ingredientOrderDoesNotMatter() {
        GameState custom = new GameState(new Random(1));
        tickOn(custom, 8);
        List<String> recipe = custom.getWaitingCustomers()[0].getOrder()
            .getIngredients();
        for (int i = recipe.size() - 1; i >= 0; i--) {
            custom.addIngredient(recipe.get(i));
        }
        assertTrue(custom.completeFrontOrder());
        assertEquals(0, custom.getQueueSize());
    }

    @Test
    public void cupActionsDoNothingAfterTheGameEnds() {
        tick(74); // lost, see thirdLostLifeEndsTheGame
        assertFalse(game.addIngredient("Milk"));
        game.clearCup();
        assertTrue(game.getCup().isEmpty());
    }

    private static void tickOn(GameState g, int times) {
        for (int i = 0; i < times; i++) {
            g.tick();
        }
    }

    // ---- getters ----

    @Test
    public void waitingCustomersIsACopy() {
        tick(8);
        Customer[] copy = game.getWaitingCustomers();
        Customer original = copy[0];
        copy[0] = null;
        assertEquals(original, game.getWaitingCustomers()[0]);
        assertEquals(1, game.getWaitingCustomers().length);
    }

    @Test
    public void emptyGameHasNoWaitingCustomers() {
        assertArrayEquals(new Customer[0], game.getWaitingCustomers());
    }

    @Test
    public void queueSizeNeverExceedsCapacity() {
        tick(60);
        assertEquals(6, game.getQueueSize());
        assertEquals(6, game.getQueueCapacity());
    }
}
