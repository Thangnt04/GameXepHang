import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Vai trò 1: Backend Server & Socket
 * Lớp Server chính, lắng nghe kết nối và quản lý danh sách ClientHandler.
 */
public class Server {

    private static final int PORT = 12345;
    // GIỮ PRIVATE
    private Map<String, ClientHandler> onlineUsers = new ConcurrentHashMap<>();

    // Đối tượng DAO để tương tác CSDL
    protected DatabaseDAO databaseDAO;

    public Server() {
        databaseDAO = new DatabaseDAO();
    }

    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is running and listening on port " + PORT + "...");

            while (true) {
                // Chấp nhận kết nối mới
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());

                // Tạo một luồng (Thread) mới cho client này
                ClientHandler clientHandler = new ClientHandler(clientSocket, this, databaseDAO);
                new Thread(clientHandler).start();
            }

        } catch (IOException e) {
            System.err.println("ServerSocket Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Thêm user vào danh sách online khi đăng nhập thành công
    public void userLoggedIn(String username, ClientHandler handler) {
        onlineUsers.put(username, handler);
        System.out.println(username + " just logged in.");
        broadcastOnlineList();
        broadcastLeaderboard();
    }

    // Xóa user khỏi danh sách online khi ngắt kết nối
    public void userLoggedOut(String username) {
        if (username != null) {
            onlineUsers.remove(username);
            System.out.println(username + " logged out.");
            broadcastOnlineList();
        }
    }

    // Gửi danh sách online - CẢI THIỆN FORMAT
    public synchronized void broadcastOnlineList() {
        // Format: ONLINE_LIST:user1(5W-2D-1L):ONLINE:IDLE,user2(3W-1D-2L):ONLINE:BUSY
        StringBuilder userListMessage = new StringBuilder("ONLINE_LIST:");
        for (ClientHandler handler : onlineUsers.values()) {
            if (handler.getUsername() != null) {
                String status = handler.isInGame() ? "BUSY" : "IDLE";
                userListMessage.append(handler.getUsername())
                        .append("(")
                        .append(handler.getTotalWins()).append("W-")
                        .append(handler.getTotalDraws()).append("D-")
                        .append(handler.getTotalLosses()).append("L")
                        .append("):ONLINE:").append(status)
                        .append(",");
            }
        }
        if (userListMessage.length() > 12) {
            userListMessage.deleteCharAt(userListMessage.length() - 1);
        }

        for (ClientHandler handler : onlineUsers.values()) {
            handler.sendMessage(userListMessage.toString());
        }
    }

    // Gửi bảng xếp hạng - CẢI THIỆN FORMAT
    public synchronized void broadcastLeaderboard() {
        List<DatabaseDAO.User> leaderboard = databaseDAO.getLeaderboard();
        StringBuilder leaderboardMessage = new StringBuilder("LEADERBOARD:");
        int rank = 1;
        for (DatabaseDAO.User user : leaderboard) {
            leaderboardMessage.append(rank++).append(". ")
                    .append(user.username)
                    .append(" (").append(user.getPoints()).append(" pts")
                    .append(" | ").append(user.totalWins).append("W-")
                    .append(user.totalDraws).append("D-")
                    .append(user.totalLosses).append("L)\n");
        }

        for (ClientHandler handler : onlineUsers.values()) {
            handler.sendMessage(leaderboardMessage.toString());
        }
    }

    // Xử lý luồng thách đấu
    public synchronized void handleChallenge(String challengerName, String opponentName) {
        ClientHandler opponentHandler = onlineUsers.get(opponentName);
        ClientHandler challengerHandler = onlineUsers.get(challengerName);

        if (opponentHandler == null) {
            challengerHandler.sendMessage("SERVER_MSG:Lỗi: Người chơi '" + opponentName + "' không online.");
            return;
        }

        if (opponentHandler.isInGame()) {
            challengerHandler.sendMessage("SERVER_MSG:Người chơi '" + opponentName + "' đang bận.");
            return;
        }

        if (challengerHandler.isInGame()) {
            challengerHandler.sendMessage("SERVER_MSG:Bạn đang trong trận, không thể thách đấu.");
            return;
        }

        if (challengerName.equals(opponentName)) {
            challengerHandler.sendMessage("SERVER_MSG:Bạn không thể tự thách đấu chính mình.");
            return;
        }

        // Gửi lời mời thách đấu tới đối thủ
        opponentHandler.sendMessage("CHALLENGE_REQUEST:" + challengerName);
        challengerHandler.sendMessage("SERVER_MSG:Đã gửi lời mời tới '" + opponentName + "'. Đang chờ phản hồi...");
    }

    // Xử lý phản hồi thách đấu
    public synchronized void handleChallengeResponse(String challengerName, String opponentName, boolean accepted) {
        ClientHandler challengerHandler = onlineUsers.get(challengerName);
        ClientHandler opponentHandler = onlineUsers.get(opponentName);

        if (challengerHandler == null) {
            opponentHandler.sendMessage("SERVER_MSG:Người thách đấu (" + challengerName + ") đã offline.");
            return;
        }

        if (accepted) {
            // Kiểm tra lại trạng thái của cả 2
            if(challengerHandler.isInGame() || opponentHandler.isInGame()) {
                challengerHandler.sendMessage("SERVER_MSG:Không thể bắt đầu trận. Một trong hai người chơi đã vào trận khác.");
                opponentHandler.sendMessage("SERVER_MSG:Không thể bắt đầu trận. Một trong hai người chơi đã vào trận khác.");
                return;
            }

            // Chấp nhận -> Bắt đầu game
            challengerHandler.sendMessage("CHALLENGE_ACCEPTED:" + opponentName);
            opponentHandler.sendMessage("CHALLENGE_ACCEPTED:" + challengerName);

            // Tạo một phiên game mới
            GameSession gameSession = new GameSession(challengerHandler, opponentHandler, this);

            // Đặt trạng thái "Bận" cho cả hai
            challengerHandler.setInGame(true, gameSession);
            opponentHandler.setInGame(true, gameSession);

            // Bắt đầu ván đầu tiên
            gameSession.startNewRound();

            // Cập nhật trạng thái "Bận" cho toàn bộ client
            broadcastOnlineList();

        } else {
            // Từ chối
            challengerHandler.sendMessage("CHALLENGE_REJECTED:" + opponentName);
        }
    }

    // Khi một game kết thúc - CẢI THIỆN
    public synchronized void gameSessionEnded(GameSession session) {
        try {
            // Đặt lại trạng thái "Rảnh" cho cả 2 người chơi
            ClientHandler p1 = session.getPlayer1();
            ClientHandler p2 = session.getPlayer2();
            
            if (p1 != null && onlineUsers.containsKey(p1.getUsername())) {
                p1.setInGame(false, null);
            }
            if (p2 != null && onlineUsers.containsKey(p2.getUsername())) {
                p2.setInGame(false, null);
            }
            
            System.out.println("A game session has ended.");
            
            // Cập nhật lại danh sách và bảng xếp hạng
            broadcastOnlineList();
            broadcastLeaderboard();
        } catch (Exception e) {
            System.out.println("Error when ending game session: " + e.getMessage());
        }
    }

    // Thêm getter để GameSession có thể truy cập
    public DatabaseDAO getDatabaseDAO() {
        return databaseDAO;
    }

    // THÊM METHOD HELPER
    public boolean isUserOnline(String username) {
        return onlineUsers.containsKey(username);
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.startServer();
    }
}

