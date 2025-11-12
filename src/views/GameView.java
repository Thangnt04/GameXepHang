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

    public GameView(GameClientController controller) {
        this.controller = controller;
        this.currentOrder = new ArrayList<>();
        this.currentPackingTray = new ArrayList<>();
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));

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

        pnlOrder.removeAll();
        for (int i = 0; i < currentOrder.size(); i++) {
            pnlOrder.add(new JLabel((i + 1) + ". " + currentOrder.get(i)));
        }

        pnlShelf.removeAll();
        for (String item : shelfItems) {
            JButton btn = new JButton(item);
            btn.addActionListener(e -> {
                if (btnSubmit.isEnabled()) {
                    currentPackingTray.add(item);
                    updatePackingTrayUI();
                }
            });
            pnlShelf.add(btn);
        }

        updatePackingTrayUI();
        btnSubmit.setEnabled(true);
        btnResetTray.setEnabled(true);

        revalidate();
        repaint();
    }

    public void updateMyProgress(int progress) {
        lblYourProgress.setText(String.format("Bạn: Hoàn thành %d/5 đơn", progress));
        if (progress < 5) {
            lblMatchProgress.setText(String.format("Đơn hàng %d/5", (progress + 1)));
        } else {
            lblMatchProgress.setText("Đã xong 5/5!");
        }
    }

    public void updateOpponentProgress(int progress) {
        lblOpponentProgress.setText(String.format("Đối thủ: Hoàn thành %d/5 đơn", progress));
    }

    private void updatePackingTrayUI() {
        pnlPackingTray.removeAll();
        for (int i = 0; i < currentPackingTray.size(); i++) {
            pnlPackingTray.add(new JLabel((i + 1) + ". " + currentPackingTray.get(i)));
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

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnSubmit) {
            if (currentPackingTray.size() != currentOrder.size()) {
                JOptionPane.showMessageDialog(this, "Bạn phải xếp đủ " + currentOrder.size() + " món!");
                return;
            }
            btnSubmit.setEnabled(false);
            btnResetTray.setEnabled(false);
            controller.sendMessageToServer("SUBMIT_ORDER:" + String.join(",", currentPackingTray));

        } else if (e.getSource() == btnResetTray) {
            currentPackingTray.clear();
            updatePackingTrayUI();

        } else if (e.getSource() == btnExit) {
            gameTimer.stop();
            btnSubmit.setEnabled(false);
            btnExit.setEnabled(false);
            btnResetTray.setEnabled(false);
            controller.sendMessageToServer("FORFEIT");
        }
    }

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
