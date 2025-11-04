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
    // Mapping user -> GameSession để dọn dẹp trạng thái BUSY bị kẹt
    private final Map<String, GameSession> sessionsByUser = new ConcurrentHashMap<>();

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
            // Lấy handler trước khi xóa khỏi map
            ClientHandler leaving = onlineUsers.remove(username);

            // Nếu user đang trong một session -> reset cả 2 bên và xóa mapping
            GameSession s = sessionsByUser.remove(username);
            if (s != null) {
                ClientHandler p1 = s.getPlayer1();
                ClientHandler p2 = s.getPlayer2();

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
                // Không có session nào, đảm bảo cờ được trả về IDLE
                leaving.setInGame(false, null);
            }

            System.out.println(username + " logged out.");
            broadcastOnlineList();
            broadcastLeaderboard();
        }
    }

    // Gửi danh sách lobby: hiển thị TẤT CẢ người chơi + trạng thái ONLINE/OFFLINE và IDLE/BUSY
    public synchronized void broadcastOnlineList() {
        // Trước khi build danh sách, dọn các session "mồ côi" để tránh kẹt BUSY
        cleanupStaleSessions();

        // Format: ONLINE_LIST:username(5W-2D-1L):ONLINE:IDLE,username2(...):OFFLINE:IDLE,...
        StringBuilder userListMessage = new StringBuilder("ONLINE_LIST:");
        try {
//            List<DatabaseDAO.User> allUsers = databaseDAO.getLeaderboard(); // View đã hiển thị tất cả users
//            for (DatabaseDAO.User u : allUsers) {
//                ClientHandler h = onlineUsers.get(u.username);
//                boolean online = (h != null);
//                String presence = online ? "ONLINE" : "OFFLINE";
//                String activity = (online && h.isInGame()) ? "BUSY" : "IDLE";
//                userListMessage
//                        .append(u.username)
//                        .append("(").append(u.totalWins).append("W-")
//                        .append(u.totalDraws).append("D-")
//                        .append(u.totalLosses).append("L")
//                        .append("):").append(presence).append(":").append(activity)
//                        .append(",");
            List<DatabaseDAO.User> allUsers = databaseDAO.getLeaderboard(); // 1. Lấy danh sách (đang sắp xếp theo điểm)

            // Sắp xếp lại danh sách allUsers theo 3 tiêu chí
            allUsers.sort((u1, u2) -> {
                ClientHandler h1 = onlineUsers.get(u1.username);
                ClientHandler h2 = onlineUsers.get(u2.username);
                boolean online1 = (h1 != null);
                boolean online2 = (h2 != null);
                boolean busy1 = (online1 && h1.isInGame());
                boolean busy2 = (online2 && h2.isInGame());

                // Tiêu chí 1: Ưu tiên Online (Online đứng trước Offline)
                if (online1 && !online2) return -1;
                if (!online1 && online2) return 1;

                // Tiêu chí 2: Ưu tiên Rảnh (Idle đứng trước Busy)
                // (Chỉ áp dụng khi cả hai cùng online hoặc cùng offline)
                if (online1 && online2) {
                    if (!busy1 && busy2) return -1; // u1 (Idle) < u2 (Busy)
                    if (busy1 && !busy2) return 1;  // u1 (Busy) > u2 (Idle)
                }

                // Tiêu chí 3: Sắp xếp theo tên A-Z
                return u1.username.compareToIgnoreCase(u2.username);
            });


            // 3. Xây dựng chuỗi tin nhắn từ danh sách ĐÃ ĐƯỢC SẮP XẾP MỚI
            for (DatabaseDAO.User u : allUsers) {
                ClientHandler h = onlineUsers.get(u.username);
                boolean online = (h != null);
                String presence = online ? "ONLINE" : "OFFLINE";
                String activity = (online && h.isInGame()) ? "BUSY" : "IDLE";
                userListMessage
                        .append(u.username)
                        .append("(").append(u.totalWins).append("W-")
                        .append(u.totalDraws).append("D-")
                        .append(u.totalLosses).append("L")
                        .append("):").append(presence).append(":").append(activity)
                        .append(",");
            }
        } catch (Exception e) {
            // Fallback: chỉ hiển thị online nếu DB lỗi
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
        }

        if (userListMessage.length() > "ONLINE_LIST:".length()) {
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
                    .append(user.totalLosses).append("L)");
            leaderboardMessage.append(";;");
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

            // Lưu mapping session cho việc dọn dẹp sau này
            sessionsByUser.put(challengerName, gameSession);
            sessionsByUser.put(opponentName, gameSession);

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
            ClientHandler p1 = session.getPlayer1();
            ClientHandler p2 = session.getPlayer2();

            if (p1 != null && onlineUsers.containsKey(p1.getUsername())) {
                p1.setInGame(false, null);
                sessionsByUser.remove(p1.getUsername());
            }
            if (p2 != null && onlineUsers.containsKey(p2.getUsername())) {
                p2.setInGame(false, null);
                sessionsByUser.remove(p2.getUsername());
            }

            System.out.println("A game session has ended.");

            broadcastOnlineList();
            broadcastLeaderboard();
        } catch (Exception e) {
            System.out.println("Error when ending game session: " + e.getMessage());
        }
    }

    // Dọn dẹp session “mồ côi”, tránh kẹt BUSY khi 1 bên out hoặc session mất tham chiếu
    private void cleanupStaleSessions() {
        try {
            // Duyệt theo user đang map session
            for (Map.Entry<String, GameSession> entry : sessionsByUser.entrySet()) {
                String uname = entry.getKey();
                GameSession s = entry.getValue();
                if (s == null) {
                    sessionsByUser.remove(uname);
                    continue;
                }
                ClientHandler p1 = s.getPlayer1();
                ClientHandler p2 = s.getPlayer2();

                boolean p1Online = p1 != null && onlineUsers.getOrDefault(p1.getUsername(), null) == p1;
                boolean p2Online = p2 != null && onlineUsers.getOrDefault(p2.getUsername(), null) == p2;

                // Nếu một trong hai offline hoặc mất tham chiếu -> reset cả hai
                if (!p1Online || !p2Online || p1 == null || p2 == null) {
                    if (p1 != null) {
                        p1.setInGame(false, null);
                        sessionsByUser.remove(p1.getUsername());
                    }
                    if (p2 != null) {
                        p2.setInGame(false, null);
                        sessionsByUser.remove(p2.getUsername());
                    }
                }
            }
        } catch (Exception ignored) {
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

