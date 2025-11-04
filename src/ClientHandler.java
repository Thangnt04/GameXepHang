import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;

/**
 * Vai trò 1: Backend Server & Socket
 * Lớp này chạy trên một Thread riêng, xử lý mọi giao tiếp với MỘT client.
 */
public class ClientHandler implements Runnable {

    private Socket clientSocket;
    private Server server;
    private DatabaseDAO databaseDAO;
    private PrintWriter out;
    private BufferedReader in;

    private String username;
    private int userId;
    private int totalWins;
    private int totalDraws;
    private int totalLosses;
    private int totalMatches;
    private boolean inGame = false;
    private GameSession currentGameSession = null;
    private volatile boolean isConnected = true;

    public ClientHandler(Socket socket, Server server, DatabaseDAO dao) {
        this.clientSocket = socket;
        this.server = server;
        this.databaseDAO = dao;
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
                System.out.println("Received from [" + (username != null ? username : "Not logged in") + "]: " + clientMessage);
                processMessage(clientMessage);
            }
        } catch (SocketException e) {
            System.out.println("Client " + (username != null ? username : "") + " disconnected.");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }

    // Xử lý thông điệp từ client
    private void processMessage(String message) {
        String[] parts = message.split(":", 2);
        String command = parts[0];

        // Chưa đăng nhập
        if (this.username == null) {
            if (command.equals("LOGIN")) {
                handleLogin(parts[1]);
            } else if (command.equals("REGISTER")) {
                handleRegister(parts[1]);
            }
            return;
        }

        // Đã đăng nhập
        switch (command) {
            case "CHALLENGE":
                // parts[1] là username của đối thủ
                String opponentName = parts[1];
                server.handleChallenge(this.username, opponentName);
                break;
            case "CHALLENGE_RESPONSE":
                // parts[1] là "challengerName:accept" hoặc "challengerName:reject"
                String[] responseParts = parts[1].split(":");
                String challengerName = responseParts[0];
                boolean accepted = responseParts[1].equals("accept");
                server.handleChallengeResponse(challengerName, this.username, accepted);
                break;
            case "SUBMIT_ORDER":
                // parts[1] là chuỗi đơn hàng đã xếp, ví dụ "Táo,Chuối,Cam"
                if (inGame && currentGameSession != null) {
                    currentGameSession.receivePlayerSubmission(this, parts[1]);
                }
                break;
            case "PLAY_AGAIN_REQUEST":
                if (inGame && currentGameSession != null) {
                    currentGameSession.playerWantsToPlayAgain(this);
                }
                break;
            case "EXIT_GAME":
                if (inGame && currentGameSession != null) {
                    currentGameSession.playerExited(this);
                }
                break;
            // (LOGOUT sẽ được xử lý khi client đóng socket)
        }
    }

    // Xử lý đăng nhập
    private void handleLogin(String credentials) {
        String[] parts = credentials.split(":");
        if (parts.length != 2) return;

        String user = parts[0];
        String pass = parts[1];

        DatabaseDAO.User dbUser = databaseDAO.login(user, pass);
        if (dbUser != null) {
            // SỬA LẠI - DÙNG METHOD THAY VÌ TRUY CẬP TRỰC TIẾP
            if (server.isUserOnline(user)) {
                sendMessage("LOGIN_FAIL:Tài khoản này đã được đăng nhập ở nơi khác.");
                return;
            }

            // Đăng nhập thành công
            this.username = dbUser.username;
            this.userId = dbUser.id;
            this.totalWins = dbUser.totalWins;
            this.totalDraws = dbUser.totalDraws;
            this.totalLosses = dbUser.totalLosses;
            this.totalMatches = dbUser.totalMatches;
            
            // Format: LOGIN_SUCCESS:wins:draws:losses:matches
            sendMessage(String.format("LOGIN_SUCCESS:%d:%d:%d:%d", 
                totalWins, totalDraws, totalLosses, totalMatches));
            server.userLoggedIn(this.username, this);
        } else {
            // Đăng nhập thất bại
            sendMessage("LOGIN_FAIL:Sai tên đăng nhập hoặc mật khẩu.");
        }
    }

    // Xử lý đăng ký
    private void handleRegister(String credentials) {
        String[] parts = credentials.split(":");
        if (parts.length != 2) return;

        String user = parts[0];
        String pass = parts[1];

        if (databaseDAO.register(user, pass)) {
            sendMessage("REGISTER_SUCCESS:Đăng ký thành công. Vui lòng đăng nhập.");
        } else {
            sendMessage("REGISTER_FAIL:Tên đăng nhập đã tồn tại.");
        }
    }

    // Gửi thông điệp tới client này - THÊM XỬ LÝ EXCEPTION
    public void sendMessage(String message) {
        if (out != null && isConnected) {
            try {
                out.println(message);
                if (out.checkError()) {
                    System.out.println("Error sending message to " + username);
                    isConnected = false;
                }
            } catch (Exception e) {
                System.out.println("Exception sending message to " + username + ": " + e.getMessage());
                isConnected = false;
            }
        }
    }

    // Dọn dẹp khi client ngắt kết nối - CẢI THIỆN
    private void cleanup() {
        isConnected = false;
        try {
            // Thông báo cho game session trước (nếu đang chơi)
            if (inGame && currentGameSession != null) {
                currentGameSession.playerExited(this);
                currentGameSession = null;
                inGame = false;
            }
            
            // Sau đó mới logout
            server.userLoggedOut(this.username);
            
            // Đóng các stream và socket
            if (in != null) in.close();
            if (out != null) out.close();
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            System.out.println("Error during cleanup for " + username + ": " + e.getMessage());
        }
    }

    // Getters
    public String getUsername() { return username; }
    public int getUserId() { return userId; }
    public int getTotalWins() { return totalWins; }
    public int getTotalDraws() { return totalDraws; }
    public int getTotalLosses() { return totalLosses; }
    public int getTotalMatches() { return totalMatches; }
    public boolean isInGame() { return inGame; }

    // Setters
    public void updateStats(int wins, int draws, int losses) {
        this.totalWins = wins;
        this.totalDraws = draws;
        this.totalLosses = losses;
        this.totalMatches = wins + draws + losses;
    }

    public void setInGame(boolean inGame, GameSession session) {
        this.inGame = inGame;
        this.currentGameSession = session;
    }
}


