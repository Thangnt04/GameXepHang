import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Vai trò: Trọng tài (Server)
 * Lớp này quản lý logic của MỘT TRẬN ĐẤU (5 đơn hàng, 60 giây)
 */
public class GameSession {

    // Danh sách các mặt hàng có thể có trong siêu thị
    private static final String[] ALL_ITEMS = {
            "Táo", "Chuối", "Cam", "Nho", "Sữa", "Bánh Mì", "Trứng", "Phô Mai", "Thịt Gà", "Cá"
    };

    private static final int MATCH_TIME_SECONDS = 60;
    private static final int NUM_ORDERS_PER_MATCH = 5;
    private static final int ORDER_SIZE = 5; // Số lượng mặt hàng trong 1 đơn

    private ClientHandler player1;
    private ClientHandler player2;
    private Server server; // Để gọi lại server khi game kết thúc

    // Thông tin trận đấu
    private List<List<String>> allOrders = new ArrayList<>();
    private List<List<String>> allShelves = new ArrayList<>();

    // Tiến độ
    private int player1Progress = 0;
    private int player2Progress = 0;

    // Dùng để đánh dấu ai xong 5/5
    private String player1Submission = null;
    private String player2Submission = null;

    // Trạng thái chờ chơi lại
    private boolean player1WantsToPlayAgain = false;
    private boolean player2WantsToPlayAgain = false;

    // Đổi tên: roundEnded -> matchEnded
    private volatile boolean matchEnded = false;
    private volatile boolean sessionEnded = false;
    private Timer matchTimer;
    private long matchStartTime;


    public GameSession(ClientHandler p1, ClientHandler p2, Server server) {
        this.player1 = p1;
        this.player2 = p2;
        this.server = server;
        System.out.println("Creating GameSession for: " + p1.getUsername() + " and " + p2.getUsername());
    }

    // Đổi tên: Bắt đầu một trận đấu mới
    public synchronized void startMatch() {
        // Reset trạng thái trận
        player1Progress = 0;
        player2Progress = 0;
        player1Submission = null;
        player2Submission = null;
        allOrders.clear();
        allShelves.clear();

        player1WantsToPlayAgain = false;
        player2WantsToPlayAgain = false;
        matchEnded = false;

        //  Tạo 5 đơn hàng và 5 kệ hàng (giống nhau cho cả 2)
        for (int i = 0; i < NUM_ORDERS_PER_MATCH; i++) {
            List<String> allItemsList = new ArrayList<>(Arrays.asList(ALL_ITEMS));
            Collections.shuffle(allItemsList);
            List<String> order = new ArrayList<>(allItemsList.subList(0, ORDER_SIZE));

            List<String> shelf = new ArrayList<>(order);
            while (shelf.size() < ALL_ITEMS.length) {
                shelf.add(allItemsList.get(shelf.size()));
            }
            Collections.shuffle(shelf);

            allOrders.add(order);
            allShelves.add(shelf);
        }

        // Gửi lệnh bắt đầu trận đấu (chỉ gửi thời gian)
        String opponent1 = player2.getUsername();
        String opponent2 = player1.getUsername();

        // Format: GAME_START:OpponentName:MatchTimeSeconds
        player1.sendMessage(String.format("GAME_START:%s:%d", opponent1, MATCH_TIME_SECONDS));
        player2.sendMessage(String.format("GAME_START:%s:%d", opponent2, MATCH_TIME_SECONDS));

        // Gửi đơn hàng ĐẦU TIÊN (index 0)
        sendNewOrderToPlayer(player1, 0);
        sendNewOrderToPlayer(player2, 0);

        // Ghi lại thời gian bắt đầu
        matchStartTime = System.currentTimeMillis();
        System.out.println("New match (60s, 5 orders) started for " + opponent2 + " vs " + opponent1);

        // Khởi tạo timer cho TOÀN BỘ TRẬN ĐẤU
        startMatchTimer();
    }

    // Hàm helper: Gửi đơn hàng mới cho người chơi
    private void sendNewOrderToPlayer(ClientHandler player, int orderIndex) {
        if (orderIndex >= NUM_ORDERS_PER_MATCH) return; // Đã xong 5/5

        String orderCsv = String.join(",", allOrders.get(orderIndex));
        String shelfCsv = String.join(",", allShelves.get(orderIndex));

        // Format: NEW_ORDER:OrderIndex:OrderCsv:ShelfCsv
        player.sendMessage(String.format("NEW_ORDER:%d:%s:%s", orderIndex, orderCsv, shelfCsv));
    }


    // Timer tự động kết thúc TRẬN ĐẤU
    private void startMatchTimer() {
        if (matchTimer != null) {
            matchTimer.cancel();
        }
        matchTimer = new Timer();
        matchTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                forceEndMatch(); // Gọi hàm kết thúc
            }
        }, MATCH_TIME_SECONDS * 1000 + 1000); // Thêm 1s buffer
    }

    /**
     * Hàm này được gọi khi HẾT GIỜ (từ timer)
     * hoặc khi 1 người chơi hoàn thành 5/5 (từ receivePlayerSubmission).
     * Đây là nơi duy nhất quyết định thắng/thua/hòa.
     */
    private synchronized void forceEndMatch() {
        if (matchEnded) return; // Đã kết thúc rồi, không xử lý
        matchEnded = true;
        if (matchTimer != null) matchTimer.cancel();

        System.out.println("Match finalizing. Calculating results...");

        String p1Result, p2Result;
        String p1Msg, p2Msg;

        boolean p1Finished = (player1Progress == NUM_ORDERS_PER_MATCH);
        boolean p2Finished = (player2Progress == NUM_ORDERS_PER_MATCH);

        // Quy tắc 3: Thắng ngay lập tức
        if (p1Finished && !p2Finished) {
            p1Result = "WIN"; p2Result = "LOSS";
            p1Msg = "Thắng! (Hoàn thành 5/5 đơn trước)";
            p2Msg = "Thua! (Đối thủ xong 5/5 đơn)";
        } else if (!p1Finished && p2Finished) {
            p1Result = "LOSS"; p2Result = "WIN";
            p1Msg = "Thua! (Đối thủ xong 5/5 đơn)";
            p2Msg = "Thắng! (Hoàn thành 5/5 đơn trước)";
        }
        // Quy tắc 2 & 4: Hết giờ, so sánh
        else if (player1Progress > player2Progress) {
            p1Result = "WIN"; p2Result = "LOSS";
            p1Msg = "Thắng! (Hết giờ, nhiều đơn hơn)";
            p2Msg = "Thua! (Hết giờ, ít đơn hơn)";
        } else if (player2Progress > player1Progress) {
            p1Result = "LOSS"; p2Result = "WIN";
            p1Msg = "Thua! (Hết giờ, ít đơn hơn)";
            p2Msg = "Thắng! (Hết giờ, nhiều đơn hơn)";
        } else {
            // Bao gồm p1Progress == p2Progress VÀ p1Finished && p2Finished
            p1Result = p2Result = "DRAW";
            p1Msg = p2Msg = "Hòa! (Hết giờ, số đơn bằng nhau)";
        }

        // Cập nhật database
        server.getDatabaseDAO().updateMatchResult(
                player1.getUserId(), player2.getUserId(),
                p1Result, p2Result,
                (p1Result.equals("WIN")), (p2Result.equals("WIN")),
                player1Progress, player2Progress // Dùng time_ms để lưu số đơn
        );

        // Cập nhật stats local
        updateLocalStats();

        // Gửi kết quả - Format: ROUND_RESULT:result:message
        String p1ResultMsg = String.format("ROUND_RESULT:%s:%s (%d/5)", p1Result, p1Msg, player1Progress);
        String p2ResultMsg = String.format("ROUND_RESULT:%s:%s (%d/5)", p2Result, p2Msg, player2Progress);

        player1.sendMessage(p1ResultMsg);
        player2.sendMessage(p2ResultMsg);

        server.broadcastLeaderboard();
    }

    // Hàm helper mới
    private void updateLocalStats() {
        DatabaseDAO.User p1Stats = server.getDatabaseDAO().getUserStats(player1.getUsername());
        DatabaseDAO.User p2Stats = server.getDatabaseDAO().getUserStats(player2.getUsername());
        if (p1Stats != null) player1.updateStats(p1Stats.totalWins, p1Stats.totalDraws, p1Stats.totalLosses);
        if (p2Stats != null) player2.updateStats(p2Stats.totalWins, p2Stats.totalDraws, p2Stats.totalLosses);
    }


    // Logic nhận đơn hàng
    public synchronized void receivePlayerSubmission(ClientHandler player, String submissionCsv) {
        if (matchEnded) return;

        int currentProgress;
        ClientHandler opponent;

        if (player == player1) {
            currentProgress = player1Progress;
            opponent = player2;
        } else {
            currentProgress = player2Progress;
            opponent = player1;
        }

        // Nếu đã nộp xong 5 đơn, không xử lý
        if (currentProgress >= NUM_ORDERS_PER_MATCH) return;

        // 1. Lấy đơn hàng hiện tại
        String correctOrderCsv = String.join(",", allOrders.get(currentProgress));

        // 2. Kiểm tra
        if (correctOrderCsv.equals(submissionCsv)) {
            // --- ĐÚNG ---
            currentProgress++;

            // Cập nhật tiến độ
            if (player == player1) player1Progress = currentProgress;
            else player2Progress = currentProgress;

            // Gửi thông báo cập nhật tiến độ cho 2 bên
            player.sendMessage("UPDATE_PROGRESS:" + currentProgress);
            opponent.sendMessage("OPPONENT_PROGRESS:" + currentProgress);

            // 3. Gửi đơn hàng MỚI (nếu còn) HOẶC KẾT THÚC
            if (currentProgress == NUM_ORDERS_PER_MATCH) {
                // Hoàn thành 5/5 -> THẮNG NGAY LẬP TỨC
                System.out.println(player.getUsername() + " finished 5/5.");
                forceEndMatch(); // Gọi kết thúc trận ngay
            } else {
                // Vẫn còn, gửi đơn tiếp theo
                sendNewOrderToPlayer(player, currentProgress);
            }

        } else {
            // --- SAI ---
            player.sendMessage("SUBMIT_FAIL:Đơn hàng sai! Vui lòng xếp lại đơn hiện tại.");
        }
    }

    public synchronized void playerWantsToPlayAgain(ClientHandler player) {
        if (player == player1) {
            player1WantsToPlayAgain = true;
            player1.sendMessage("SERVER_MSG:Đã gửi yêu cầu chơi tiếp. Đang chờ đối thủ...");
        } else if (player == player2) {
            player2WantsToPlayAgain = true;
            player2.sendMessage("SERVER_MSG:Đã gửi yêu cầu chơi tiếp. Đang chờ đối thủ...");
        }

        // Nếu cả hai cùng muốn chơi lại -> Tạo trận MỚI
        if (player1WantsToPlayAgain && player2WantsToPlayAgain) {
            System.out.println("Both players agreed to play again. Starting new match.");
            startMatch(); // Đổi tên hàm
        }
        // Gửi yêu cầu cho người chơi kia
        else if (player1WantsToPlayAgain) {
            player2.sendMessage("PLAY_AGAIN_REQUEST");
        } else if (player2WantsToPlayAgain) {
            player1.sendMessage("PLAY_AGAIN_REQUEST");
        }
    }

    // Xử lý khi một người chơi chủ động bấm "Bỏ cuộc" (hoặc thoát game)
    public synchronized void playerForfeited(ClientHandler forfeitingPlayer) {
        if (matchEnded || sessionEnded) return; // Đã kết thúc rồi, không xử lý nữa

        matchEnded = true;
        sessionEnded = true;

        if (matchTimer != null) matchTimer.cancel();

        // Xác định người thắng cuộc
        ClientHandler winningPlayer = (forfeitingPlayer == player1) ? player2 : player1;

        if (winningPlayer == null) {
            server.gameSessionEnded(this);
            return;
        }

        System.out.println(forfeitingPlayer.getUsername() + " đã bỏ cuộc. " + winningPlayer.getUsername() + " thắng.");

        String p1Result, p2Result;
        String p1Msg, p2Msg;

        if (forfeitingPlayer == player1) {
            p1Result = "LOSS";
            p2Result = "WIN";
            p1Msg = "Bạn đã bỏ cuộc!";
            p2Msg = "Thắng! (Đối thủ bỏ cuộc)";
        } else {
            p1Result = "WIN";
            p2Result = "LOSS";
            p1Msg = "Thắng! (Đối thủ bỏ cuộc)";
            p2Msg = "Bạn đã bỏ cuộc!";
        }

        // Cập nhật CSDL
        server.getDatabaseDAO().updateMatchResult(
                player1.getUserId(), player2.getUserId(),
                p1Result, p2Result,
                (p1Result.equals("WIN")), (p2Result.equals("WIN")), // Gán người thắng là "correct"
                player1Progress, player2Progress // Ghi lại tiến độ lúc bỏ cuộc
        );

        // Cập nhật stats local
        updateLocalStats();

        // Gửi thông báo kết quả (sử dụng logic đã sửa)
        if (player1 != null) player1.sendMessage("OPPONENT_EXITED:" + p1Msg);
        if (player2 != null) player2.sendMessage("OPPONENT_EXITED:" + p2Msg);

        // Cập nhật BXH cho mọi người
        server.broadcastLeaderboard();

        // Dọn dẹp session
        server.gameSessionEnded(this);
    }

    // Xử lý khi một người chơi thoát (khi đang không trong game)
    public synchronized void playerExited(ClientHandler player) {
        if (sessionEnded) return; // Tránh xử lý nhiều lần
        sessionEnded = true;

        if (matchTimer != null) matchTimer.cancel();

        ClientHandler p1 = player1;
        ClientHandler p2 = player2;

        // Gửi thông báo cho đối thủ
        try {
            if (player == p1 && p2 != null) {
                p2.sendMessage("OPPONENT_EXITED:Đối thủ đã thoát khỏi trận đấu.");
            } else if (player == p2 && p1 != null) {
                p1.sendMessage("OPPONENT_EXITED:Đối thủ đã thoát khỏi trận đấu.");
            }
        } catch (Exception e) {
            System.out.println("Error sending opponent exited notification: " + e.getMessage());
        }

        // Báo cho Server biết để cập nhật trạng thái
        server.gameSessionEnded(this);
    }

    // Getters
    public ClientHandler getPlayer1() {
        return player1;
    }

    public ClientHandler getPlayer2() {
        return player2;
    }
}