package controllers;

import services.AuthService;
import services.LobbyService;
import repositories.UserRepository;
import repositories.MatchRepository;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServerController {
    private static final int PORT = 12345;
    private Map<String, ClientController> onlineUsers = new ConcurrentHashMap<>();
    private Map<String, GameSessionController> sessionsByUser = new ConcurrentHashMap<>();

    private UserRepository userRepository;
    private MatchRepository matchRepository;
    private AuthService authService;
    private LobbyService lobbyService;

    public ServerController() {
        this.userRepository = new UserRepository();
        this.matchRepository = new MatchRepository();
        this.authService = new AuthService(userRepository);
        this.lobbyService = new LobbyService(userRepository);
    }

    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is running and listening on port " + PORT + "...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());

                ClientController clientController = new ClientController(clientSocket, this, authService, matchRepository);
                new Thread(clientController).start();
            }
        } catch (IOException e) {
            System.err.println("ServerSocket Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void userLoggedIn(String username, ClientController controller) {
        onlineUsers.put(username, controller);
        System.out.println(username + " just logged in.");
        lobbyService.broadcastOnlineList(onlineUsers, sessionsByUser);
        lobbyService.broadcastLeaderboard(onlineUsers);
    }

    public void userLoggedOut(String username) {
        if (username != null) {
            ClientController leaving = onlineUsers.remove(username);
            GameSessionController session = sessionsByUser.remove(username);

            if (session != null) {
                ClientController p1 = session.getPlayer1();
                ClientController p2 = session.getPlayer2();

                if (p1 != null) {
                    p1.setInGame(false, null);
                    sessionsByUser.remove(p1.getUsername());
                }
                if (p2 != null) {
                    p2.setInGame(false, null);
                    sessionsByUser.remove(p2.getUsername());
                    if (onlineUsers.containsKey(p2.getUsername())) {
                        p2.sendMessage("SERVER_MSG:Đối thủ đã rời trận. Bạn đã quay về sảnh.");
                    }
                }
            } else if (leaving != null) {
                leaving.setInGame(false, null);
            }

            System.out.println(username + " logged out.");
            lobbyService.broadcastOnlineList(onlineUsers, sessionsByUser);
            lobbyService.broadcastLeaderboard(onlineUsers);
        }
    }

    public synchronized void handleChallenge(String challengerName, String opponentName) {
        ClientController opponent = onlineUsers.get(opponentName);
        ClientController challenger = onlineUsers.get(challengerName);

        if (opponent == null) {
            challenger.sendMessage("SERVER_MSG:Lỗi: Người chơi '" + opponentName + "' không online.");
            return;
        }
        if (opponent.isInGame()) {
            challenger.sendMessage("SERVER_MSG:Người chơi '" + opponentName + "' đang bận.");
            return;
        }
        if (challenger.isInGame()) {
            challenger.sendMessage("SERVER_MSG:Bạn đang trong trận, không thể thách đấu.");
            return;
        }
        if (challengerName.equals(opponentName)) {
            challenger.sendMessage("SERVER_MSG:Bạn không thể tự thách đấu chính mình.");
            return;
        }

        opponent.sendMessage("CHALLENGE_REQUEST:" + challengerName);
        challenger.sendMessage("SERVER_MSG:Đã gửi lời mời tới '" + opponentName + "'. Đang chờ phản hồi...");
    }

    public synchronized void handleChallengeResponse(String challengerName, String opponentName, boolean accepted) {
        ClientController challenger = onlineUsers.get(challengerName);
        ClientController opponent = onlineUsers.get(opponentName);

        if (challenger == null) {
            opponent.sendMessage("SERVER_MSG:Người thách đấu (" + challengerName + ") đã offline.");
            return;
        }

        if (accepted) {
            if (challenger.isInGame() || opponent.isInGame()) {
                challenger.sendMessage("SERVER_MSG:Không thể bắt đầu trận. Một trong hai người chơi đã vào trận khác.");
                opponent.sendMessage("SERVER_MSG:Không thể bắt đầu trận. Một trong hai người chơi đã vào trận khác.");
                return;
            }

            challenger.sendMessage("CHALLENGE_ACCEPTED:" + opponentName);
            opponent.sendMessage("CHALLENGE_ACCEPTED:" + challengerName);

            GameSessionController gameSession = new GameSessionController(challenger, opponent, this, matchRepository);
            challenger.setInGame(true, gameSession);
            opponent.setInGame(true, gameSession);

            sessionsByUser.put(challengerName, gameSession);
            sessionsByUser.put(opponentName, gameSession);

            gameSession.startMatch();
            lobbyService.broadcastOnlineList(onlineUsers, sessionsByUser);
        } else {
            challenger.sendMessage("CHALLENGE_REJECTED:" + opponentName);
        }
    }

    public synchronized void gameSessionEnded(GameSessionController session) {
        try {
            ClientController p1 = session.getPlayer1();
            ClientController p2 = session.getPlayer2();

            if (p1 != null && onlineUsers.containsKey(p1.getUsername())) {
                p1.setInGame(false, null);
                sessionsByUser.remove(p1.getUsername());
            }
            if (p2 != null && onlineUsers.containsKey(p2.getUsername())) {
                p2.setInGame(false, null);
                sessionsByUser.remove(p2.getUsername());
            }

            System.out.println("A game session has ended.");
            lobbyService.broadcastOnlineList(onlineUsers, sessionsByUser);
            lobbyService.broadcastLeaderboard(onlineUsers);
        } catch (Exception e) {
            System.out.println("Error when ending game session: " + e.getMessage());
        }
    }

    public boolean isUserOnline(String username) {
        return onlineUsers.containsKey(username);
    }

    public UserRepository getUserRepository() {
        return userRepository;
    }

    public MatchRepository getMatchRepository() {
        return matchRepository;
    }

    public static void main(String[] args) {
        ServerController server = new ServerController();
        server.startServer();
    }
}
