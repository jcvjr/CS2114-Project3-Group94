package queuebrew;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
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
 * (ingredients, Clear Cup, Complete Order) to the controller. Keeps no copy of
 * the game state between redraws.
 */
public class QueueBrewGUI implements GameListener {

    private final GameInfo info;
    private final QueueBrew controller;
    private boolean dayDialogOpen;
    private final JFrame frame;
    private final JLabel dayLabel;
    private final JLabel timeLabel;
    private final JLabel livesLabel;
    private final JLabel queueCountLabel;
    private final JLabel messageLabel;
    private final JLabel cupLabel;
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
        frame.setLayout(new BorderLayout(8, 8));

        dayLabel = new JLabel();
        timeLabel = new JLabel();
        livesLabel = new JLabel();
        queueCountLabel = new JLabel();
        JPanel top = new JPanel(new GridLayout(2, 2));
        top.add(dayLabel);
        top.add(timeLabel);
        top.add(livesLabel);
        top.add(queueCountLabel);
        frame.add(top, BorderLayout.NORTH);

        queueModel = new DefaultListModel<>();
        frame.add(new JScrollPane(new JList<>(queueModel)), BorderLayout.CENTER);

        JTextArea recipeCard = new JTextArea(
            "RECIPES\n" + String.join("\n", info.getRecipeCard()));
        recipeCard.setEditable(false);
        frame.add(recipeCard, BorderLayout.EAST);

        ingredientButtons = new ArrayList<>();
        JPanel ingredients = new JPanel(new FlowLayout());
        for (String name : info.getIngredients()) {
            JButton button = new JButton(name);
            button.addActionListener(e -> controller.onIngredientClicked(name));
            ingredientButtons.add(button);
            ingredients.add(button);
        }
        cupLabel = new JLabel(" ", SwingConstants.CENTER);
        messageLabel = new JLabel(" ", SwingConstants.CENTER);
        clearButton = new JButton("Clear Cup");
        clearButton.addActionListener(e -> controller.onClearCupClicked());
        completeButton = new JButton("Complete Order");
        completeButton.addActionListener(e -> controller.onCompleteClicked());
        JPanel actions = new JPanel(new GridLayout(1, 2));
        actions.add(clearButton);
        actions.add(completeButton);

        JPanel bottom = new JPanel(new GridLayout(4, 1));
        bottom.add(messageLabel);
        bottom.add(cupLabel);
        bottom.add(ingredients);
        bottom.add(actions);
        frame.add(bottom, BorderLayout.SOUTH);

        frame.setSize(640, 480);
        gameChanged();
    }

    /**
     * Makes the game window visible.
     */
    public void display() {
        frame.setVisible(true);
    }

    @Override
    public void gameChanged() {
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

        String text = info.getMessage();
        if (info.getStatus() == GameState.Status.WON) {
            text = "You win! You survived all " + GameState.TOTAL_DAYS
                + " days.";
        }
        else if (info.getStatus() == GameState.Status.LOST) {
            text = "Game over - you ran out of lives."
                + (text.isEmpty() ? "" : " " + text);
        }
        if (info.getStatus() == GameState.Status.PLAYING
            && info.getPenaltySecondsLeft() > 0) {
            text = "Wrong order! Cleaning up the spill: "
                + info.getPenaltySecondsLeft() + "s";
        }
        messageLabel.setText(text.isEmpty() ? " " : text);

        boolean playing = info.getStatus() == GameState.Status.PLAYING
            && !info.isBetweenDays() && info.getPenaltySecondsLeft() == 0;
        completeButton.setEnabled(playing);
        clearButton.setEnabled(playing);
        for (JButton button : ingredientButtons) {
            button.setEnabled(playing);
        }

        if (info.isBetweenDays() && !dayDialogOpen) {
            SwingUtilities.invokeLater(this::showDayCompleteDialog);
        }
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
