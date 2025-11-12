package views;

import controllers.GameClientController;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LoginView extends JPanel implements ActionListener {
    private GameClientController controller;
    private JTextField tfUsername;
    private JPasswordField pfPassword;
    private JButton btnLogin, btnRegister;

    public LoginView(GameClientController controller) {
        this.controller = controller;
        initComponents();
    }

    private void initComponents() {
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
            controller.sendMessageToServer("LOGIN:" + user + ":" + pass);
        } else if (e.getSource() == btnRegister) {
            controller.sendMessageToServer("REGISTER:" + user + ":" + pass);
        }
    }

    public String getUsername() {
        return tfUsername.getText();
    }

    public void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Lỗi", JOptionPane.ERROR_MESSAGE);
    }

    public void showSuccess(String message) {
        JOptionPane.showMessageDialog(this, message, "Thành công", JOptionPane.INFORMATION_MESSAGE);
    }
}
