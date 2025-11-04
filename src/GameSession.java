import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Vai trò 1: Trọng tài (Server)
 * Lớp này quản lý logic của MỘT ván game giữa 2 người chơi.
 * Nó được tạo ra khi 2 người chơi đồng ý thách đấu.
 */
public class GameSession {

    // Danh sách các mặt hàng có thể có trong siêu thị
    private static final String[] ALL_ITEMS = {
            "Táo", "Chuối", "Cam", "Nho", "Sữa", "Bánh Mì", "Trứng", "Phô Mai", "Thịt Gà", "Cá"
    };

    private static final int ROUND_TIME_SECONDS = 60; // Thời gian một ván
    private static final int ORDER_SIZE = 5; // Số lượng mặt hàng trong 1 đơn

    private ClientHandler player1;
    private ClientHandler player2;
    private Server server; // Để gọi lại server khi game kết thúc

    // Thông tin ván chơi hiện tại
    private List<String> currentOrder; // Đơn hàng yêu cầu (đúng thứ tự)
    private List<String> currentShelf; // Kệ hàng (ngẫu nhiên)

    // Kết quả của ván
    private String player1Submission = null;
    private String player2Submission = null;
    private long player1SubmitTime = -1; // (ms)
    private long player2SubmitTime = -1; // (ms)
    private long roundStartTime;

    // Trạng thái chờ chơi lại
    private boolean player1WantsToPlayAgain = false;
    private boolean player2WantsToPlayAgain = false;
    private boolean roundEnded = false;

    private Timer roundTimer;

    private volatile boolean sessionEnded = false; // THÊM FLAG


    public GameSession(ClientHandler p1, ClientHandler p2, Server server) {
        this.player1 = p1;
        this.player2 = p2;
        this.server = server;
        System.out.println("Creating GameSession for: " + p1.getUsername() + " and " + p2.getUsername());
    }

    // Bắt đầu một ván mới (hoặc ván đầu tiên)
    public synchronized void startNewRound() {
        // Reset trạng thái ván
        player1Submission = null;
        player2Submission = null;
        player1SubmitTime = -1;
        player2SubmitTime = -1;
        player1WantsToPlayAgain = false;
        player2WantsToPlayAgain = false;
        roundEnded = false;

        // 1. Tạo đơn hàng (order)
        List<String> allItemsList = new ArrayList<>(Arrays.asList(ALL_ITEMS));
        Collections.shuffle(allItemsList);
        currentOrder = new ArrayList<>(allItemsList.subList(0, ORDER_SIZE));

        // 2. Tạo kệ hàng (shelf)
        // Kệ hàng phải chứa đủ các món trong đơn, và thêm vài món ngẫu nhiên
        currentShelf = new ArrayList<>(currentOrder);
        while (currentShelf.size() < ALL_ITEMS.length) {
            currentShelf.add(allItemsList.get(currentShelf.size()));
        }
        Collections.shuffle(currentShelf); // Xáo trộn kệ hàng

        // 3. Chuyển đổi sang chuỗi CSV để gửi
        String orderCsv = String.join(",", currentOrder);
        String shelfCsv = String.join(",", currentShelf);

        // 4. Gửi thông tin ván game cho cả 2 client
        String opponent1 = player2.getUsername();
        String opponent2 = player1.getUsername();

        String msg1 = String.format("GAME_START:%s:%s:%s:%d", opponent1, orderCsv, shelfCsv, ROUND_TIME_SECONDS);
        String msg2 = String.format("GAME_START:%s:%s:%s:%d", opponent2, orderCsv, shelfCsv, ROUND_TIME_SECONDS);

        player1.sendMessage(msg1);
        player2.sendMessage(msg2);

        // 5. Ghi lại thời gian bắt đầu
        roundStartTime = System.currentTimeMillis();
        System.out.println("New round started. Order: " + orderCsv);

        // 6. Khởi tạo timer tự động kết thúc ván
        startRoundTimer();
    }

    // Timer tự động kết thúc ván khi hết giờ
    private void startRoundTimer() {
        if (roundTimer != null) {
            roundTimer.cancel();
        }
        roundTimer = new Timer();
        roundTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                forceEndRound();
            }
        }, ROUND_TIME_SECONDS * 1000 + 1000); // Thêm 1s buffer
    }

    // Buộc kết thúc ván khi hết giờ
    private synchronized void forceEndRound() {
        if (roundEnded) return;

        // Người chưa nộp = timeout
        if (player1Submission == null) {
            player1Submission = "TIMEOUT";
            player1SubmitTime = ROUND_TIME_SECONDS * 1000;
        }
        if (player2Submission == null) {
            player2Submission = "TIMEOUT";
            player2SubmitTime = ROUND_TIME_SECONDS * 1000;
        }

        checkIfRoundEnded();
    }

    // Nhận kết quả xếp hàng từ một người chơi
    public synchronized void receivePlayerSubmission(ClientHandler player, String submissionCsv) {
        if (roundEnded) return;

        // Validation: kiểm tra submission hợp lệ
        if (!isValidSubmission(submissionCsv)) {
            player.sendMessage("SERVER_MSG:Kết quả không hợp lệ!");
            return;
        }

        long submitTime = System.currentTimeMillis() - roundStartTime;

        if (player == player1 && player1Submission == null) {
            player1Submission = submissionCsv;
            player1SubmitTime = submitTime;
            player.sendMessage("SERVER_MSG:Đã nhận kết quả. Đang chờ đối thủ...");
            player2.sendMessage("OPPONENT_SUBMITTED");
            
            System.out.println(player1.getUsername() + " submitted: " + submissionCsv + " (Time: " + submitTime + "ms)");

        } else if (player == player2 && player2Submission == null) {
            player2Submission = submissionCsv;
            player2SubmitTime = submitTime;
            player.sendMessage("SERVER_MSG:Đã nhận kết quả. Đang chờ đối thủ...");
            player1.sendMessage("OPPONENT_SUBMITTED");
            
            System.out.println(player2.getUsername() + " submitted: " + submissionCsv + " (Time: " + submitTime + "ms)");
        }

        checkIfRoundEnded();
    }

    // Validation: kiểm tra submission có hợp lệ không
    private boolean isValidSubmission(String submissionCsv) {
        if ("TIMEOUT".equals(submissionCsv)) {
            return true; // Cho phép TIMEOUT
        }

        String[] items = submissionCsv.split(",");
        if (items.length != ORDER_SIZE) {
            return false;
        }

        // Kiểm tra các item có nằm trong shelf không
        for (String item : items) {
            if (!currentShelf.contains(item.trim())) {
                return false;
            }
        }

        return true;
    }

    // Hàm này được gọi khi hết giờ (từ client) hoặc khi cả 2 đã nộp
    public synchronized void checkIfRoundEnded() {
        if (roundEnded) return;
        if (player1Submission == null || player2Submission == null) return;

        roundEnded = true;
        if (roundTimer != null) {
            roundTimer.cancel();
        }

        System.out.println("Both players submitted. Starting scoring...");

        String correctOrderCsv = String.join(",", currentOrder);
        boolean p1Timeout = "TIMEOUT".equals(player1Submission);
        boolean p2Timeout = "TIMEOUT".equals(player2Submission);
        boolean p1Correct = !p1Timeout && correctOrderCsv.equals(player1Submission);
        boolean p2Correct = !p2Timeout && correctOrderCsv.equals(player2Submission);

        double p1TimeSeconds = player1SubmitTime / 1000.0;
        double p2TimeSeconds = player2SubmitTime / 1000.0;

        // Xác định kết quả: WIN, DRAW, LOSS
        String p1Result, p2Result;
        String p1Msg, p2Msg;

        if (p1Timeout && p2Timeout) {
            p1Result = p2Result = "DRAW";
            p1Msg = p2Msg = "Hòa! (Cả 2 hết giờ)";
        } else if (p1Timeout) {
            p1Result = "LOSS";
            p2Result = "WIN";
            p1Msg = "Thua! (Hết giờ)";
            p2Msg = p2Correct ? "Thắng! (Đối thủ hết giờ)" : "Thắng! (Đối thủ hết giờ, bạn cũng sai nhưng vẫn thắng)";
        } else if (p2Timeout) {
            p1Result = "WIN";
            p2Result = "LOSS";
            p2Msg = "Thua! (Hết giờ)";
            p1Msg = p1Correct ? "Thắng! (Đối thủ hết giờ)" : "Thắng! (Đối thủ hết giờ, bạn cũng sai nhưng vẫn thắng)";
        } else if (p1Correct && p2Correct) {
            if (p1TimeSeconds < p2TimeSeconds) {
                p1Result = "WIN"; p2Result = "LOSS";
                p1Msg = "Thắng! (Nhanh hơn)";
                p2Msg = "Thua! (Chậm hơn)";
            } else if (p2TimeSeconds < p1TimeSeconds) {
                p1Result = "LOSS"; p2Result = "WIN";
                p1Msg = "Thua! (Chậm hơn)";
                p2Msg = "Thắng! (Nhanh hơn)";
            } else {
                p1Result = p2Result = "DRAW";
                p1Msg = p2Msg = "Hòa! (Cùng đúng và cùng thời gian)";
            }
        } else if (p1Correct && !p2Correct) {
            p1Result = "WIN"; p2Result = "LOSS";
            p1Msg = "Thắng! (Đối thủ sai)";
            p2Msg = "Thua! (Bạn xếp sai)";
        } else if (!p1Correct && p2Correct) {
            p1Result = "LOSS"; p2Result = "WIN";
            p1Msg = "Thua! (Bạn xếp sai)";
            p2Msg = "Thắng! (Đối thủ sai)";
        } else {
            p1Result = p2Result = "DRAW";
            p1Msg = p2Msg = "Hòa! (Cả 2 cùng sai)";
        }

        // Cập nhật database
        server.getDatabaseDAO().updateMatchResult(
            player1.getUserId(), player2.getUserId(),
            p1Result, p2Result,
            p1Correct, p2Correct,
            player1SubmitTime, player2SubmitTime
        );

        // Cập nhật stats local
        DatabaseDAO.User p1Stats = server.getDatabaseDAO().getUserStats(player1.getUsername());
        DatabaseDAO.User p2Stats = server.getDatabaseDAO().getUserStats(player2.getUsername());
        
        if (p1Stats != null) {
            player1.updateStats(p1Stats.totalWins, p1Stats.totalDraws, p1Stats.totalLosses);
        }
        if (p2Stats != null) {
            player2.updateStats(p2Stats.totalWins, p2Stats.totalDraws, p2Stats.totalLosses);
        }

        // Gửi kết quả - Format: ROUND_RESULT:result:message:your_time:opponent_time:correct
        String p1ResultMsg = String.format("ROUND_RESULT:%s:%s:%.2f:%.2f:%b",
                p1Result, p1Msg, p1TimeSeconds, p2TimeSeconds, p1Correct);
        String p2ResultMsg = String.format("ROUND_RESULT:%s:%s:%.2f:%.2f:%b",
                p2Result, p2Msg, p2TimeSeconds, p1TimeSeconds, p2Correct);

        player1.sendMessage(p1ResultMsg);
        player2.sendMessage(p2ResultMsg);

        System.out.println("Round ended. P1: " + p1Result + ", P2: " + p2Result);

        server.broadcastLeaderboard();
    }

    public synchronized void playerWantsToPlayAgain(ClientHandler player) {
        if (player == player1) {
            player1WantsToPlayAgain = true;
            player1.sendMessage("SERVER_MSG:Đã gửi yêu cầu chơi tiếp. Đang chờ đối thủ...");
        } else if (player == player2) {
            player2WantsToPlayAgain = true;
            player2.sendMessage("SERVER_MSG:Đã gửi yêu cầu chơi tiếp. Đang chờ đối thủ...");
        }

        // Nếu cả hai cùng muốn chơi lại -> Tạo ván mới
        if (player1WantsToPlayAgain && player2WantsToPlayAgain) {
            System.out.println("Both players agreed to play again. Starting new round.");
            startNewRound();
        }
        // Gửi yêu cầu cho người chơi kia
        else if (player1WantsToPlayAgain) {
            player2.sendMessage("PLAY_AGAIN_REQUEST");
        } else if (player2WantsToPlayAgain) {
            player1.sendMessage("PLAY_AGAIN_REQUEST");
        }
    }

    // Xử lý khi một người chơi thoát game - CẢI THIỆN
    public synchronized void playerExited(ClientHandler player) {
        if (sessionEnded) return; // Tránh xử lý nhiều lần
        sessionEnded = true;

        // Hủy timer nếu có
        if (roundTimer != null) {
            roundTimer.cancel();
            roundTimer = null;
        }

        // Lấy thông tin trước khi set null
        ClientHandler p1 = player1;
        ClientHandler p2 = player2;

        // Đánh dấu null ngay để tránh xử lý lại
        player1 = null;
        player2 = null;

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
