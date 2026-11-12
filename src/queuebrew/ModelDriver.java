package queuebrew;

import java.util.Random;

/**
 * Plays a whole game with no window, to show the model stands on its own. The
 * simulated player completes the front order every few seconds. Run with an
 * optional seconds-between-completions argument (default 2, a win); a big number such
 * as 100 shows a loss.
 */
public class ModelDriver {

    /**
     * @param args
     *            optional: seconds between order completions
     */
    public static void main(String[] args) {
        int every = args.length > 0 ? Integer.parseInt(args[0]) : 2;
        GameState game = new GameState(new Random(94));
        game.addListener(() -> {
            if (!game.getMessage().isEmpty()) {
                System.out.printf("  day %d %s  lives=%d  %s%n", game.getDay(),
                    TimeUtil.formatTime(game.getSecondsLeftInDay()),
                    game.getLives(), game.getMessage());
            }
        });
        int seconds = 0;
        while (game.getStatus() == GameState.Status.PLAYING) {
            game.tick();
            seconds++;
            if (seconds % every == 0) {
                game.completeFrontOrder();
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
}
