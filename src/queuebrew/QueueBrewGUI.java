package queuebrew;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

/**
 * Thin view: draws what {@link GameInfo} reports and passes the Complete Order
 * click to the controller. Keeps no copy of the game state between redraws.
 */
public class QueueBrewGUI implements GameListener {

    private final GameInfo info;
    private final JFrame frame;
    private final JLabel dayLabel;
    private final JLabel timeLabel;
    private final JLabel livesLabel;
    private final JLabel queueCountLabel;
    private final JLabel messageLabel;
    private final DefaultListModel<String> queueModel;
    private final JButton completeButton;

    /**
     * Builds the window and draws the starting state.
     *
     * @param info
     *            read-only access to the game
     * @param controller
     *            told when the player clicks Complete Order
     */
    public QueueBrewGUI(GameInfo info, QueueBrew controller) {
        this.info = info;
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

        messageLabel = new JLabel(" ", SwingConstants.CENTER);
        completeButton = new JButton("Complete Order");
        completeButton.addActionListener(e -> controller.onCompleteClicked());
        JPanel bottom = new JPanel(new GridLayout(2, 1));
        bottom.add(messageLabel);
        bottom.add(completeButton);
        frame.add(bottom, BorderLayout.SOUTH);

        frame.setSize(360, 420);
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

        String text = info.getMessage();
        if (info.getStatus() == GameState.Status.WON) {
            text = "You win! You survived all " + GameState.TOTAL_DAYS
                + " days.";
        }
        else if (info.getStatus() == GameState.Status.LOST) {
            text = "Game over - you ran out of lives.";
        }
        messageLabel.setText(text.isEmpty() ? " " : text);
        completeButton.setEnabled(info.getStatus() == GameState.Status.PLAYING);
    }
}
