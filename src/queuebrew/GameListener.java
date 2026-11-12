package queuebrew;

/**
 * Told by the model after every change so a view can redraw.
 */
public interface GameListener {

    /**
     * Called after the game state changes. The listener re-reads the
     * {@link GameInfo} getters.
     */
    void gameChanged();
}
