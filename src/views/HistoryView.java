package views;

import controllers.GameClientController;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class HistoryView extends JPanel {
    private GameClientController controller;
    private JLabel lblTitle;
    private JTable historyTable;
    private DefaultTableModel tableModel;
    private JButton btnBack;
    private SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    public HistoryView(GameClientController controller) {
        this.controller = controller;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        lblTitle = new JLabel("Lịch sử đấu của...", JLabel.CENTER);
        lblTitle.setFont(new Font("Arial", Font.BOLD, 18));
        add(lblTitle, BorderLayout.NORTH);

        String[] columnNames = {"Đối thủ", "Kết quả", "Tỷ số (Bạn - Đ/thủ)", "Thời gian"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        historyTable = new JTable(tableModel);
        historyTable.setFont(new Font("Arial", Font.PLAIN, 12));
        historyTable.setFillsViewportHeight(true);
        historyTable.setRowHeight(20);

        historyTable.getColumnModel().getColumn(0).setPreferredWidth(150);
        historyTable.getColumnModel().getColumn(1).setPreferredWidth(70);
        historyTable.getColumnModel().getColumn(2).setPreferredWidth(130);
        historyTable.getColumnModel().getColumn(3).setPreferredWidth(120);

        add(new JScrollPane(historyTable), BorderLayout.CENTER);

        btnBack = new JButton("Quay lại Sảnh");
        btnBack.addActionListener(e -> controller.showPanel("LOBBY"));
        add(btnBack, BorderLayout.SOUTH);
    }

    public void displayHistory(String targetUsername, String data) {
        lblTitle.setText("Lịch sử đấu của: " + targetUsername);
        tableModel.setRowCount(0);

        if (data == null || data.isEmpty()) {
            return;
        }

        String[] matches = data.split(";;");
        for (String match : matches) {
            String[] parts = match.split(",");
            if (parts.length < 5) continue;

            String opponent = parts[0];
            String result = parts[1];
            String myScore = parts[2];
            String opponentScore = parts[3];
            Date date = new Date(Long.parseLong(parts[4]));

            Object[] rowData = {
                    opponent,
                    result,
                    myScore + " - " + opponentScore,
                    sdf.format(date)
            };

            tableModel.addRow(rowData);
        }
    }
}
