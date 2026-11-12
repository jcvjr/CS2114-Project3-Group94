package queuebrew;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The game model: the line, lives, day, clock, arrivals and win/lose rules.
 * Knows nothing about Swing or the controller; its only outward call is
 * {@link GameListener#gameChanged()}.
 */
public class GameState implements GameInfo {

    /** Where the game stands. */
    public enum Status {
        PLAYING, WON, LOST
    }

    /** Lives the player starts with. */
    public static final int STARTING_LIVES = 3;
    /** Number of days in a game. */
    public static final int TOTAL_DAYS = 7;
    /** Length of each day in seconds. */
    public static final int SECONDS_PER_DAY = 60;
    /** Number of slots in the line. */
    public static final int QUEUE_CAPACITY = 6;
    /** Seconds between arrivals on day 1. */
    public static final int START_ARRIVAL_INTERVAL_SECONDS = 8;
    /** How much the arrival interval shrinks each day after day 1. */
    public static final int ARRIVAL_INTERVAL_DECREASE_PER_DAY = 1;
    /** The shortest allowed arrival interval. */
    public static final int MIN_ARRIVAL_INTERVAL_SECONDS = 5;

    /** Message when the player completes an order with nobody in line. */
    public static final String EMPTY_MESSAGE =
        "Queue is empty, cannot remove orders at this time.";
    /** Message when a customer arrives to a full line. */
    public static final String TURNED_AWAY_MESSAGE =
        "The line was full - a customer was turned away!";

    /** Most ingredients a cup can hold. */
    public static final int CUP_CAPACITY = 6;
    /** Message when the player tries to complete with an empty cup. */
    public static final String EMPTY_CUP_MESSAGE =
        "The cup is empty - add ingredients first.";
    /** Message when the cup does not match the front order. */
    public static final String WRONG_ORDER_MESSAGE =
        "Wrong order! The cup was dumped - try again.";
    /** Seconds the player is locked out after serving a wrong order. */
    public static final int WRONG_ORDER_PENALTY_SECONDS = 3;
    /** Message when the player adds to a full cup. */
    public static final String CUP_FULL_MESSAGE = "The cup is full.";

    private static final String[] NAMES = { "Alex", "Sam", "Mia", "Jordan",
        "Riley", "Taylor", "Casey", "Morgan", "Jamie", "Avery" };

    private final Line line;
    private final List<String> cup;
    private final Random random;
    private final List<GameListener> listeners;
    private int lives;
    private int day;
    private int secondsLeftInDay;
    private int secondsSinceArrival;
    private int penaltySecondsLeft;
    private Status status;
    private String message;

    /**
     * Creates a new game with its own Random.
     */
    public GameState() {
        this(new Random());
    }

    /**
     * Creates a new game using the given Random so arrivals can be repeated in
     * tests.
     *
     * @param random
     *            the source of random names and drinks
     * @throws IllegalArgumentException
     *             if random is null
     */
    public GameState(Random random) {
        if (random == null) {
            throw new IllegalArgumentException("random must not be null");
        }
        this.random = random;
        line = new Line(QUEUE_CAPACITY);
        cup = new ArrayList<>();
        listeners = new ArrayList<>();
        lives = STARTING_LIVES;
        day = 1;
        secondsLeftInDay = SECONDS_PER_DAY;
        secondsSinceArrival = 0;
        status = Status.PLAYING;
        message = "";
    }

    /**
     * Registers a listener to be told whenever the state changes.
     *
     * @param listener
     *            the listener
     * @throws IllegalArgumentException
     *             if listener is null
     */
    public void addListener(GameListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        listeners.add(listener);
    }

    /**
     * Advances the game by one second. Does nothing if the game is over.
     */
    public void tick() {
        if (status != Status.PLAYING) {
            return;
        }
        message = "";
        if (penaltySecondsLeft > 0) {
            penaltySecondsLeft--;
        }
        secondsLeftInDay--;
        secondsSinceArrival++;
        if (secondsSinceArrival >= arrivalIntervalForDay(day)) {
            secondsSinceArrival = 0;
            addArrival();
        }
        if (status == Status.PLAYING && secondsLeftInDay == 0) {
            endDay();
        }
        notifyListeners();
    }

    /**
     * Adds one ingredient to the cup.
     *
     * @param ingredient
     *            an ingredient from the menu
     * @return true if added; false if the cup is full or the game is over
     * @throws IllegalArgumentException
     *             if ingredient is null or not on the menu
     */
    public boolean addIngredient(String ingredient) {
        if (!Menu.isIngredient(ingredient)) {
            throw new IllegalArgumentException("unknown ingredient: "
                + ingredient);
        }
        if (status != Status.PLAYING || penaltySecondsLeft > 0) {
            return false;
        }
        if (cup.size() >= CUP_CAPACITY) {
            message = CUP_FULL_MESSAGE;
            notifyListeners();
            return false;
        }
        cup.add(ingredient);
        message = "";
        notifyListeners();
        return true;
    }

    /**
     * Empties the cup. Does nothing if the game is over or the player is
     * serving a wrong-order penalty.
     */
    public void clearCup() {
        if (status != Status.PLAYING || penaltySecondsLeft > 0) {
            return;
        }
        cup.clear();
        message = "";
        notifyListeners();
    }

    /**
     * Serves the front customer if the cup matches their recipe. A wrong cup
     * is dumped, the customer keeps waiting, and the player cannot act for
     * {@link #WRONG_ORDER_PENALTY_SECONDS} seconds; no life is lost.
     *
     * @return true if the front customer was served; false if the line was
     *         empty, the cup was empty or wrong, the player is serving a
     *         penalty, or the game is over
     */
    public boolean completeFrontOrder() {
        if (status != Status.PLAYING || penaltySecondsLeft > 0) {
            return false;
        }
        Customer front = line.peek();
        boolean served = false;
        if (front == null) {
            message = EMPTY_MESSAGE;
        }
        else if (cup.isEmpty()) {
            message = EMPTY_CUP_MESSAGE;
        }
        else if (!front.getOrder().matches(cup)) {
            cup.clear();
            penaltySecondsLeft = WRONG_ORDER_PENALTY_SECONDS;
            message = WRONG_ORDER_MESSAGE;
        }
        else {
            line.dequeue();
            cup.clear();
            message = "";
            served = true;
        }
        notifyListeners();
        return served;
    }

    private void addArrival() {
        Customer arriving = new Customer(NAMES[random.nextInt(NAMES.length)],
            Menu.randomOrder(random));
        if (!line.enqueue(arriving)) {
            lives--;
            message = TURNED_AWAY_MESSAGE;
            if (lives <= 0) {
                status = Status.LOST;
            }
        }
    }

    private void endDay() {
        if (day == TOTAL_DAYS) {
            status = Status.WON;
        }
        else {
            day++;
            secondsLeftInDay = SECONDS_PER_DAY;
            secondsSinceArrival = 0;
        }
    }

    private static int arrivalIntervalForDay(int day) {
        return Math.max(MIN_ARRIVAL_INTERVAL_SECONDS,
            START_ARRIVAL_INTERVAL_SECONDS
                - ARRIVAL_INTERVAL_DECREASE_PER_DAY * (day - 1));
    }

    private void notifyListeners() {
        for (GameListener listener : listeners) {
            listener.gameChanged();
        }
    }

    @Override
    public int getLives() {
        return lives;
    }

    @Override
    public int getDay() {
        return day;
    }

    @Override
    public int getSecondsLeftInDay() {
        return secondsLeftInDay;
    }

    @Override
    public int getQueueSize() {
        return line.getSize();
    }

    @Override
    public int getQueueCapacity() {
        return line.getCapacity();
    }

    @Override
    public Customer[] getWaitingCustomers() {
        return line.toArray();
    }

    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public int getPenaltySecondsLeft() {
        return penaltySecondsLeft;
    }

    @Override
    public List<String> getCup() {
        return new ArrayList<>(cup);
    }

    @Override
    public List<String> getIngredients() {
        return Menu.getIngredients();
    }

    @Override
    public List<String> getRecipeCard() {
        return Menu.getRecipeCard();
    }
}
