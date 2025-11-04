import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * File để preview giao diện mà không cần chạy server
 */
public class UIPreview {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            String[] options = {"Login", "Lobby (Improved)", "Game (Improved)"};
            int choice = JOptionPane.showOptionDialog(null,
                    "Chọn màn hình muốn xem:",
                    "UI Preview - Game Xếp Hàng",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]);

            switch (choice) {
                case 0: showLoginPreview(); break;
                case 1: showLobbyPreview(); break;
                case 2: showGamePreview(); break;
            }
        });
    }

    private static void showLoginPreview() {
        JFrame frame = new JFrame("Preview: Login Screen");
        frame.setSize(500, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Title
        JLabel title = new JLabel("🏪 Game Xếp Hàng Siêu Thị", JLabel.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 24));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(title, gbc);

        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Tên đăng nhập:"), gbc);

        gbc.gridx = 1;
        JTextField tfUsername = new JTextField(20);
        panel.add(tfUsername, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Mật khẩu:"), gbc);

        gbc.gridx = 1;
        JPasswordField pfPassword = new JPasswordField(20);
        panel.add(pfPassword, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton btnLogin = new JButton("🔐 Đăng nhập");
        JButton btnRegister = new JButton("📝 Đăng ký");
        btnLogin.setPreferredSize(new Dimension(150, 35));
        btnRegister.setPreferredSize(new Dimension(150, 35));
        buttonPanel.add(btnLogin);
        buttonPanel.add(btnRegister);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        panel.add(buttonPanel, gbc);

        frame.add(panel);
        frame.setVisible(true);
    }

    private static void showLobbyPreview() {
        JFrame frame = new JFrame("Preview: Lobby Screen (Improved)");
        frame.setSize(1000, 700);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Top panel
        JPanel topPanel = new JPanel(new BorderLayout());
        JLabel lblWelcome = new JLabel("Chào mừng, Player1! 👋", JLabel.CENTER);
        lblWelcome.setFont(new Font("Arial", Font.BOLD, 20));
        topPanel.add(lblWelcome, BorderLayout.NORTH);

        JLabel lblStats = new JLabel("📊 Thống kê của bạn: 5W-2D-1L | Điểm: 17", JLabel.CENTER);
        lblStats.setFont(new Font("Arial", Font.PLAIN, 14));
        topPanel.add(lblStats, BorderLayout.CENTER);

        JPanel buttonsPanel = new JPanel();
        JButton btnRefresh = new JButton("🔄 Làm mới");
        JButton btnLogout = new JButton("🚪 Đăng xuất");
        buttonsPanel.add(btnRefresh);
        buttonsPanel.add(btnLogout);
        topPanel.add(buttonsPanel, BorderLayout.SOUTH);

        mainPanel.add(topPanel, BorderLayout.NORTH);

        // Left: Online players
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(new TitledBorder("🌐 Người chơi đang Online"));

        DefaultListModel<String> playersModel = new DefaultListModel<>();
        playersModel.addElement("Player1(5W-2D-1L):ONLINE:IDLE");
        playersModel.addElement("Player2(8W-1D-3L):ONLINE:BUSY");
        playersModel.addElement("Player3(3W-4D-2L):ONLINE:IDLE");
        playersModel.addElement("Player4(10W-0D-1L):ONLINE:IDLE");

        JList<String> playersList = new JList<>(playersModel);
        playersList.setFont(new Font("Monospaced", Font.PLAIN, 13));
        leftPanel.add(new JScrollPane(playersList), BorderLayout.CENTER);

        JButton btnChallenge = new JButton("⚔️ Thách đấu người chơi đã chọn");
        btnChallenge.setFont(new Font("Arial", Font.BOLD, 13));
        leftPanel.add(btnChallenge, BorderLayout.SOUTH);

        // Right: Leaderboard
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(new TitledBorder("🏆 Bảng xếp hạng"));

        JTextArea leaderboard = new JTextArea();
        leaderboard.setFont(new Font("Monospaced", Font.PLAIN, 13));
        leaderboard.setEditable(false);
        leaderboard.setText(
            "1. Player4 (30 pts | 10W-0D-1L)\n" +
            "2. Player2 (25 pts | 8W-1D-3L)\n" +
            "3. Player1 (17 pts | 5W-2D-1L)\n" +
            "4. Player3 (13 pts | 3W-4D-2L)\n"
        );
        rightPanel.add(new JScrollPane(leaderboard), BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setDividerLocation(450);
        mainPanel.add(splitPane, BorderLayout.CENTER);

        frame.add(mainPanel);
        frame.setVisible(true);
    }

    private static void showGamePreview() {
        JFrame frame = new JFrame("Preview: Game Screen (Improved)");
        frame.setSize(1200, 800);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Top panel
        JPanel topPanel = new JPanel(new BorderLayout());
        
        JPanel infoPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        infoPanel.add(new JLabel("🎮 Đang chơi với: Player2"));
        infoPanel.add(new JLabel("📦 Đã xếp: 3/5", JLabel.RIGHT));
        infoPanel.add(new JLabel("📊 Bạn: Đang xếp (3/5)"));
        infoPanel.add(new JLabel("📊 Đối thủ: Đã nộp bài! ✅", JLabel.RIGHT));
        topPanel.add(infoPanel, BorderLayout.NORTH);

        JLabel lblTimer = new JLabel("⏱️ Thời gian: 45s", JLabel.CENTER);
        lblTimer.setFont(new Font("Arial", Font.BOLD, 28));
        lblTimer.setForeground(new Color(255, 165, 0));
        topPanel.add(lblTimer, BorderLayout.CENTER);

        mainPanel.add(topPanel, BorderLayout.NORTH);

        // Center
        JPanel centerPanel = new JPanel(new GridLayout(2, 1, 10, 10));

        JPanel shelfPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        shelfPanel.setBorder(new TitledBorder("🏪 Kệ Hàng"));
        shelfPanel.setBackground(new Color(255, 250, 240));
        String[] items = {"Táo", "Chuối", "Cam", "Nho", "Sữa", "Bánh Mì", "Trứng", "Phô Mai"};
        for (String item : items) {
            JButton btn = new JButton(item);
            btn.setPreferredSize(new Dimension(100, 40));
            btn.setBackground(new Color(230, 230, 250));
            shelfPanel.add(btn);
        }
        centerPanel.add(shelfPanel);

        JPanel trayPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        trayPanel.setBorder(new TitledBorder("📦 Khay Xếp Hàng"));
        trayPanel.setBackground(new Color(240, 255, 240));
        trayPanel.add(new JLabel("1. Táo"));
        trayPanel.add(new JLabel("2. Cam"));
        trayPanel.add(new JLabel("3. Sữa"));
        centerPanel.add(trayPanel);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Left: Order
        JPanel orderPanel = new JPanel();
        orderPanel.setLayout(new BoxLayout(orderPanel, BoxLayout.Y_AXIS));
        orderPanel.setBorder(new TitledBorder("📋 Đơn Hàng"));
        orderPanel.setBackground(new Color(255, 255, 240));
        for (int i = 1; i <= 5; i++) {
            JLabel lbl = new JLabel(i + ". Item " + i);
            lbl.setFont(new Font("Arial", Font.BOLD, 14));
            orderPanel.add(lbl);
        }
        mainPanel.add(new JScrollPane(orderPanel), BorderLayout.WEST);

        // Bottom: Buttons
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        JButton btnSubmit = new JButton("✅ Gửi Đơn Hàng");
        btnSubmit.setBackground(new Color(0, 200, 0));
        btnSubmit.setForeground(Color.WHITE);
        btnSubmit.setFont(new Font("Arial", Font.BOLD, 14));
        btnSubmit.setPreferredSize(new Dimension(160, 40));

        JButton btnReset = new JButton("🔄 Xếp Lại");
        btnReset.setBackground(new Color(255, 165, 0));
        btnReset.setForeground(Color.WHITE);
        btnReset.setPreferredSize(new Dimension(120, 40));

        JButton btnExit = new JButton("🚪 Thoát");
        btnExit.setBackground(new Color(200, 0, 0));
        btnExit.setForeground(Color.WHITE);
        btnExit.setPreferredSize(new Dimension(120, 40));

        bottomPanel.add(btnSubmit);
        bottomPanel.add(btnReset);
        bottomPanel.add(btnExit);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        frame.add(mainPanel);
        frame.setVisible(true);
    }
}
