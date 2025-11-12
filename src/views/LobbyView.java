package views;

import controllers.GameClientController;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class LobbyView extends JPanel {
    private GameClientController controller;

    // Phần Người chơi (Player List)
    private JList<String> onlinePlayersList;
    private DefaultListModel<String> playersListModel;
    private JButton btnChallenge, btnViewHistory;

    // Phần Bảng xếp hạng (Leaderboard) - Đã thay đổi thành JTable
    private JTable leaderboardTable;
    private DefaultTableModel leaderboardModel;

    // Thông tin người dùng
    private JLabel lblWelcome, lblYourStats;
    private String currentUsername; // Thêm biến để lưu tên người dùng hiện tại

    public LobbyView(GameClientController controller) {
        this.controller = controller;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // --- TOP PANEL (Welcome and Stats) ---
        JPanel topPanel = new JPanel(new BorderLayout());
        lblWelcome = new JLabel("Chào mừng!", JLabel.CENTER);
        lblWelcome.setFont(new Font("Arial", Font.BOLD, 18));
        topPanel.add(lblWelcome, BorderLayout.NORTH);

        lblYourStats = new JLabel("Thống kê: 0W-0D-0L", JLabel.CENTER);
        topPanel.add(lblYourStats, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);

        // --- LEFT PANEL (Online Players) ---
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(new TitledBorder("Người chơi"));

        playersListModel = new DefaultListModel<>();
        onlinePlayersList = new JList<>(playersListModel);
        onlinePlayersList.setFont(new Font("Monospaced", Font.PLAIN, 14)); // Tăng font
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

        // --- RIGHT PANEL (Leaderboard - JTable) ---
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(new TitledBorder("Bảng xếp hạng"));

        // 1. Khởi tạo Model và Table
        String[] columnNames = {"Hạng", "Tên", "Điểm", "Thắng", "Hòa", "Thua"};
        leaderboardModel = new DefaultTableModel(columnNames, 0) {
            // Ngăn chặn việc chỉnh sửa
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        leaderboardTable = new JTable(leaderboardModel);

        // 2. Định dạng giao diện chung
        leaderboardTable.setFont(new Font("Arial", Font.PLAIN, 14));
        leaderboardTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        leaderboardTable.setRowHeight(25);
        leaderboardTable.setFillsViewportHeight(true);
        leaderboardTable.setShowGrid(true);
        leaderboardTable.setGridColor(new Color(220, 220, 220));

        // 3. Căn chỉnh dữ liệu cột (Gọi phương thức riêng để giữ code sạch)
        setupLeaderboardTableAlignment();

        rightPanel.add(new JScrollPane(leaderboardTable), BorderLayout.CENTER);

        // --- SPLIT PANE ---
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setDividerLocation(400); // Điều chỉnh vị trí chia
        add(splitPane, BorderLayout.CENTER);
    }

    /**
     * Thiết lập căn chỉnh và chiều rộng cho các cột JTable của bảng xếp hạng.
     */
    private void setupLeaderboardTableAlignment() {
        // Căn giữa cho cột Hạng và các cột số
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);

        // Căn trái cho cột Tên
        DefaultTableCellRenderer leftRenderer = new DefaultTableCellRenderer();
        leftRenderer.setHorizontalAlignment(JLabel.LEFT);

        // Áp dụng Renderer và chiều rộng cột
        if (leaderboardTable.getColumnModel().getColumnCount() > 0) {
            leaderboardTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
            leaderboardTable.getColumnModel().getColumn(0).setPreferredWidth(50); // Hạng

            leaderboardTable.getColumnModel().getColumn(1).setCellRenderer(leftRenderer);
            leaderboardTable.getColumnModel().getColumn(1).setPreferredWidth(200); // Tên

            // Áp dụng căn giữa và chiều rộng cho các cột số còn lại
            for (int i = 2; i < leaderboardTable.getColumnModel().getColumnCount(); i++) {
                leaderboardTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
                leaderboardTable.getColumnModel().getColumn(i).setPreferredWidth(80);
            }
        }
    }
// ----------------------------------------------------------------------
    // CÁC PHƯƠNG THỨC CẬP NHẬT GIAO DIỆN
    // ----------------------------------------------------------------------

    public void setWelcomeMessage(String username) {
        // Lưu tên người dùng hiện tại
        this.currentUsername = username;

        lblWelcome.setText("Chào mừng, " + username + "!");
    }

    public void setYourStats(int wins, int draws, int losses) {
        lblYourStats.setText(String.format("Thống kê: %dW-%dD-%dL | Điểm: %d",
                wins, draws, losses, (wins * 3 + draws)));
    }

    public void updateOnlineList(String[] players) {
        playersListModel.clear();

        if (this.currentUsername == null) {
            // Fallback nếu currentUsername chưa được thiết lập
            for (String player : players) playersListModel.addElement(player);
            return;
        }

        for (String playerInfo : players) {
            // Trích xuất tên người chơi (ví dụ: "User1 (Online)" -> "User1")
            String playerName = playerInfo.split("\\(")[0].trim();

            // Lọc ra người chơi hiện tại
            if (!playerName.equals(this.currentUsername)) {
                playersListModel.addElement(playerInfo);
            }
        }
    }

    /**
     * Cập nhật bảng xếp hạng với dữ liệu từ server.
     * Giả định chuỗi leaderboard thô có định dạng: "Hạng|Tên|Điểm|Thắng|Hòa|Thua;;..."
     */
    /**
     * Cập nhật bảng xếp hạng với dữ liệu từ server.
     * Cần đảm bảo dữ liệu server gửi theo định dạng: "Hạng|Tên|Điểm|Thắng|Hòa|Thua;;..."
     */
    public void updateLeaderboard(String leaderboard) {
        // DÒNG DEBUG QUAN TRỌNG: Kiểm tra dữ liệu thô
        System.out.println("DEBUG LEADERBOARD RAW: >>>" + leaderboard + "<<<");

        leaderboardModel.setRowCount(0);

        // 1. Phân tách các dòng (row) dựa trên ";;" (Nếu Server dùng ký tự khác, hãy sửa ở đây)
        String[] lines = leaderboard.split(";;");

        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty()) continue;

            // 2. Phân tách các giá trị (cell) dựa trên "|" (Nếu Server dùng ký tự khác, hãy sửa ở đây)
            String[] parts = trimmedLine.split("\\|", -1);

            // Kiểm tra phải có đủ 6 cột
            final int EXPECTED_COLUMNS = 6;

            if (parts.length == EXPECTED_COLUMNS) {
                // Thêm một dòng (row) mới vào TableModel
                leaderboardModel.addRow(parts);
                // System.out.println("DEBUG LEADERBOARD: Added row: " + trimmedLine);
            } else {
                // In lỗi định dạng rõ ràng
                System.err.println("ERROR LEADERBOARD PARSING: Dòng bị bỏ qua do thiếu cột! ");
                System.err.println("  -> Dòng: '" + trimmedLine + "'");
                System.err.println("  -> Số cột tìm thấy: " + parts.length + ". Cần: " + EXPECTED_COLUMNS);
            }
        }
    }

    // ----------------------------------------------------------------------
    // CÁC PHƯƠNG THỨC XỬ LÝ SỰ KIỆN VÀ THÔNG ĐIỆP
    // ----------------------------------------------------------------------

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