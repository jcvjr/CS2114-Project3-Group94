package queuebrew;

import java.util.Collections;
import java.util.Random;

/**
 * Plays a whole game with no window, to show the model stands on its own. The
 * simulated player makes the front order correctly (one ingredient per second)
 * and completes it. Pass "lazy" to make the player never serve, which shows a
 * loss.
 */
public class ModelDriver {

    /**
     * @param args
     *            optional: "lazy" for a player who never serves anyone
     */
    public static void main(String[] args) {
        boolean lazy = args.length > 0 && args[0].equals("lazy");
        GameState game = new GameState(new Random(94)); // random arrival gaps
        game.addListener(() -> {
            if (!game.getMessage().isEmpty()) {
                System.out.printf("  day %d %s  lives=%d  %s%n", game.getDay(),
                    TimeUtil.formatTime(game.getSecondsLeftInDay()),
                    game.getLives(), game.getMessage());
            }
        });
        int seconds = 0;
        while (game.getStatus() == GameState.Status.PLAYING) {
            if (game.isBetweenDays()) {
                System.out.println("Day " + (game.getDay() - 1) + " complete!");
                game.startNextDay();
            }
            game.tick();
            seconds++;
            if (!lazy) {
                playOneSecond(game);
            }
            if (seconds % GameState.SECONDS_PER_DAY == 0) {
                System.out.printf("[after %d s] day %d, line %d/%d, lives %d%n",
                    seconds, game.getDay(), game.getQueueSize(),
                    game.getQueueCapacity(), game.getLives());
            }
        }
        System.out.println("Final status: " + game.getStatus() + " after "
            + seconds + " seconds");
    }

    /** Adds the next missing ingredient, or completes when the cup is right. */
    private static void playOneSecond(GameState game) {
        Customer[] waiting = game.getWaitingCustomers();
        if (waiting.length == 0) {
            return;
        }
        Order order = waiting[0].getOrder();
        if (order.matches(game.getCup())) {
            game.completeFrontOrder();
            return;
        }
        for (String needed : order.getIngredients()) {
            if (Collections.frequency(game.getCup(), needed) < Collections
                .frequency(order.getIngredients(), needed)) {
                game.addIngredient(needed);
                return;
            }
        }
    }
}
