package queuebrew;

/**
 * Read-only view of the game, which is all the view is allowed to see.
 */
public interface GameInfo {

    /**
     * @return lives the player has left
     */
    int getLives();

    /**
     * @return the current day, 1 to the total number of days
     */
    int getDay();

    /**
     * @return seconds left in the current day
     */
    int getSecondsLeftInDay();

    /**
     * @return how many customers are in line
     */
    int getQueueSize();

    /**
     * @return the maximum number of customers in line
     */
    int getQueueCapacity();

    /**
     * @return a copy of the waiting customers, front first
     */
    Customer[] getWaitingCustomers();

    /**
     * @return PLAYING, WON or LOST
     */
    GameState.Status getStatus();

    /**
     * @return the latest message for the player, or "" if there is none
     */
    String getMessage();

    /**
     * @return seconds left in the wrong-order penalty, during which the player
     *         cannot act; 0 when there is no penalty
     */
    int getPenaltySecondsLeft();

    /**
     * @return a copy of the ingredients currently in the cup, in the order
     *         they were added
     */
    java.util.List<String> getCup();

    /**
     * @return the ingredients the player can add
     */
    java.util.List<String> getIngredients();

    /**
     * @return one line per drink showing its recipe
     */
    java.util.List<String> getRecipeCard();
}
