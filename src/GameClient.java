import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GameClient extends JFrame {

    private static final String SERVER_ADDRESS = "127.0.0.1";
    private static final int SERVER_PORT = 12345;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private CardLayout cardLayout;
    private JPanel mainPanel;
    private LoginPanel loginPanel;
    private LobbyPanel lobbyPanel;
    private GamePanel gamePanel;

    private String username;

    public GameClient() {
        this(false);
    }

    public GameClient(boolean previewMode) {
        setTitle("Game Xếp Đơn Hàng Siêu Thị" + (previewMode ? " [PREVIEW MODE]" : ""));
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        loginPanel = new LoginPanel(this);
        lobbyPanel = new LobbyPanel(this);
        gamePanel = new GamePanel(this);

        mainPanel.add(loginPanel, "LOGIN");
        mainPanel.add(lobbyPanel, "LOBBY");
        mainPanel.add(gamePanel, "GAME");

        add(mainPanel);
        cardLayout.show(mainPanel, "LOGIN");

        if (!previewMode) {
            try {
                connectToServer();
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this,
                        "Không thể kết nối tới server: " + e.getMessage(),
                        "Lỗi kết nối",
                        JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        } else {
            loadPreviewData();
        }
    }

    private void connectToServer() throws IOException {
        socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        new Thread(new ServerListener()).start();
    }

    public void sendMessageToServer(String message) {
        if (out != null) {
            System.out.println("Sending to Server: " + message);
            out.println(message);
        }
    }

    public void showPanel(String panelName) {
        cardLayout.show(mainPanel, panelName);
    }

    private void loadPreviewData() {
        username = "PreviewUser";
        lobbyPanel.setWelcomeMessage(username);
        String[] fakePlayers = {"Player1(5W-2D-1L):ONLINE:IDLE", "Player2(3W-1D-2L):ONLINE:BUSY"};
        lobbyPanel.updateOnlineList(fakePlayers);
        lobbyPanel.updateLeaderboard("LEADERBOARD:1. Player1 (17 pts | 5W-2D-1L)");
        addPreviewButtons();
    }

    private void addPreviewButtons() {
        JPanel previewPanel = new JPanel(new FlowLayout());
        previewPanel.setBackground(Color.YELLOW);
        JButton btnShowLogin = new JButton("Login");
        JButton btnShowLobby = new JButton("Lobby");
        JButton btnShowGame = new JButton("Game");
        btnShowLogin.addActionListener(e -> showPanel("LOGIN"));
        btnShowLobby.addActionListener(e -> showPanel("LOBBY"));
        btnShowGame.addActionListener(e -> {
            gamePanel.startNewRound("Player2", "Táo,Cam,Sữa,Trứng,Thịt Gà",
                    "Táo,Chuối,Cam,Nho,Sữa,Bánh Mì,Trứng,Phô Mai,Thịt Gà,Cá", 60);
            showPanel("GAME");
        });
        previewPanel.add(new JLabel("PREVIEW: "));
        previewPanel.add(btnShowLogin);
        previewPanel.add(btnShowLobby);
        previewPanel.add(btnShowGame);
        add(previewPanel, BorderLayout.SOUTH);
    }

    class LoginPanel extends JPanel implements ActionListener {
        private GameClient client;
        private JTextField tfUsername;
        private JPasswordField pfPassword;
        private JButton btnLogin, btnRegister;

        public LoginPanel(GameClient client) {
            this.client = client;
            setLayout(new GridBagLayout());
            setBorder(new EmptyBorder(20, 20, 20, 20));

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            gbc.gridx = 0; gbc.gridy = 0;
            add(new JLabel("Tên đăng nhập:"), gbc);
            gbc.gridx = 1;
            tfUsername = new JTextField(20);
            add(tfUsername, gbc);

            gbc.gridx = 0; gbc.gridy = 1;
            add(new JLabel("Mật khẩu:"), gbc);
            gbc.gridx = 1;
            pfPassword = new JPasswordField(20);
            add(pfPassword, gbc);

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            btnLogin = new JButton("Đăng nhập");
            btnRegister = new JButton("Đăng ký");
            buttonPanel.add(btnLogin);
            buttonPanel.add(btnRegister);

            gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
            add(buttonPanel, gbc);

            btnLogin.addActionListener(this);
            btnRegister.addActionListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String user = tfUsername.getText().trim();
            String pass = new String(pfPassword.getPassword());

            if (user.isEmpty() || pass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập đủ tên và mật khẩu.");
                return;
            }

            if (e.getSource() == btnLogin) {
                client.sendMessageToServer("LOGIN:" + user + ":" + pass);
            } else if (e.getSource() == btnRegister) {
                client.sendMessageToServer("REGISTER:" + user + ":" + pass);
            }
        }
    }

    class LobbyPanel extends JPanel {
        private GameClient client;
        private JList<String> onlinePlayersList;
        private DefaultListModel<String> playersListModel;
        private JTextArea leaderboardArea;
        private JButton btnChallenge;
        private JLabel lblWelcome, lblYourStats;

        public LobbyPanel(GameClient client) {
            this.client = client;
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
            leftPanel.setBorder(new TitledBorder("Người chơi Online"));

            playersListModel = new DefaultListModel<>();
            onlinePlayersList = new JList<>(playersListModel);
            onlinePlayersList.setFont(new Font("Monospaced", Font.PLAIN, 12));
            onlinePlayersList.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent evt) {
                    if (evt.getClickCount() == 2) challengeSelectedPlayer();
                }
            });

            leftPanel.add(new JScrollPane(onlinePlayersList), BorderLayout.CENTER);
            btnChallenge = new JButton("Thách đấu");
            btnChallenge.addActionListener(e -> challengeSelectedPlayer());
            leftPanel.add(btnChallenge, BorderLayout.SOUTH);

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
            leaderboardArea.setText(leaderboard.replace("LEADERBOARD:", ""));
        }

        private void challengeSelectedPlayer() {
            String selected = onlinePlayersList.getSelectedValue();
            if (selected == null) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn người chơi.");
                return;
            }
            String opponentName = selected.split("\\(")[0];
            client.sendMessageToServer("CHALLENGE:" + opponentName);
        }
    }

    class GamePanel extends JPanel implements ActionListener {
        private GameClient client;
        private JLabel lblOpponent, lblTimer, lblOrderStatus, lblYourStats, lblOpponentStats;
        private JPanel pnlOrder, pnlShelf, pnlPackingTray;
        private JButton btnSubmit, btnExit, btnResetTray;
        private javax.swing.Timer gameTimer;
        private int timeLeft;
        private List<String> currentOrder;
        private List<String> currentPackingTray;
        private boolean hasAskedToPlayAgain = false;

        public GamePanel(GameClient client) {
            this.client = client;
            this.currentOrder = new ArrayList<>();
            this.currentPackingTray = new ArrayList<>();

            setLayout(new BorderLayout(10, 10));
            setBorder(new EmptyBorder(10, 10, 10, 10));

            JPanel topPanel = new JPanel(new BorderLayout());
            JPanel infoPanel = new JPanel(new GridLayout(2, 2));
            lblOpponent = new JLabel("Đang chơi với: ");
            lblYourStats = new JLabel("Bạn: Chưa hoàn thành");
            lblOpponentStats = new JLabel("Đối thủ: Chưa hoàn thành");
            lblOrderStatus = new JLabel("Đã xếp: 0/0", JLabel.RIGHT);
            infoPanel.add(lblOpponent);
            infoPanel.add(lblOrderStatus);
            infoPanel.add(lblYourStats);
            infoPanel.add(lblOpponentStats);
            topPanel.add(infoPanel, BorderLayout.NORTH);

            lblTimer = new JLabel("Thời gian: 60s", JLabel.CENTER);
            lblTimer.setFont(new Font("Arial", Font.BOLD, 24));
            lblTimer.setForeground(new Color(0, 150, 0));
            topPanel.add(lblTimer, BorderLayout.CENTER);
            add(topPanel, BorderLayout.NORTH);

            JPanel centerPanel = new JPanel(new GridLayout(2, 1, 10, 10));
            pnlShelf = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
            pnlShelf.setBorder(new TitledBorder("Kệ Hàng"));
            centerPanel.add(new JScrollPane(pnlShelf));

            pnlPackingTray = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
            pnlPackingTray.setBorder(new TitledBorder("Khay Xếp Hàng"));
            centerPanel.add(new JScrollPane(pnlPackingTray));
            add(centerPanel, BorderLayout.CENTER);

            pnlOrder = new JPanel();
            pnlOrder.setLayout(new BoxLayout(pnlOrder, BoxLayout.Y_AXIS));
            pnlOrder.setBorder(new TitledBorder("Đơn Hàng"));
            add(new JScrollPane(pnlOrder), BorderLayout.WEST);

            JPanel bottomPanel = new JPanel(new FlowLayout());
            btnSubmit = new JButton("Gửi Đơn Hàng");
            btnResetTray = new JButton("Xếp Lại");
            btnExit = new JButton("Thoát");
            bottomPanel.add(btnSubmit);
            bottomPanel.add(btnResetTray);
            bottomPanel.add(btnExit);
            add(bottomPanel, BorderLayout.SOUTH);

            btnSubmit.addActionListener(this);
            btnResetTray.addActionListener(this);
            btnExit.addActionListener(this);

            gameTimer = new javax.swing.Timer(1000, e -> {
                timeLeft--;
                lblTimer.setText("Thời gian: " + timeLeft + "s");
                if (timeLeft <= 10) lblTimer.setForeground(Color.RED);
                if (timeLeft <= 0) {
                    gameTimer.stop();
                    client.sendMessageToServer("SUBMIT_ORDER:TIMEOUT");
                    btnSubmit.setEnabled(false);
                }
            });
        }

        public void startNewRound(String opponent, String orderCsv, String shelfCsv, int time) {
            lblOpponent.setText("Đang chơi với: " + opponent);
            lblYourStats.setText("Bạn: Đang xếp...");
            lblOpponentStats.setText("Đối thủ: Đang xếp...");
            timeLeft = time;
            lblTimer.setText("Thời gian: " + timeLeft + "s");
            lblTimer.setForeground(new Color(0, 150, 0));

            currentOrder = Arrays.asList(orderCsv.split(","));
            List<String> shelfItems = Arrays.asList(shelfCsv.split(","));
            currentPackingTray.clear();

            pnlOrder.removeAll();
            for (int i = 0; i < currentOrder.size(); i++) {
                pnlOrder.add(new JLabel((i + 1) + ". " + currentOrder.get(i)));
            }

            pnlShelf.removeAll();
            for (String item : shelfItems) {
                JButton btn = new JButton(item);
                btn.addActionListener(e -> {
                    currentPackingTray.add(item);
                    updatePackingTrayUI();
                });
                pnlShelf.add(btn);
            }

            updatePackingTrayUI();
            btnSubmit.setEnabled(true);
            hasAskedToPlayAgain = false;
            gameTimer.start();
            revalidate();
            repaint();
        }

        private void updatePackingTrayUI() {
            pnlPackingTray.removeAll();
            for (int i = 0; i < currentPackingTray.size(); i++) {
                pnlPackingTray.add(new JLabel((i + 1) + ". " + currentPackingTray.get(i)));
            }
            lblOrderStatus.setText(String.format("Đã xếp: %d/%d", currentPackingTray.size(), currentOrder.size()));
            pnlPackingTray.revalidate();
            pnlPackingTray.repaint();
        }

        public void updateOpponentStatus(String status) {
            lblOpponentStats.setText("Đối thủ: " + status);
        }

        public void stopTimer() {
            if (gameTimer.isRunning()) gameTimer.stop();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == btnSubmit) {
                if (currentPackingTray.size() != currentOrder.size()) {
                    JOptionPane.showMessageDialog(this, "Bạn phải xếp đủ " + currentOrder.size() + " món!");
                    return;
                }
                gameTimer.stop();
                btnSubmit.setEnabled(false);
                client.sendMessageToServer("SUBMIT_ORDER:" + String.join(",", currentPackingTray));
            } else if (e.getSource() == btnResetTray) {
                currentPackingTray.clear();
                updatePackingTrayUI();
            } else if (e.getSource() == btnExit) {
                gameTimer.stop();
                int choice = JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn thoát?", "Xác nhận", JOptionPane.YES_NO_OPTION);
                if (choice == JOptionPane.YES_OPTION) {
                    client.sendMessageToServer("EXIT_GAME");
                    client.showPanel("LOBBY");
                } else if (timeLeft > 0) {
                    gameTimer.start();
                }
            }
        }
    }

    class ServerListener implements Runnable {
        @Override
        public void run() {
            String serverMessage;
            try {
                while ((serverMessage = in.readLine()) != null) {
                    System.out.println("Received from Server: " + serverMessage);
                    String finalMsg = serverMessage;
                    SwingUtilities.invokeLater(() -> processServerMessage(finalMsg));
                }
            } catch (IOException e) {
                if (!socket.isClosed()) e.printStackTrace();
            } finally {
                System.out.println("Listener disconnected.");
            }
        }

        private void processServerMessage(String message) {
            String[] parts = message.split(":", 2);
            String command = parts[0];
            String data = (parts.length > 1) ? parts[1] : "";

            switch (command) {
                case "LOGIN_SUCCESS":
                    String[] statsData = data.split(":");
                    username = loginPanel.tfUsername.getText();
                    lobbyPanel.setWelcomeMessage(username);
                    if (statsData.length >= 3) {
                        lobbyPanel.setYourStats(Integer.parseInt(statsData[0]),
                                Integer.parseInt(statsData[1]), Integer.parseInt(statsData[2]));
                    }
                    showPanel("LOBBY");
                    break;
                case "LOGIN_FAIL":
                case "REGISTER_FAIL":
                    JOptionPane.showMessageDialog(loginPanel, data, "Lỗi", JOptionPane.ERROR_MESSAGE);
                    break;
                case "REGISTER_SUCCESS":
                    JOptionPane.showMessageDialog(loginPanel, data, "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    break;
                case "ONLINE_LIST":
                    lobbyPanel.updateOnlineList(data.split(","));
                    break;
                case "LEADERBOARD":
                    lobbyPanel.updateLeaderboard(data);
                    break;
                case "SERVER_MSG":
                    JOptionPane.showMessageDialog(lobbyPanel, data);
                    break;
                case "CHALLENGE_REQUEST":
                    int choice = JOptionPane.showConfirmDialog(lobbyPanel,
                            "Người chơi '" + data + "' muốn thách đấu bạn. Bạn có đồng ý?",
                            "Lời mời thách đấu", JOptionPane.YES_NO_OPTION);
                    String response = (choice == JOptionPane.YES_OPTION) ? "accept" : "reject";
                    sendMessageToServer("CHALLENGE_RESPONSE:" + data + ":" + response);
                    break;
                case "CHALLENGE_REJECTED":
                    JOptionPane.showMessageDialog(lobbyPanel, "Người chơi '" + data + "' đã từ chối.");
                    break;
                case "GAME_START":
                    String[] gameData = data.split(":");
                    gamePanel.startNewRound(gameData[0], gameData[1], gameData[2], Integer.parseInt(gameData[3]));
                    showPanel("GAME");
                    break;
                case "ROUND_RESULT":
                    String[] resultData = data.split(":");
                    if (!gamePanel.hasAskedToPlayAgain) {
                        gamePanel.hasAskedToPlayAgain = true;
                        int playChoice = JOptionPane.showConfirmDialog(gamePanel,
                                resultData[1] + "\n\nBạn có muốn chơi tiếp?", "Kết quả - " + resultData[0], JOptionPane.YES_NO_OPTION);
                        if (playChoice == JOptionPane.YES_OPTION) {
                            sendMessageToServer("PLAY_AGAIN_REQUEST");
                        } else {
                            sendMessageToServer("EXIT_GAME");
                            showPanel("LOBBY");
                        }
                    }
                    break;
                case "OPPONENT_SUBMITTED":
                    gamePanel.updateOpponentStatus("Đã nộp bài!");
                    break;
                case "PLAY_AGAIN_REQUEST":
                    if (!gamePanel.hasAskedToPlayAgain) {
                        gamePanel.hasAskedToPlayAgain = true;
                        int playChoice2 = JOptionPane.showConfirmDialog(gamePanel,
                                "Đối thủ muốn chơi tiếp. Bạn có đồng ý?", "Yêu cầu chơi tiếp", JOptionPane.YES_NO_OPTION);
                        if (playChoice2 == JOptionPane.YES_OPTION) {
                            sendMessageToServer("PLAY_AGAIN_REQUEST");
                        } else {
                            sendMessageToServer("EXIT_GAME");
                            showPanel("LOBBY");
                        }
                    }
                    break;
                case "OPPONENT_EXITED":
                    gamePanel.stopTimer();
                    gamePanel.hasAskedToPlayAgain = true;
                    JOptionPane.showMessageDialog(gamePanel, data, "Trận đấu kết thúc", JOptionPane.INFORMATION_MESSAGE);
                    showPanel("LOBBY");
                    break;
            }
        }
    }

    public static void main(String[] args) {
        boolean previewMode = args.length > 0 && args[0].equals("--preview");
        SwingUtilities.invokeLater(() -> {
            GameClient client = new GameClient(previewMode);
            client.setVisible(true);
            client.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                    try {
                        if (client.socket != null && !client.socket.isClosed()) {
                            client.socket.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        });
    }
}