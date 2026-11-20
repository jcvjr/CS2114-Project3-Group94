package queuebrew;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * Thin view: draws what {@link GameInfo} reports and passes the player's clicks
 * (ingredients, Clear Cup, Complete Order, Play Again) to the controller. Keeps
 * no copy of the game state between redraws.
 *
 * The window has two screens: the game, and a win/lose screen that replaces it
 * when the game ends. Components have names (for example "completeButton") so
 * tests can find them.
 */
public class QueueBrewGUI implements GameListener {

    private static final String GAME_SCREEN = "gameScreen";
    private static final String END_SCREEN = "endScreen";

    private final GameInfo info;
    private final QueueBrew controller;
    private boolean dayDialogOpen;
    private final JFrame frame;
    private final CardLayout cards;
    private final JPanel screens;
    private final JPanel gameScreen;
    private final JPanel endScreen;
    private final JLabel endTitleLabel;
    private final JLabel endDetailLabel;
    private final JLabel endStatsLabel;
    private final JLabel dayLabel;
    private final JLabel timeLabel;
    private final JLabel livesLabel;
    private final JLabel queueCountLabel;
    private final JLabel messageLabel;
    private final JLabel cupLabel;
    private final JLabel patienceLabel;
    private final DefaultListModel<String> queueModel;
    private final List<JButton> ingredientButtons;
    private final JButton clearButton;
    private final JButton completeButton;

    /**
     * Builds the window and draws the starting state.
     *
     * @param info
     *            read-only access to the game
     * @param controller
     *            told when the player clicks something
     */
    public QueueBrewGUI(GameInfo info, QueueBrew controller) {
        this.info = info;
        this.controller = controller;
        frame = new JFrame("Queue Brew");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        cards = new CardLayout();
        screens = new JPanel(cards);
        gameScreen = new JPanel(new BorderLayout(8, 8));
        gameScreen.setName(GAME_SCREEN);

        dayLabel = named(new JLabel(), "dayLabel");
        timeLabel = named(new JLabel(), "timeLabel");
        livesLabel = named(new JLabel(), "livesLabel");
        queueCountLabel = named(new JLabel(), "queueCountLabel");
        JPanel top = new JPanel(new GridLayout(2, 2));
        top.add(dayLabel);
        top.add(timeLabel);
        top.add(livesLabel);
        top.add(queueCountLabel);
        gameScreen.add(top, BorderLayout.NORTH);

        queueModel = new DefaultListModel<>();
        JList<String> queueList = new JList<>(queueModel);
        queueList.setName("queueList");
        gameScreen.add(new JScrollPane(queueList), BorderLayout.CENTER);

        JTextArea recipeCard = new JTextArea(
            "RECIPES\n" + String.join("\n", info.getRecipeCard()));
        recipeCard.setEditable(false);
        gameScreen.add(recipeCard, BorderLayout.EAST);

        ingredientButtons = new ArrayList<>();
        JPanel ingredients = new JPanel(new FlowLayout());
        for (String name : info.getIngredients()) {
            JButton button = new JButton(name);
            button.setName("ingredient-" + name);
            button.addActionListener(e -> controller.onIngredientClicked(name));
            ingredientButtons.add(button);
            ingredients.add(button);
        }
        cupLabel = named(new JLabel(" ", SwingConstants.CENTER), "cupLabel");
        patienceLabel = named(new JLabel(" ", SwingConstants.CENTER),
            "patienceLabel");
        messageLabel = named(new JLabel(" ", SwingConstants.CENTER),
            "messageLabel");
        clearButton = new JButton("Clear Cup");
        clearButton.setName("clearButton");
        clearButton.addActionListener(e -> controller.onClearCupClicked());
        completeButton = new JButton("Complete Order");
        completeButton.setName("completeButton");
        completeButton.addActionListener(e -> controller.onCompleteClicked());
        JPanel actions = new JPanel(new GridLayout(1, 2));
        actions.add(clearButton);
        actions.add(completeButton);

        JPanel bottom = new JPanel(new GridLayout(5, 1));
        bottom.add(patienceLabel);
        bottom.add(messageLabel);
        bottom.add(cupLabel);
        bottom.add(ingredients);
        bottom.add(actions);
        gameScreen.add(bottom, BorderLayout.SOUTH);

        endScreen = new JPanel(new GridLayout(4, 1, 8, 8));
        endScreen.setName(END_SCREEN);
        endScreen.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        endTitleLabel = named(new JLabel(" ", SwingConstants.CENTER),
            "endTitleLabel");
        endTitleLabel.setFont(endTitleLabel.getFont().deriveFont(Font.BOLD, 40f));
        endDetailLabel = named(new JLabel(" ", SwingConstants.CENTER),
            "endDetailLabel");
        endStatsLabel = named(new JLabel(" ", SwingConstants.CENTER),
            "endStatsLabel");
        JButton playAgainButton = new JButton("Play Again");
        playAgainButton.setName("playAgainButton");
        playAgainButton.addActionListener(e -> controller.onPlayAgainClicked());
        endScreen.add(endTitleLabel);
        endScreen.add(endDetailLabel);
        endScreen.add(endStatsLabel);
        endScreen.add(playAgainButton);

        screens.add(gameScreen, GAME_SCREEN);
        screens.add(endScreen, END_SCREEN);
        frame.add(screens);

        frame.setSize(640, 520);
        gameChanged();
    }

    private static JLabel named(JLabel label, String name) {
        label.setName(name);
        return label;
    }

    /**
     * Makes the game window visible.
     */
    public void display() {
        frame.setVisible(true);
    }

    /** @return the window, so tests in this package can look inside it */
    JFrame getFrame() {
        return frame;
    }

    @Override
    public void gameChanged() {
        if (info.getStatus() != GameState.Status.PLAYING) {
            showEndScreen();
            return;
        }
        cards.show(screens, GAME_SCREEN);

        dayLabel.setText("Day " + info.getDay() + " of "
            + GameState.TOTAL_DAYS);
        timeLabel.setText("Time left: "
            + TimeUtil.formatTime(info.getSecondsLeftInDay()));
        livesLabel.setText("Lives: " + info.getLives());
        queueCountLabel.setText("In line: " + info.getQueueSize() + " / "
            + info.getQueueCapacity());

        queueModel.clear();
        int position = 1;
        for (Customer c : info.getWaitingCustomers()) {
            queueModel.addElement(position + ". " + c.getName() + " - "
                + c.getOrder().getDrink());
            position++;
        }

        List<String> cup = info.getCup();
        cupLabel.setText(cup.isEmpty() ? "Cup: (empty)"
            : "Cup: " + String.join(" + ", cup));

        patienceLabel.setText(info.getFrontPatienceLeft() > 0
            ? "Front customer will leave in " + info.getFrontPatienceLeft() + "s"
            : " ");

        String text = info.getMessage();
        if (info.getPenaltySecondsLeft() > 0) {
            text = "Wrong order! Cleaning up the spill: "
                + info.getPenaltySecondsLeft() + "s";
        }
        messageLabel.setText(text.isEmpty() ? " " : text);

        boolean canAct = !info.isBetweenDays()
            && info.getPenaltySecondsLeft() == 0;
        completeButton.setEnabled(canAct);
        clearButton.setEnabled(canAct);
        for (JButton button : ingredientButtons) {
            button.setEnabled(canAct);
        }

        if (info.isBetweenDays() && !dayDialogOpen) {
            SwingUtilities.invokeLater(this::showDayCompleteDialog);
        }
    }

    /** Replaces the game with the win or lose screen. */
    private void showEndScreen() {
        boolean won = info.getStatus() == GameState.Status.WON;
        endTitleLabel.setText(won ? "YOU WIN!" : "GAME OVER");
        if (won) {
            endDetailLabel.setText("You kept the shop running for all "
                + GameState.TOTAL_DAYS + " days.");
            endStatsLabel.setText("Lives left: " + info.getLives());
        }
        else {
            endDetailLabel.setText(info.getMessage().isEmpty()
                ? "You ran out of lives." : info.getMessage());
            endStatsLabel.setText("You reached day " + info.getDay() + " of "
                + GameState.TOTAL_DAYS + ".");
        }
        cards.show(screens, END_SCREEN);
    }

    /** Shows the end-of-day popup, then tells the controller to go on. */
    private void showDayCompleteDialog() {
        if (dayDialogOpen || !info.isBetweenDays()) {
            return;
        }
        dayDialogOpen = true;
        int finished = info.getDay() - 1;
        JOptionPane.showMessageDialog(frame,
            "Congratulations! You completed day " + finished + ".\n"
                + (info.getMessage().isEmpty() ? "" : info.getMessage() + "\n")
                + "Lives left: " + info.getLives() + "\n"
                + "The shop closes for the night and the line clears.\n\n"
                + "Click OK to start day " + info.getDay() + ".",
            "Day " + finished + " complete", JOptionPane.INFORMATION_MESSAGE);
        dayDialogOpen = false;
        controller.onNextDayClicked();
    }
}
