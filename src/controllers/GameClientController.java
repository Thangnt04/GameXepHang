package controllers;

import models.GameState;
import views.*;
import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class GameClientController extends JFrame {
    private static final String SERVER_ADDRESS = "127.0.0.1";
    private static final int SERVER_PORT = 12345;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private CardLayout cardLayout;
    private JPanel mainPanel;
    private LoginView loginView;
    private LobbyView lobbyView;
    private GameView gameView;
    private HistoryView historyView;

    private GameState gameState;

    public GameClientController() {
        gameState = new GameState();
        
        setTitle("Game Xếp Đơn Hàng Siêu Thị");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        loginView = new LoginView(this);
        lobbyView = new LobbyView(this);
        gameView = new GameView(this);
        historyView = new HistoryView(this);

        mainPanel.add(loginView, "LOGIN");
        mainPanel.add(lobbyView, "LOBBY");
        mainPanel.add(gameView, "GAME");
        mainPanel.add(historyView, "HISTORY");

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
                    gameState.setUsername(loginView.getUsername());
                    lobbyView.setWelcomeMessage(gameState.getUsername());
                    if (statsData.length >= 3) {
                        lobbyView.setYourStats(Integer.parseInt(statsData[0]),
                                Integer.parseInt(statsData[1]), Integer.parseInt(statsData[2]));
                    }
                    showPanel("LOBBY");
                    break;
                case "LOGIN_FAIL":
                case "REGISTER_FAIL":
                    loginView.showError(data);
                    break;
                case "REGISTER_SUCCESS":
                    loginView.showSuccess(data);
                    break;
                case "ONLINE_LIST":
                    lobbyView.updateOnlineList(data.split(","));
                    break;
                case "LEADERBOARD":
                    lobbyView.updateLeaderboard(data);
                    break;
                case "SERVER_MSG":
                    lobbyView.showMessage(data);
                    break;
                case "CHALLENGE_REQUEST":
                    int choice = lobbyView.showChallengeRequest(data);
                    String response = (choice == JOptionPane.YES_OPTION) ? "accept" : "reject";
                    sendMessageToServer("CHALLENGE_RESPONSE:" + data + ":" + response);
                    break;
                case "CHALLENGE_REJECTED":
                    lobbyView.showMessage("Người chơi '" + data + "' đã từ chối.");
                    break;
                case "GAME_START":
                    String[] gameData = data.split(":");
                    gameState.setCurrentOpponent(gameData[0]);
                    gameView.startMatch(gameData[0], Integer.parseInt(gameData[1]));
                    showPanel("GAME");
                    break;
                case "NEW_ORDER":
                    String[] orderData = data.split(":", 3);
                    gameView.displayNewOrder(Integer.parseInt(orderData[0]), orderData[1], orderData[2]);
                    break;
                case "UPDATE_PROGRESS":
                    gameState.setMyProgress(Integer.parseInt(data));
                    gameView.updateMyProgress(gameState.getMyProgress());
                    break;
                case "OPPONENT_PROGRESS":
                    gameState.setOpponentProgress(Integer.parseInt(data));
                    gameView.updateOpponentProgress(gameState.getOpponentProgress());
                    break;
                case "SUBMIT_FAIL":
                    gameView.showSubmitError(data);
                    gameView.reEnableSubmission();
                    break;
                case "ROUND_RESULT":
                    String[] resultData = data.split(":", 2);
                    if (!gameView.hasAskedToPlayAgain()) {
                        gameView.setHasAskedToPlayAgain(true);
                        int playChoice = gameView.showResultDialog(resultData[1], "Kết quả - " + resultData[0]);
                        if (playChoice == JOptionPane.YES_OPTION) {
                            sendMessageToServer("PLAY_AGAIN_REQUEST");
                        } else {
                            sendMessageToServer("EXIT_GAME");
                            showPanel("LOBBY");
                        }
                    }
                    break;
                case "PLAY_AGAIN_REQUEST":
                    if (!gameView.hasAskedToPlayAgain()) {
                        gameView.setHasAskedToPlayAgain(true);
                        int playChoice2 = gameView.showPlayAgainRequest();
                        if (playChoice2 == JOptionPane.YES_OPTION) {
                            sendMessageToServer("PLAY_AGAIN_REQUEST");
                        } else {
                            sendMessageToServer("EXIT_GAME");
                            showPanel("LOBBY");
                        }
                    }
                    break;
                case "OPPONENT_EXITED":
                    gameView.showOpponentExited(data);
                    showPanel("LOBBY");
                    break;
                case "HISTORY_DATA":
                    String[] historyParts = data.split(":", 2);
                    String targetUsername = historyParts[0];
                    String historyData = (historyParts.length > 1) ? historyParts[1] : "";
                    historyView.displayHistory(targetUsername, historyData);
                    showPanel("HISTORY");
                    break;
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameClientController client = new GameClientController();
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
