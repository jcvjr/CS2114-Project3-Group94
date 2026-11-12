package queuebrew;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Controller: creates and wires the model and view, owns the one-second timer,
 * and forwards the player's click to the model.
 */
public class QueueBrew {

    private final GameState game;
    private final QueueBrewGUI gui;
    private final Timer timer;

    /**
     * Creates the model and view and wires them together. The timer is not
     * started until {@link #start()}.
     */
    public QueueBrew() {
        game = new GameState();
        gui = new QueueBrewGUI(game, this);
        game.addListener(gui);
        timer = new Timer(1000, e -> onTimerTick());
    }

    /**
     * Shows the window and starts the clock.
     */
    public void start() {
        gui.display();
        timer.start();
    }

    /**
     * Called by the view when the player clicks an ingredient button.
     *
     * @param ingredient
     *            the ingredient to add to the cup
     */
    public void onIngredientClicked(String ingredient) {
        game.addIngredient(ingredient);
    }

    /**
     * Called by the view when the player dismisses the end-of-day message.
     */
    public void onNextDayClicked() {
        game.startNextDay();
    }

    /**
     * Called by the view when the player clicks Clear Cup.
     */
    public void onClearCupClicked() {
        game.clearCup();
    }

    /**
     * Called by the view when the player clicks Complete Order.
     */
    public void onCompleteClicked() {
        game.completeFrontOrder();
    }

    private void onTimerTick() {
        game.tick();
        if (game.getStatus() != GameState.Status.PLAYING) {
            timer.stop();
        }
    }

    /**
     * Starts the game.
     *
     * @param args
     *            unused
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new QueueBrew().start());
    }
}
