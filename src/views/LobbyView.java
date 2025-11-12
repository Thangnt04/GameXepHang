package views;

import controllers.GameClientController;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class LobbyView extends JPanel {
    private GameClientController controller;
    private JList<String> onlinePlayersList;
    private DefaultListModel<String> playersListModel;
    private JTextArea leaderboardArea;
    private JButton btnChallenge, btnViewHistory;
    private JLabel lblWelcome, lblYourStats;

    public LobbyView(GameClientController controller) {
        this.controller = controller;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel topPanel = new JPanel(new BorderLayout());
        lblWelcome = new JLabel("Chào mừng!", JLabel.CENTER);
        lblWelcome.setFont(new Font("Arial", Font.BOLD, 18));
        topPanel.add(lblWelcome, BorderLayout.NORTH);

        lblYourStats = new JLabel("Thống kê: 0W-0D-0L", JLabel.CENTER);
        topPanel.add(lblYourStats, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(new TitledBorder("Người chơi"));

        playersListModel = new DefaultListModel<>();
        onlinePlayersList = new JList<>(playersListModel);
        onlinePlayersList.setFont(new Font("Monospaced", Font.PLAIN, 12));
        onlinePlayersList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) challengeSelectedPlayer();
            }
        });

        leftPanel.add(new JScrollPane(onlinePlayersList), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        btnChallenge = new JButton("Thách đấu");
        btnChallenge.addActionListener(e -> challengeSelectedPlayer());

        btnViewHistory = new JButton("Xem lịch sử");
        btnViewHistory.addActionListener(e -> viewSelectedPlayerHistory());

        buttonPanel.add(btnChallenge);
        buttonPanel.add(btnViewHistory);
        leftPanel.add(buttonPanel, BorderLayout.SOUTH);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(new TitledBorder("Bảng xếp hạng"));
        leaderboardArea = new JTextArea(15, 30);
        leaderboardArea.setEditable(false);
        leaderboardArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        rightPanel.add(new JScrollPane(leaderboardArea), BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setDividerLocation(400);
        add(splitPane, BorderLayout.CENTER);
    }

    public void setWelcomeMessage(String username) {
        lblWelcome.setText("Chào mừng, " + username + "!");
    }

    public void setYourStats(int wins, int draws, int losses) {
        lblYourStats.setText(String.format("Thống kê: %dW-%dD-%dL | Điểm: %d",
                wins, draws, losses, (wins * 3 + draws)));
    }

    public void updateOnlineList(String[] players) {
        playersListModel.clear();
        for (String player : players) playersListModel.addElement(player);
    }

    public void updateLeaderboard(String leaderboard) {
        String formattedText = leaderboard.replace(";;", "\n");
        leaderboardArea.setText(formattedText);
    }

    private void challengeSelectedPlayer() {
        String selected = onlinePlayersList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn người chơi.");
            return;
        }
        String opponentName = selected.split("\\(")[0];
        controller.sendMessageToServer("CHALLENGE:" + opponentName);
    }

    private void viewSelectedPlayerHistory() {
        String selected = onlinePlayersList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn người chơi.");
            return;
        }
        String targetUsername = selected.split("\\(")[0];
        controller.sendMessageToServer("GET_HISTORY:" + targetUsername);
    }

    public void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    public int showChallengeRequest(String challengerName) {
        return JOptionPane.showConfirmDialog(this,
                "Người chơi '" + challengerName + "' muốn thách đấu bạn. Bạn có đồng ý?",
                "Lời mời thách đấu", JOptionPane.YES_NO_OPTION);
    }
}
