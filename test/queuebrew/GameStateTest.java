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
        game = new GameState(new Random(1), 0); // no jitter: exact timing
    }

    /** One second, first ending any pause between days like the view would. */
    private void tickOnce() {
        if (game.isBetweenDays()) {
            game.startNextDay();
        }
        game.tick();
    }

    private void tick(int times) {
        for (int i = 0; i < times; i++) {
            tickOnce();
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
            tickOnce();
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
        GameState other = new GameState(new Random(1), 0);
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
        tickAndServe(120); // days 1 and 2 go fine
        // Day 3 (6 s gaps), never serving: the line fills at 36 s, then 42,
        // 48 and 54 s each turn a customer away.
        tick(54);
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
        game.startNextDay();
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
        tick(60); // lost, see leftoverCustomersBeyondYourLivesLoseTheGame
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

    // ---- between days ----

    @Test
    public void endOfDayPausesTheGameUntilTheNextDayStarts() {
        tickAndServe(60);
        assertTrue(game.isBetweenDays());
        assertEquals(2, game.getDay());
        for (int i = 0; i < 5; i++) {
            game.tick(); // ignored while paused
        }
        assertEquals(60, game.getSecondsLeftInDay());
        assertEquals(0, game.getQueueSize());
        assertFalse(game.addIngredient("Milk"));
        assertFalse(game.completeFrontOrder());
        game.startNextDay();
        assertFalse(game.isBetweenDays());
        game.tick();
        assertEquals(59, game.getSecondsLeftInDay());
    }

    @Test
    public void startNextDayNotifiesOnlyWhenPaused() {
        int[] calls = { 0 };
        game.addListener(() -> calls[0]++);
        game.startNextDay();
        assertEquals(0, calls[0]);
        tickAndServe(60);
        int afterDayEnd = calls[0];
        game.startNextDay();
        assertEquals(afterDayEnd + 1, calls[0]);
    }

    @Test
    public void winningDaySevenDoesNotPause() {
        tickAndServe(7 * 60);
        assertEquals(GameState.Status.WON, game.getStatus());
        assertFalse(game.isBetweenDays());
    }

    @Test
    public void newGameIsNotBetweenDays() {
        assertFalse(game.isBetweenDays());
    }

    @Test
    public void endOfDayClearsTheLineTheCupAndAnyPenalty() {
        tickAndServe(40);
        tick(19); // arrivals at 48 s and 56 s are left waiting
        assertEquals(2, game.getQueueSize());
        game.addIngredient("Foam");
        game.addIngredient("Foam");
        game.addIngredient("Foam");
        game.completeFrontOrder(); // wrong cup: starts a penalty
        assertTrue(game.getPenaltySecondsLeft() > 0);
        game.tick(); // 60th second ends the day
        assertTrue(game.isBetweenDays());
        assertEquals(0, game.getQueueSize());
        assertTrue(game.getCup().isEmpty());
        assertEquals(0, game.getPenaltySecondsLeft());
    }

    @Test
    public void nextDayStartsWithAnEmptyLine() {
        tickAndServe(40);
        tick(20);
        game.startNextDay();
        assertEquals(0, game.getQueueSize());
        assertEquals(2, game.getDay());
    }

    @Test
    public void eachCustomerLeftInLineAtClosingCostsALife() {
        tickAndServe(40);
        tick(20); // two customers left waiting
        assertEquals(1, game.getLives());
        assertEquals(GameState.Status.PLAYING, game.getStatus());
        assertTrue(game.isBetweenDays());
        assertTrue(game.getMessage().contains("2 customers"), game.getMessage());
    }

    @Test
    public void oneCustomerLeftAtClosingCostsOneLife() {
        tickAndServe(52);
        tick(8);
        assertEquals(2, game.getLives());
        assertTrue(game.getMessage().contains("1 customer was"), game.getMessage());
    }

    @Test
    public void leftoverCustomersBeyondYourLivesLoseTheGame() {
        tick(60); // never served anyone: 1 turned away, 6 left in line
        assertEquals(0, game.getLives());
        assertEquals(GameState.Status.LOST, game.getStatus());
        assertFalse(game.isBetweenDays());
    }

    @Test
    public void emptyLineAtClosingCostsNothing() {
        tickAndServe(60);
        assertEquals(3, game.getLives());
        assertEquals("", game.getMessage());
    }

    @Test
    public void noCustomerArrivesInTheLastThreeSeconds() {
        for (long seed = 0; seed < 60; seed++) {
            game = new GameState(new Random(seed));
            for (int second : arrivalSecondsOnDayOne()) {
                assertTrue(second <= GameState.SECONDS_PER_DAY
                    - GameState.CLOSING_SECONDS_WITHOUT_ARRIVALS,
                    "seed " + seed + " arrival at " + second);
            }
        }
    }

    @Test
    public void closingSecondBringsNoArrival() {
        // Days 3, 5, 6 and 7 have gaps that divide 60, so without a quiet
        // period at closing a customer would arrive on the last second and
        // always cost a life.
        tickAndServe(180);
        assertEquals(3, game.getLives());
        assertEquals(0, game.getQueueSize());
    }

    @Test
    public void leftoverCustomersAtTheEndOfDaySevenCountToo() {
        tickAndServe(7 * 60 - 6);
        tick(5); // one customer arrives at 55 s and is left waiting
        tick(1);
        assertEquals(GameState.Status.WON, game.getStatus());
        assertEquals(2, game.getLives());
    }

    // ---- random arrival gaps ----

    /** Ticks the first day, serving at once, and returns each arrival second. */
    private List<Integer> arrivalSecondsOnDayOne() {
        List<Integer> seconds = new java.util.ArrayList<>();
        for (int t = 1; t <= GameState.SECONDS_PER_DAY; t++) {
            game.tick();
            if (game.getQueueSize() > 0) {
                seconds.add(t);
                serveFront();
            }
        }
        return seconds;
    }

    private static List<Integer> gapsBetween(List<Integer> seconds) {
        List<Integer> gaps = new java.util.ArrayList<>();
        int previous = 0;
        for (int s : seconds) {
            gaps.add(s - previous);
            previous = s;
        }
        return gaps;
    }

    @Test
    public void jitteredGapsVaryButStayNearTheDaysInterval() {
        game = new GameState(new Random(7));
        List<Integer> gaps = gapsBetween(arrivalSecondsOnDayOne());
        assertTrue(gaps.size() >= 5);
        for (int gap : gaps) {
            assertTrue(gap >= 8 - GameState.ARRIVAL_JITTER_SECONDS
                && gap <= 8 + GameState.ARRIVAL_JITTER_SECONDS, "gap " + gap);
        }
        assertTrue(new java.util.HashSet<>(gaps).size() > 1,
            "gaps should not all be equal: " + gaps);
    }

    @Test
    public void gapsNeverDropBelowTheAbsoluteMinimum() {
        game = new GameState(new Random(3), 10);
        for (int gap : gapsBetween(arrivalSecondsOnDayOne())) {
            assertTrue(gap >= GameState.MIN_ARRIVAL_GAP_SECONDS, "gap " + gap);
        }
    }

    @Test
    public void arrivalsGetFasterEachDayUpToTheFinalDay() {
        int previous = 0;
        int dayOne = 0;
        int dayServen = 0;
        for (int day = 1; day <= GameState.TOTAL_DAYS; day++) {
            if (game.isBetweenDays()) {
                game.startNextDay();
            }
            int arrivals = 0;
            for (int t = 0; t < GameState.SECONDS_PER_DAY; t++) {
                game.tick();
                if (game.getQueueSize() > 0) {
                    arrivals++;
                    serveFront();
                }
            }
            assertTrue(arrivals >= previous, "day " + day + ": " + arrivals);
            previous = arrivals;
            if (day == 1) {
                dayOne = arrivals;
            }
            dayServen = arrivals;
        }
        assertTrue(dayServen >= 2 * dayOne, dayOne + " vs " + dayServen);
    }

    @Test
    public void finalDayGapsAreTwoToFourSeconds() {
        game = new GameState(new Random(5));
        tickAndServe(6 * GameState.SECONDS_PER_DAY);
        game.startNextDay();
        assertEquals(7, game.getDay());
        List<Integer> seconds = new java.util.ArrayList<>();
        for (int t = 1; t <= GameState.SECONDS_PER_DAY; t++) {
            game.tick();
            if (game.getQueueSize() > 0) {
                seconds.add(t);
                serveFront();
            }
        }
        List<Integer> gaps = gapsBetween(seconds);
        assertTrue(gaps.size() >= 12, "arrivals: " + gaps);
        for (int gap : gaps) {
            assertTrue(gap >= 2 && gap <= 4, "gap " + gap + " in " + gaps);
        }
        assertTrue(new java.util.HashSet<>(gaps).size() > 1, "no variety");
    }

    @Test
    public void sameSeedGivesSameJitteredArrivals() {
        game = new GameState(new Random(11));
        List<Integer> first = arrivalSecondsOnDayOne();
        game = new GameState(new Random(11));
        assertEquals(first, arrivalSecondsOnDayOne());
    }

    @Test
    public void negativeJitterIsRejected() {
        assertThrows(IllegalArgumentException.class,
            () -> new GameState(new Random(1), -1));
        assertThrows(IllegalArgumentException.class,
            () -> new GameState(null, 0));
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
        GameState custom = new GameState(new Random(1), 0);
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
        tick(60); // lost, see leftoverCustomersBeyondYourLivesLoseTheGame
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
        tick(59);
        assertEquals(6, game.getQueueSize());
        assertEquals(6, game.getQueueCapacity());
    }
}
