package queuebrew;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Random;
import javax.swing.AbstractButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the view and controller together with a real model: clicks must flow
 * through the controller into the model, and the view must show what the model
 * reports. The window is never shown on screen, and the tests are skipped on a
 * machine with no display. The game clock is driven by calling the model's
 * tick() directly, so nothing here waits for real time.
 */
public class QueueBrewGUITest {

    private GameState game;
    private QueueBrew controller;
    private JFrame frame;

    @BeforeEach
    public void setUp() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
            "needs a display");
        // exact arrival timing, and patience off so nobody walks out
        startWith(new GameState(new Random(1), 0, 100000));
    }

    @AfterEach
    public void tearDown() {
        if (frame != null) {
            onEdt(frame::dispose);
        }
    }

    // ---- helpers ----

    private void startWith(GameState newGame) {
        if (frame != null) {
            onEdt(frame::dispose);
        }
        game = newGame;
        onEdt(() -> {
            controller = new QueueBrew(game);
            frame = controller.getGui().getFrame();
        });
    }

    private static void onEdt(Runnable task) {
        try {
            SwingUtilities.invokeAndWait(task);
        }
        catch (InterruptedException e) {
            throw new AssertionError(e);
        }
        catch (InvocationTargetException e) {
            if (e.getCause() instanceof AssertionError) {
                throw (AssertionError) e.getCause();
            }
            throw new AssertionError(e.getCause());
        }
    }

    private static Component search(Container root, String name) {
        for (Component c : root.getComponents()) {
            if (name.equals(c.getName())) {
                return c;
            }
            if (c instanceof Container) {
                Component found = search((Container) c, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private Component find(String name) {
        Component c = search(frame.getContentPane(), name);
        assertNotNull(c, "no component named " + name);
        return c;
    }

    private String text(String labelName) {
        return ((JLabel) find(labelName)).getText();
    }

    private void click(String buttonName) {
        AbstractButton button = (AbstractButton) find(buttonName);
        onEdt(button::doClick);
    }

    private boolean enabled(String name) {
        return find(name).isEnabled();
    }

    private int listSize() {
        return ((JList<?>) find("queueList")).getModel().getSize();
    }

    /** Advances the model by whole seconds. */
    private void ticks(int times) {
        onEdt(() -> {
            for (int i = 0; i < times; i++) {
                game.tick();
            }
        });
    }

    /** Plays the whole game through the model, serving every customer. */
    private void playAWinningGame() {
        onEdt(() -> {
            while (game.getStatus() == GameState.Status.PLAYING) {
                if (game.isBetweenDays()) {
                    // done in the same task, so the popup never gets to open
                    game.startNextDay();
                }
                game.tick();
                Customer[] waiting = game.getWaitingCustomers();
                if (waiting.length > 0) {
                    for (String item : waiting[0].getOrder().getIngredients()) {
                        game.addIngredient(item);
                    }
                    game.completeFrontOrder();
                }
            }
        });
    }

    // ---- what the view shows ----

    @Test
    public void startingScreenShowsTheFreshGame() {
        assertEquals("Day 1 of 7", text("dayLabel"));
        assertEquals("Time left: 1:00", text("timeLabel"));
        assertEquals("Lives: 3", text("livesLabel"));
        assertEquals("In line: 0 / 6", text("queueCountLabel"));
        assertEquals("Cup: (empty)", text("cupLabel"));
        assertTrue(find("gameScreen").isVisible());
        assertFalse(find("endScreen").isVisible());
    }

    @Test
    public void viewFollowsTheModelAsTimePasses() {
        ticks(8);
        assertEquals("Time left: 0:52", text("timeLabel"));
        assertEquals("In line: 1 / 6", text("queueCountLabel"));
        assertEquals(1, listSize());
    }

    @Test
    public void frontCustomerCountdownIsShownWhenSomeoneIsWaiting() {
        startWith(new GameState(new Random(1), 0));
        assertEquals(" ", text("patienceLabel"));
        ticks(8);
        assertEquals("Front customer will leave in 15s", text("patienceLabel"));
    }

    // ---- clicks flow through the controller into the model ----

    @Test
    public void ingredientClickReachesTheModelAndTheCupLabel() {
        click("ingredient-Milk");
        click("ingredient-Espresso");
        assertEquals(List.of("Milk", "Espresso"), game.getCup());
        assertEquals("Cup: Milk + Espresso", text("cupLabel"));
    }

    @Test
    public void clearCupButtonEmptiesTheCup() {
        click("ingredient-Milk");
        click("clearButton");
        assertTrue(game.getCup().isEmpty());
        assertEquals("Cup: (empty)", text("cupLabel"));
    }

    @Test
    public void makingTheDrinkAndCompletingServesTheFrontCustomer() {
        ticks(8);
        assertEquals(1, listSize());
        Customer front = game.getWaitingCustomers()[0];
        for (String item : front.getOrder().getIngredients()) {
            click("ingredient-" + item);
        }
        click("completeButton");
        assertEquals(0, game.getQueueSize());
        assertEquals(0, listSize());
        assertEquals("Cup: (empty)", text("cupLabel"));
        assertEquals("In line: 0 / 6", text("queueCountLabel"));
    }

    // ---- bad input through the view ----

    @Test
    public void completingWithNobodyInLineShowsTheEmptyQueueMessage() {
        click("completeButton");
        assertEquals(GameState.EMPTY_MESSAGE, text("messageLabel"));
        assertEquals("Lives: 3", text("livesLabel"));
        assertTrue(find("gameScreen").isVisible());
    }

    @Test
    public void completingWithAnEmptyCupShowsAMessageAndKeepsTheCustomer() {
        ticks(8);
        click("completeButton");
        assertEquals(GameState.EMPTY_CUP_MESSAGE, text("messageLabel"));
        assertEquals(1, listSize());
    }

    @Test
    public void wrongOrderShowsTheLockoutAndDisablesTheButtons() {
        ticks(8);
        click("ingredient-Foam");
        click("ingredient-Foam");
        click("ingredient-Foam");
        click("completeButton");
        assertTrue(text("messageLabel").startsWith("Wrong order!"),
            text("messageLabel"));
        assertFalse(enabled("completeButton"));
        assertFalse(enabled("clearButton"));
        assertFalse(enabled("ingredient-Milk"));
        assertEquals(1, listSize());
        click("ingredient-Milk"); // disabled: nothing happens
        assertTrue(game.getCup().isEmpty());
        ticks(GameState.WRONG_ORDER_PENALTY_SECONDS);
        assertTrue(enabled("completeButton"));
        assertTrue(enabled("ingredient-Milk"));
    }

    @Test
    public void fullCupMessageIsShown() {
        for (int i = 0; i < GameState.CUP_CAPACITY + 1; i++) {
            click("ingredient-Milk");
        }
        assertEquals(GameState.CUP_FULL_MESSAGE, text("messageLabel"));
        assertEquals(GameState.CUP_CAPACITY, game.getCup().size());
    }

    // ---- win / lose screen and restart ----

    @Test
    public void endScreenStaysHiddenWhileThePlayerIsPlaying() {
        ticks(30);
        assertTrue(find("gameScreen").isVisible());
        assertFalse(find("endScreen").isVisible());
    }

    @Test
    public void losingShowsTheGameOverScreenWithTheReason() {
        ticks(60); // never served anyone: the line is full and left waiting
        assertEquals(GameState.Status.LOST, game.getStatus());
        assertTrue(find("endScreen").isVisible());
        assertFalse(find("gameScreen").isVisible());
        assertEquals("GAME OVER", text("endTitleLabel"));
        assertTrue(text("endDetailLabel").contains("still in line at closing"),
            text("endDetailLabel"));
        assertEquals("You reached day 1 of 7.", text("endStatsLabel"));
    }

    @Test
    public void winningShowsTheYouWinScreen() {
        playAWinningGame();
        assertEquals(GameState.Status.WON, game.getStatus());
        assertTrue(find("endScreen").isVisible());
        assertFalse(find("gameScreen").isVisible());
        assertEquals("YOU WIN!", text("endTitleLabel"));
        assertTrue(text("endDetailLabel").contains("7 days"),
            text("endDetailLabel"));
        assertEquals("Lives left: 3", text("endStatsLabel"));
    }

    @Test
    public void playAgainStartsANewGameAndBringsBackTheGameScreen() {
        ticks(60);
        assertEquals(GameState.Status.LOST, game.getStatus());
        click("playAgainButton");
        assertEquals(GameState.Status.PLAYING, game.getStatus());
        assertEquals(3, game.getLives());
        assertEquals(1, game.getDay());
        assertTrue(find("gameScreen").isVisible());
        assertFalse(find("endScreen").isVisible());
        assertEquals("Day 1 of 7", text("dayLabel"));
        assertEquals("Lives: 3", text("livesLabel"));
        assertEquals("In line: 0 / 6", text("queueCountLabel"));
        assertTrue(enabled("completeButton"));
        ticks(8);
        assertEquals(1, listSize()); // the new game really runs
    }

    @Test
    public void playAgainWorksAfterAWinToo() {
        playAWinningGame();
        click("playAgainButton");
        assertEquals(GameState.Status.PLAYING, game.getStatus());
        assertEquals("Day 1 of 7", text("dayLabel"));
        assertTrue(find("gameScreen").isVisible());
    }
}
