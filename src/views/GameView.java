package views;

import controllers.GameClientController;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GameView extends JPanel implements ActionListener {
    private GameClientController controller;
    private JLabel lblOpponent, lblTimer, lblMatchProgress, lblYourProgress, lblOpponentProgress;
    private JPanel pnlOrder, pnlShelf, pnlPackingTray;
    private JButton btnSubmit, btnExit, btnResetTray;

    private javax.swing.Timer gameTimer;
    private int timeLeft;
    private List<String> currentOrder;
    private List<String> currentPackingTray;
    private boolean hasAskedToPlayAgain = false;

    // Các hằng số cho UI trực quan
    private static final int ROW_SPACING = 1;
    private static final int HORIZONTAL_GAP = 5;
    private static final int ICON_WIDTH = 70;
    private static final int ICON_HEIGHT = 50;
    private int myProgress = 0;

    public GameView(GameClientController controller) {
        this.controller = controller;
        this.currentOrder = new ArrayList<>();
        this.currentPackingTray = new ArrayList<>();
        setupUI();
    }

    private void setupUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // --- TOP PANEL (Thông tin & Timer) ---
        JPanel topPanel = new JPanel(new BorderLayout());
        JPanel infoPanel = new JPanel(new GridLayout(2, 2));
        lblOpponent = new JLabel("Đang chơi với: ");
        lblYourProgress = new JLabel("Bạn: Hoàn thành 0/5 đơn");
        lblOpponentProgress = new JLabel("Đối thủ: Hoàn thành 0/5 đơn");
        lblMatchProgress = new JLabel("Đơn hàng 1/5", JLabel.RIGHT);

        infoPanel.add(lblOpponent);
        infoPanel.add(lblMatchProgress);
        infoPanel.add(lblYourProgress);
        infoPanel.add(lblOpponentProgress);
        topPanel.add(infoPanel, BorderLayout.NORTH);

        lblTimer = new JLabel("Thời gian: 1:00", JLabel.CENTER);
        lblTimer.setFont(new Font("Arial", Font.BOLD, 24));
        lblTimer.setForeground(new Color(0, 150, 0));
        topPanel.add(lblTimer, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);

        // --- CENTER PANEL (Kệ Hàng & Khay Xếp Hàng) ---
        JPanel centerPanel = new JPanel(new GridLayout(2, 1, 10, 10));

        // KỆ HÀNG (Sử dụng BoxLayout cho việc chia hàng)
        pnlShelf = new JPanel();
        pnlShelf.setLayout(new BoxLayout(pnlShelf, BoxLayout.Y_AXIS));
        pnlShelf.setBorder(new TitledBorder("Kệ Hàng"));

        JScrollPane shelfScrollPane = new JScrollPane(pnlShelf);
        shelfScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        shelfScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        centerPanel.add(shelfScrollPane);

        // KHAY XẾP HÀNG (Sử dụng FlowLayout)
        pnlPackingTray = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        pnlPackingTray.setBorder(new TitledBorder("Khay Xếp Hàng"));
        centerPanel.add(new JScrollPane(pnlPackingTray));

        add(centerPanel, BorderLayout.CENTER);

        // --- WEST PANEL (Đơn Hàng) ---
        pnlOrder = new JPanel();
        pnlOrder.setLayout(new BoxLayout(pnlOrder, BoxLayout.Y_AXIS));
        pnlOrder.setBorder(new TitledBorder("Đơn Hàng"));
        add(new JScrollPane(pnlOrder), BorderLayout.WEST);

        // --- SOUTH PANEL (Buttons) ---
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

        // --- TIMER LOGIC ---
        gameTimer = new javax.swing.Timer(1000, e -> {
            timeLeft--;
            if (timeLeft >= 0) {
                lblTimer.setText("Thời gian: " + (timeLeft / 60) + ":" + String.format("%02d", (timeLeft % 60)));
            }
            if (timeLeft <= 30) lblTimer.setForeground(Color.RED);
            if (timeLeft <= 0) {
                gameTimer.stop();
                btnSubmit.setEnabled(false);
                btnExit.setEnabled(false);
                btnResetTray.setEnabled(false);
                lblTimer.setText("HẾT GIỜ!");
            }
        });
    }

    // HÀM TIỆN ÍCH: Tạo nút có ảnh (Đảm bảo đường dẫn tài nguyên /resources/items/item.png là chính xác)
    private JButton createItemButton(String itemName) {
        JButton btn = new JButton(itemName);
        btn.setFocusPainted(false);

        try {
            String imagePath = "/resources/items/" + itemName + ".png";
            ImageIcon icon = new ImageIcon(getClass().getResource(imagePath));

            if (icon.getImageLoadStatus() == MediaTracker.COMPLETE && icon.getIconWidth() > 0) {
                Image scaledImage = icon.getImage().getScaledInstance(ICON_WIDTH, ICON_HEIGHT, Image.SCALE_SMOOTH);
                ImageIcon scaledIcon = new ImageIcon(scaledImage);

                btn.setIcon(scaledIcon);
                btn.setText(itemName);

                btn.setHorizontalTextPosition(SwingConstants.CENTER);
                btn.setVerticalTextPosition(SwingConstants.BOTTOM);
                btn.setFont(new Font("Arial", Font.PLAIN, 10));
                btn.setPreferredSize(new Dimension(ICON_WIDTH + 30, ICON_HEIGHT + 25));

            } else {
                btn.setPreferredSize(new Dimension(ICON_WIDTH + 30, ICON_HEIGHT + 25));
            }
        } catch (Exception e) {
            btn.setPreferredSize(new Dimension(ICON_WIDTH + 30, ICON_HEIGHT + 25));
        }
        return btn;
    }

    // ----------------------------------------------------------------------
    // PHƯƠNG THỨC XỬ LÝ TRẬN ĐẤU
    // ----------------------------------------------------------------------

    public void startMatch(String opponent, int time) {
        lblOpponent.setText("Đang chơi với: " + opponent);
        timeLeft = time;
        lblTimer.setText("Thời gian: " + (timeLeft / 60) + ":" + String.format("%02d", (timeLeft % 60)));
        lblTimer.setForeground(new Color(0, 150, 0));

        updateMyProgress(0);
        updateOpponentProgress(0);
        hasAskedToPlayAgain = false;

        btnSubmit.setEnabled(true);
        btnExit.setEnabled(true);
        btnResetTray.setEnabled(true);

        pnlOrder.removeAll();
        pnlShelf.removeAll();
        pnlPackingTray.removeAll();
        pnlOrder.revalidate();
        pnlShelf.revalidate();
        pnlPackingTray.revalidate();

        gameTimer.start();
    }

    public void displayNewOrder(int orderIndex, String orderCsv, String shelfCsv) {
        lblMatchProgress.setText(String.format("Đơn hàng %d/5", orderIndex + 1));

        currentOrder = Arrays.asList(orderCsv.split(","));
        List<String> shelfItems = Arrays.asList(shelfCsv.split(","));
        currentPackingTray.clear();

        // 1. Cập nhật Đơn Hàng
        pnlOrder.removeAll();
        for (int i = 0; i < currentOrder.size(); i++) {
            JLabel orderItem = new JLabel((i + 1) + ". " + currentOrder.get(i));
            orderItem.setFont(new Font("Monospaced", Font.PLAIN, 14));
            orderItem.setAlignmentX(Component.LEFT_ALIGNMENT);
            pnlOrder.add(orderItem);
        }
        pnlOrder.add(Box.createVerticalGlue());
        pnlOrder.revalidate();

        // 2. Cập nhật Kệ Hàng (Sử dụng nút có Icon)
        pnlShelf.removeAll();
        final int ITEMS_PER_ROW = 6;
        JPanel currentRowPanel = null;
        int itemCount = 0;

        for (String item : shelfItems) {
            if (itemCount % ITEMS_PER_ROW == 0) {
                if (currentRowPanel != null) {
                    pnlShelf.add(currentRowPanel);
                    pnlShelf.add(Box.createVerticalStrut(ROW_SPACING));
                }
                currentRowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, HORIZONTAL_GAP, 0));
                currentRowPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            }

            JButton btn = createItemButton(item);
            btn.addActionListener(e -> {
                if (btnSubmit.isEnabled()) {
                    currentPackingTray.add(item);
                    updatePackingTrayUI();
                }
            });
            currentRowPanel.add(btn);
            itemCount++;
        }

        if (itemCount > 0 && currentRowPanel != null) {
            pnlShelf.add(currentRowPanel);
        }

        // 3. Cập nhật Khay Xếp Hàng
        updatePackingTrayUI();
        btnSubmit.setEnabled(true);
        btnResetTray.setEnabled(true);

        revalidate();
        repaint();
    }

    public void updateMyProgress(int progress) {
        this.myProgress = progress;
        lblYourProgress.setText(String.format("Bạn: Hoàn thành %d/5 đơn", progress));
        // Cập nhật chi tiết lblMatchProgress
        if (progress < 5 && currentOrder != null && !currentOrder.isEmpty()) {
            lblMatchProgress.setText(String.format("Đơn hàng %d/5 (Đã xếp: %d/%d)",
                    (myProgress + 1),
                    currentPackingTray.size(),
                    currentOrder.size()));
        } else if (progress == 5) {
            lblMatchProgress.setText("Đã xong 5/5!");
        }
    }

    public void updateOpponentProgress(int progress) {
        lblOpponentProgress.setText(String.format("Đối thủ: Hoàn thành %d/5 đơn", progress));
    }

    private void updatePackingTrayUI() {
        pnlPackingTray.removeAll();

        for (int i = 0; i < currentPackingTray.size(); i++) {
            String item = currentPackingTray.get(i);
            JButton btn = createItemButton(item);

            // Đánh số thứ tự và thay đổi kích thước/text cho Khay
            btn.setText((i + 1) + ". " + item);
            btn.setPreferredSize(new Dimension(ICON_WIDTH + 10, ICON_HEIGHT + 25));

            final int indexToRemove = i;
            btn.addActionListener(e -> {
                if (btnSubmit.isEnabled()) {
                    // Xóa vật phẩm khỏi khay và cập nhật lại UI
                    currentPackingTray.remove(indexToRemove);
                    updatePackingTrayUI();
                }
            });
            pnlPackingTray.add(btn);
        }

        // Cập nhật lại thông tin tiến độ xếp hàng
        if (myProgress < 5 && currentOrder != null && !currentOrder.isEmpty()) {
            lblMatchProgress.setText(String.format("Đơn hàng %d/5 (Đã xếp: %d/%d)",
                    (myProgress + 1),
                    currentPackingTray.size(),
                    currentOrder.size()));
        } else if (myProgress == 5) {
            lblMatchProgress.setText("Đã xong 5/5!");
        }

        pnlPackingTray.revalidate();
        pnlPackingTray.repaint();
    }

    public void stopTimer() {
        if (gameTimer.isRunning()) gameTimer.stop();
    }

    public void reEnableSubmission() {
        btnSubmit.setEnabled(true);
        btnResetTray.setEnabled(true);
    }

    public boolean hasAskedToPlayAgain() {
        return hasAskedToPlayAgain;
    }

    public void setHasAskedToPlayAgain(boolean asked) {
        this.hasAskedToPlayAgain = asked;
    }
// ----------------------------------------------------------------------
    // XỬ LÝ SỰ KIỆN NÚT
    // ----------------------------------------------------------------------

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnSubmit) {
            if (currentPackingTray.size() != currentOrder.size()) {
                JOptionPane.showMessageDialog(this, "Bạn phải xếp đủ " + currentOrder.size() + " món!");
                return;
            }
            btnSubmit.setEnabled(false);
            btnResetTray.setEnabled(false);
            // GỌI CONTROLLER
            controller.sendMessageToServer("SUBMIT_ORDER:" + String.join(",", currentPackingTray));

        } else if (e.getSource() == btnResetTray) {
            currentPackingTray.clear();
            updatePackingTrayUI();

        } else if (e.getSource() == btnExit) {
            gameTimer.stop();
            btnSubmit.setEnabled(false);
            btnExit.setEnabled(false);
            btnResetTray.setEnabled(false);
            // GỌI CONTROLLER
            controller.sendMessageToServer("FORFEIT");
        }
    }

    // ----------------------------------------------------------------------
    // HIỂN THỊ THÔNG BÁO VÀ HỘP THOẠI
    // ----------------------------------------------------------------------

    public int showResultDialog(String message, String title) {
        stopTimer();
        return JOptionPane.showConfirmDialog(this,
                message + "\n\nBạn có muốn chơi tiếp?",
                title,
                JOptionPane.YES_NO_OPTION);
    }

    public int showPlayAgainRequest() {
        return JOptionPane.showConfirmDialog(this,
                "Đối thủ muốn chơi tiếp. Bạn có đồng ý?",
                "Yêu cầu chơi tiếp",
                JOptionPane.YES_NO_OPTION);
    }

    public void showOpponentExited(String message) {
        stopTimer();
        setHasAskedToPlayAgain(true);
        JOptionPane.showMessageDialog(this, message, "Trận đấu kết thúc", JOptionPane.INFORMATION_MESSAGE);
    }

    public void showSubmitError(String message) {
        JOptionPane.showMessageDialog(this, message, "Sai rồi!", JOptionPane.WARNING_MESSAGE);
    }
}
