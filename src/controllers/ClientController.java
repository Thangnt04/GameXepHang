package controllers;

import models.ClientSession;
import services.AuthService;
import repositories.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;

public class ClientController implements Runnable {
    private Socket clientSocket;
    private ServerController server;
    private AuthService authService;
    private MatchRepository matchRepository;
    private PrintWriter out;
    private BufferedReader in;

    private ClientSession session;
    private GameSessionController currentGameSession = null;
    private volatile boolean isConnected = true;

    public ClientController(Socket socket, ServerController server, AuthService authService, MatchRepository matchRepository) {
        this.clientSocket = socket;
        this.server = server;
        this.authService = authService;
        this.matchRepository = matchRepository;
        try {
            this.out = new PrintWriter(clientSocket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        String clientMessage;
        try {
            while ((clientMessage = in.readLine()) != null) {
                System.out.println("Received from [" + (session != null ? session.getUsername() : "Not logged in") + "]: " + clientMessage);
                processMessage(clientMessage);
            }
        } catch (SocketException e) {
            System.out.println("Client " + (session != null ? session.getUsername() : "") + " disconnected.");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }

    private void processMessage(String message) {
        String[] parts = message.split(":", 2);
        String command = parts[0];

        if (session == null) {
            if (command.equals("LOGIN")) {
                handleLogin(parts[1]);
            } else if (command.equals("REGISTER")) {
                handleRegister(parts[1]);
            }
            return;
        }

        switch (command) {
            case "CHALLENGE":
                server.handleChallenge(session.getUsername(), parts[1]);
                break;
            case "CHALLENGE_RESPONSE":
                String[] responseParts = parts[1].split(":");
                server.handleChallengeResponse(responseParts[0], session.getUsername(), responseParts[1].equals("accept"));
                break;
            case "SUBMIT_ORDER":
                if (session.isInGame() && currentGameSession != null) {
                    currentGameSession.receivePlayerSubmission(this, parts[1]);
                }
                break;
            case "PLAY_AGAIN_REQUEST":
                if (session.isInGame() && currentGameSession != null) {
                    currentGameSession.playerWantsToPlayAgain(this);
                }
                break;
            case "EXIT_GAME":
                if (session.isInGame() && currentGameSession != null) {
                    currentGameSession.playerExited(this);
                }
                break;
            case "FORFEIT":
                if (session.isInGame() && currentGameSession != null) {
                    currentGameSession.playerForfeited(this);
                }
                break;
            case "GET_HISTORY":
                handleGetHistory(parts[1]);
                break;
        }
    }

    private void handleLogin(String credentials) {
        String[] parts = credentials.split(":");
        if (parts.length != 2) return;

        ClientSession loginSession = authService.login(parts[0], parts[1]);
        if (loginSession != null) {
            if (server.isUserOnline(loginSession.getUsername())) {
                sendMessage("LOGIN_FAIL:Tài khoản này đã được đăng nhập ở nơi khác.");
                return;
            }
            this.session = loginSession;
            sendMessage(String.format("LOGIN_SUCCESS:%d:%d:%d:%d",
                    session.getTotalWins(), session.getTotalDraws(), session.getTotalLosses(), session.getTotalMatches()));
            server.userLoggedIn(session.getUsername(), this);
        } else {
            sendMessage("LOGIN_FAIL:Sai tên đăng nhập hoặc mật khẩu.");
        }
    }

    private void handleRegister(String credentials) {
        String[] parts = credentials.split(":");
        if (parts.length != 2) return;

        if (authService.register(parts[0], parts[1])) {
            sendMessage("REGISTER_SUCCESS:Đăng ký thành công. Vui lòng đăng nhập.");
        } else {
            sendMessage("REGISTER_FAIL:Tên đăng nhập đã tồn tại.");
        }
    }

    private void handleGetHistory(String targetUsername) {
        List<MatchRepository.MatchHistory> historyList = matchRepository.getMatchHistory(targetUsername);
        StringBuilder historyData = new StringBuilder("HISTORY_DATA:");
        historyData.append(targetUsername).append(":");

        for (MatchRepository.MatchHistory match : historyList) {
            historyData.append(match.opponentName).append(",");
            historyData.append(match.myResult).append(",");
            historyData.append(match.myScore).append(",");
            historyData.append(match.opponentScore).append(",");
            historyData.append(match.playedAt.getTime());
            historyData.append(";;");
        }

        if (historyList.size() > 0) {
            historyData.setLength(historyData.length() - 2);
        }
        sendMessage(historyData.toString());
    }

    public void sendMessage(String message) {
        if (out != null && isConnected) {
            try {
                out.println(message);
                if (out.checkError()) {
                    System.out.println("Error sending message to " + (session != null ? session.getUsername() : "unknown"));
                    isConnected = false;
                }
            } catch (Exception e) {
                System.out.println("Exception sending message: " + e.getMessage());
                isConnected = false;
            }
        }
    }

    private void cleanup() {
        isConnected = false;
        try {
            if (session != null && session.isInGame() && currentGameSession != null) {
                currentGameSession.playerForfeited(this);
                currentGameSession = null;
                session.setInGame(false);
            }

            if (session != null) {
                server.userLoggedOut(session.getUsername());
            }

            if (in != null) in.close();
            if (out != null) out.close();
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            System.out.println("Error during cleanup: " + e.getMessage());
        }
    }

    public String getUsername() { return session != null ? session.getUsername() : null; }
    public int getUserId() { return session != null ? session.getUserId() : -1; }
    public ClientSession getSession() { return session; }
    public boolean isInGame() { return session != null && session.isInGame(); }

    public void setInGame(boolean inGame, GameSessionController gameSession) {
        if (session != null) {
            session.setInGame(inGame);
            this.currentGameSession = gameSession;
        }
    }
}
