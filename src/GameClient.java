import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
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
    private HistoryPanel historyPanel;

    private String username;

    public GameClient() {
        setTitle("Game Xếp Đơn Hàng Siêu Thị");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        loginPanel = new LoginPanel(this);
        lobbyPanel = new LobbyPanel(this);
        gamePanel = new GamePanel(this);
        historyPanel = new HistoryPanel(this);

        mainPanel.add(loginPanel, "LOGIN");
        mainPanel.add(lobbyPanel, "LOBBY");
        mainPanel.add(gamePanel, "GAME");
        mainPanel.add(historyPanel, "HISTORY");

        add(mainPanel);
        cardLayout.show(mainPanel, "LOGIN");

        try {
            connectToServer();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Không thể kết nối tới server: " + e.getMessage(),
                    "Lỗi kết nối",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
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

    // --- Lớp LobbyPanel ---
    class LobbyPanel extends JPanel {
        private GameClient client;
        private JList<String> onlinePlayersList;
        private DefaultListModel<String> playersListModel;
        private JTextArea leaderboardArea;
        private JButton btnChallenge,btnViewHistory;
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
            client.sendMessageToServer("CHALLENGE:" + opponentName);
        }
        private void viewSelectedPlayerHistory() {
            String selected = onlinePlayersList.getSelectedValue();
            if (selected == null) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn người chơi.");
                return;
            }
            String targetUsername = selected.split("\\(")[0];
            client.sendMessageToServer("GET_HISTORY:" + targetUsername);
        }
    }



    // --- Lớp GamePanel ---
    class GamePanel extends JPanel implements ActionListener {
        private GameClient client;
        private JLabel lblOpponent, lblTimer, lblMatchProgress, lblYourProgress, lblOpponentProgress;
        private JPanel pnlOrder, pnlShelf, pnlPackingTray;
        private JButton btnSubmit, btnExit, btnResetTray;

        private javax.swing.Timer gameTimer;
        private int timeLeft;
        private List<String> currentOrder;
        private List<String> currentPackingTray;
        private boolean hasAskedToPlayAgain = false;

        private int myProgress = 0;
        private int opponentProgress = 0;

        public GamePanel(GameClient client) {
            this.client = client;
            this.currentOrder = new ArrayList<>();
            this.currentPackingTray = new ArrayList<>();

            setLayout(new BorderLayout(10, 10));
            setBorder(new EmptyBorder(10, 10, 10, 10));

            JPanel topPanel = new JPanel(new BorderLayout());
            JPanel infoPanel = new JPanel(new GridLayout(2, 2));
            lblOpponent = new JLabel("Đang chơi với: ");
            lblYourProgress = new JLabel("Bạn: Hoàn thành 0/5 đơn");
            lblOpponentProgress = new JLabel("Đối thủ: Hoàn thành 0/5 đơn");
            lblMatchProgress = new JLabel("Đơn hàng 1/5", JLabel.RIGHT);

            infoPanel.add(lblOpponent);
            infoPanel.add(lblMatchProgress); // Sửa thứ tự
            infoPanel.add(lblYourProgress);
            infoPanel.add(lblOpponentProgress);
            topPanel.add(infoPanel, BorderLayout.NORTH);

            lblTimer = new JLabel("Thời gian: 1:00", JLabel.CENTER); // Sửa text mặc định
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
            btnExit = new JButton("Bỏ cuộc");
            bottomPanel.add(btnSubmit);
            bottomPanel.add(btnResetTray);
            bottomPanel.add(btnExit);
            add(bottomPanel, BorderLayout.SOUTH);

            btnSubmit.addActionListener(this);
            btnResetTray.addActionListener(this);
            btnExit.addActionListener(this);

            gameTimer = new javax.swing.Timer(1000, e -> {
                timeLeft--;

                if (timeLeft >= 0) {
                    lblTimer.setText("Thời gian: " + (timeLeft / 60) + ":" + String.format("%02d", (timeLeft % 60)));
                }
                if (timeLeft <= 30) lblTimer.setForeground(Color.RED); // Báo đỏ khi còn 30s
                if (timeLeft <= 0) {
                    gameTimer.stop();
                    // Không cần gửi TIMEOUT nữa, Server sẽ tự ngắt
                    // Vô hiệu hóa nút khi hết giờ
                    btnSubmit.setEnabled(false);
                    btnExit.setEnabled(false);
                    btnResetTray.setEnabled(false);
                    lblTimer.setText("HẾT GIỜ!");
                }
            });
        }

        public void startMatch(String opponent, int time) {
            lblOpponent.setText("Đang chơi với: " + opponent);
            timeLeft = time;
            lblTimer.setText("Thời gian: " + (timeLeft / 60) + ":" + String.format("%02d", (timeLeft % 60)));
            lblTimer.setForeground(new Color(0, 150, 0));

            // Reset tiến độ
            updateMyProgress(0);
            updateOpponentProgress(0);
            hasAskedToPlayAgain = false;

            // Kích hoạt các nút
            btnSubmit.setEnabled(true);
            btnExit.setEnabled(true);
            btnResetTray.setEnabled(true);

            // Xóa rác
            pnlOrder.removeAll();
            pnlShelf.removeAll();
            pnlPackingTray.removeAll();
            pnlOrder.revalidate();
            pnlShelf.revalidate();
            pnlPackingTray.revalidate();

            gameTimer.start();
        }

        // HÀM MỚI: Hiển thị đơn hàng
        public void displayNewOrder(int orderIndex, String orderCsv, String shelfCsv) {
            lblMatchProgress.setText(String.format("Đơn hàng %d/5", orderIndex + 1));

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
                    if (btnSubmit.isEnabled()) { // Chỉ cho phép thêm nếu game còn
                        currentPackingTray.add(item);
                        updatePackingTrayUI();
                    }
                });
                pnlShelf.add(btn);
            }

            updatePackingTrayUI();
            btnSubmit.setEnabled(true); // Kích hoạt lại nút nộp
            btnResetTray.setEnabled(true); // Kích hoạt lại nút reset

            revalidate();
            repaint();
        }

        // Cập nhật tiến độ của BẠN
        public void updateMyProgress(int progress) {
            this.myProgress = progress;
            lblYourProgress.setText(String.format("Bạn: Hoàn thành %d/5 đơn", progress));
            // Cập nhật lại text đơn hàng
            if (progress < 5) {
                lblMatchProgress.setText(String.format("Đơn hàng %d/5", (myProgress + 1)));
            } else {
                lblMatchProgress.setText("Đã xong 5/5!");
            }
        }

        //Cập nhật tiến độ của ĐỐI THỦ
        public void updateOpponentProgress(int progress) {
            this.opponentProgress = progress;
            lblOpponentProgress.setText(String.format("Đối thủ: Hoàn thành %d/5 đơn", progress));
        }

        private void updatePackingTrayUI() {
            pnlPackingTray.removeAll();
            for (int i = 0; i < currentPackingTray.size(); i++) {
                pnlPackingTray.add(new JLabel((i + 1) + ". " + currentPackingTray.get(i)));
            }
            // Hiển thị cả tiến độ và số lượng đã xếp
            if (myProgress < 5) {
                lblMatchProgress.setText(String.format("Đơn hàng %d/5 (Đã xếp: %d/%d)", (myProgress + 1), currentPackingTray.size(), currentOrder.size()));
            }
            pnlPackingTray.revalidate();
            pnlPackingTray.repaint();
        }

        public void stopTimer() {
            if (gameTimer.isRunning()) gameTimer.stop();
        }

        public void reEnableSubmission() {
            // Cho phép người chơi nộp lại và xếp lại
            btnSubmit.setEnabled(true);
            btnResetTray.setEnabled(true);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == btnSubmit) {
                if (currentPackingTray.size() != currentOrder.size()) {
                    JOptionPane.showMessageDialog(this, "Bạn phải xếp đủ " + currentOrder.size() + " món!");
                    return;
                }
                btnSubmit.setEnabled(false);
                btnResetTray.setEnabled(false);
                client.sendMessageToServer("SUBMIT_ORDER:" + String.join(",", currentPackingTray));

            } else if (e.getSource() == btnResetTray) {
                currentPackingTray.clear();
                updatePackingTrayUI();

            } else if (e.getSource() == btnExit) {
                // Dừng timer
                gameTimer.stop();
                // Vô hiệu hóa các nút
                btnSubmit.setEnabled(false);
                btnExit.setEnabled(false);
                btnResetTray.setEnabled(false);
                // Gửi lệnh Bỏ cuộc (FORFEIT) mới.
                client.sendMessageToServer("FORFEIT");
            }
        }
    }

    class HistoryPanel extends JPanel implements ActionListener {
        private GameClient client;
        private JLabel lblTitle;
        private JTable historyTable; // THAY ĐỔI: Từ JTextArea sang JTable
        private DefaultTableModel tableModel; // THAY ĐỔI: Model cho JTable
        private JButton btnBack;
        private SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

        public HistoryPanel(GameClient client) {
            this.client = client;
            setLayout(new BorderLayout(10, 10));
            setBorder(new EmptyBorder(10, 10, 10, 10));

            lblTitle = new JLabel("Lịch sử đấu của...", JLabel.CENTER);
            lblTitle.setFont(new Font("Arial", Font.BOLD, 18));
            add(lblTitle, BorderLayout.NORTH);

            // --- THAY ĐỔI LOGIC TẠO BẢNG ---
            // 1. Tạo Model với các cột
            String[] columnNames = {"Đối thủ", "Kết quả", "Tỷ số (Bạn - Đ/thủ)", "Thời gian"};
            tableModel = new DefaultTableModel(columnNames, 0) {
                // Ghi đè phương thức để ngăn người dùng chỉnh sửa ô
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            // 2. Tạo JTable từ Model
            historyTable = new JTable(tableModel);
            historyTable.setFont(new Font("Arial", Font.PLAIN, 12));
            historyTable.setFillsViewportHeight(true); // Cho phép bảng lấp đầy chiều cao
            historyTable.setRowHeight(20); // Tăng chiều cao hàng một chút

            // 3. Tùy chỉnh độ rộng cột (tùy chọn nhưng nên làm)
            historyTable.getColumnModel().getColumn(0).setPreferredWidth(150); // Đối thủ
            historyTable.getColumnModel().getColumn(1).setPreferredWidth(70);  // Kết quả
            historyTable.getColumnModel().getColumn(2).setPreferredWidth(130); // Tỷ số
            historyTable.getColumnModel().getColumn(3).setPreferredWidth(120); // Thời gian

            // 4. Thêm JTable vào JScrollPane (thay vì JTextArea)
            add(new JScrollPane(historyTable), BorderLayout.CENTER);
            // --- KẾT THÚC THAY ĐỔI LOGIC TẠO BẢNG ---

            btnBack = new JButton("Quay lại Sảnh");
            btnBack.addActionListener(this);
            add(btnBack, BorderLayout.SOUTH);
        }

        public void displayHistory(String targetUsername, String data) {
            lblTitle.setText("Lịch sử đấu của: " + targetUsername);

            // 1. Xóa tất cả hàng cũ khỏi bảng
            tableModel.setRowCount(0);

            if (data == null || data.isEmpty()) {
                // Nếu không có dữ liệu, bảng sẽ trống (tốt hơn là hiển thị text)
                return;
            }

            // --- THAY ĐỔI LOGIC HIỂN THỊ DỮ LIỆU ---
            // Format: opponent1,WIN,5,2,date1;;opponent2,LOSS,1,5,date2
            String[] matches = data.split(";;");

            for (String match : matches) {
                String[] parts = match.split(",");
                if (parts.length < 5) continue;

                String opponent = parts[0];
                String result = parts[1];
                String myScore = parts[2];
                String opponentScore = parts[3];
                Date date = new Date(Long.parseLong(parts[4]));

                // Tạo một mảng Object cho hàng mới
                Object[] rowData = {
                        opponent,
                        result,
                        myScore + " - " + opponentScore, // Hiển thị tỷ số
                        sdf.format(date)
                };

                // Thêm hàng mới vào model, JTable sẽ tự động cập nhật
                tableModel.addRow(rowData);
            }
            // --- KẾT THÚC THAY ĐỔI LOGIC HIỂN THỊ ---
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == btnBack) {
                client.showPanel("LOBBY");
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
                    String[] gameData = data.split(":"); // OpponentName:MatchTime
                    gamePanel.startMatch(gameData[0], Integer.parseInt(gameData[1]));
                    showPanel("GAME");
                    break;

                case "NEW_ORDER":
                    // Format: OrderIndex:OrderCsv:ShelfCsv
                    String[] orderData = data.split(":", 3);
                    gamePanel.displayNewOrder(Integer.parseInt(orderData[0]), orderData[1], orderData[2]);
                    break;
                case "UPDATE_PROGRESS":
                    // Format: newProgress
                    gamePanel.updateMyProgress(Integer.parseInt(data));
                    break;
                case "OPPONENT_PROGRESS":
                    // Format: newProgress
                    gamePanel.updateOpponentProgress(Integer.parseInt(data));
                    break;
                case "SUBMIT_FAIL":
                    JOptionPane.showMessageDialog(gamePanel, data, "Sai rồi!", JOptionPane.WARNING_MESSAGE);
                    // Cho phép nộp lại bằng cách gọi hàm công khai của gamePanel
                    gamePanel.reEnableSubmission();
                    break;
                case "ROUND_RESULT":
                    String[] resultData = data.split(":", 2); // Tách 2 phần: Result:Message
                    if (!gamePanel.hasAskedToPlayAgain) {
                        gamePanel.hasAskedToPlayAgain = true;
                        gamePanel.stopTimer(); // Dừng timer ngay khi có kết quả

                        int playChoice = JOptionPane.showConfirmDialog(gamePanel,
                                resultData[1] + "\n\nBạn có muốn chơi tiếp?", "Kết quả - " + resultData[0], JOptionPane.YES_NO_OPTION);
                        if (playChoice == JOptionPane.YES_OPTION) {
                            sendMessageToServer("PLAY_AGAIN_REQUEST");
                        } else {
                            sendMessageToServer("EXIT_GAME"); // Gửi EXIT_GAME
                            showPanel("LOBBY");
                        }
                    }
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
                    gamePanel.hasAskedToPlayAgain = true; // Ngăn hỏi chơi lại
                    JOptionPane.showMessageDialog(gamePanel, data, "Trận đấu kết thúc", JOptionPane.INFORMATION_MESSAGE);
                    showPanel("LOBBY");
                    break;
                case "HISTORY_DATA":
                    // Format: targetUsername:opponent1,WIN,5,2,date1;;...
                    String[] historyParts = data.split(":", 2);
                    String targetUsername = historyParts[0];
                    String historyData = (historyParts.length > 1) ? historyParts[1] : "";

                    historyPanel.displayHistory(targetUsername, historyData);
                    showPanel("HISTORY");
                    break;
            }
        }
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameClient client = new GameClient();
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