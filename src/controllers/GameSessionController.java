package controllers;

import models.MatchModel;
import services.GameSessionService;
import repositories.MatchRepository;
import repositories.UserRepository;
import java.util.Timer;
import java.util.TimerTask;

// Điều phối trận đấu, timer, kết quả, chơi lại/thoát/bỏ cuộc.
public class GameSessionController {
    private ClientController player1;
    private ClientController player2;
    private ServerController server;
    private MatchRepository matchRepository;

    private MatchModel matchModel;
    private GameSessionService gameService;

    private volatile boolean sessionEnded = false;
    private Timer matchTimer;
    private long matchStartTime;

    public GameSessionController(ClientController p1, ClientController p2, ServerController server, MatchRepository matchRepository) {
        this.player1 = p1;
        this.player2 = p2;
        this.server = server;
        this.matchRepository = matchRepository;
        this.matchModel = new MatchModel();
        this.gameService = new GameSessionService();
        System.out.println("Creating GameSession for: " + p1.getUsername() + " and " + p2.getUsername());
    }

    // Bắt đầu một trận đấu mới giữa hai người chơi
    public synchronized void startMatch() {
        matchModel.reset();
        gameService.generateOrders(matchModel);

        String opponent1 = player2.getUsername();
        String opponent2 = player1.getUsername();

        player1.sendMessage(String.format("GAME_START:%s:%d", opponent1, MatchModel.getMatchTimeSeconds()));
        player2.sendMessage(String.format("GAME_START:%s:%d", opponent2, MatchModel.getMatchTimeSeconds()));

        sendNewOrderToPlayer(player1, 0);
        sendNewOrderToPlayer(player2, 0);

        matchStartTime = System.currentTimeMillis();
        System.out.println("New match started for " + opponent2 + " vs " + opponent1);

        startMatchTimer();
    }

    // Gửi đơn hàng/kệ tương ứng cho một người chơi
    private void sendNewOrderToPlayer(ClientController player, int orderIndex) {
        if (orderIndex >= MatchModel.getNumOrdersPerMatch()) return;

        String orderCsv = String.join(",", matchModel.getAllOrders().get(orderIndex));
        String shelfCsv = String.join(",", matchModel.getAllShelves().get(orderIndex));

        player.sendMessage(String.format("NEW_ORDER:%d:%s:%s", orderIndex, orderCsv, shelfCsv));
    }

    // Khởi tạo bộ đếm thời gian trận
    private void startMatchTimer() {
        if (matchTimer != null) matchTimer.cancel();
        matchTimer = new Timer();
        matchTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                forceEndMatch();
            }
        }, MatchModel.getMatchTimeSeconds() * 1000 + 1000);
    }

    // kết thúc trận khi hết giờ hoặc cần finalize
    private synchronized void forceEndMatch() {
        if (matchModel.isMatchEnded()) return;
        matchModel.setMatchEnded(true);
        if (matchTimer != null) matchTimer.cancel();

        System.out.println("Match finalizing. Calculating results...");

        int p1Progress = matchModel.getPlayer1Progress();
        int p2Progress = matchModel.getPlayer2Progress();
        boolean p1Finished = (p1Progress == MatchModel.getNumOrdersPerMatch());
        boolean p2Finished = (p2Progress == MatchModel.getNumOrdersPerMatch());

        String result = gameService.calculateMatchResult(p1Progress, p2Progress, p1Finished, p2Finished);
        
        String p1Result, p2Result, p1Msg, p2Msg;
        
        if (result.equals("P1_WIN")) {
            p1Result = "WIN"; p2Result = "LOSS";
            p1Msg = p1Finished ? "Thắng! (Hoàn thành 5/5 đơn trước)" : "Thắng! (Hết giờ, nhiều đơn hơn)";
            p2Msg = p2Finished ? "Thua! (Đối thủ xong 5/5 đơn)" : "Thua! (Hết giờ, ít đơn hơn)";
        } else if (result.equals("P2_WIN")) {
            p1Result = "LOSS"; p2Result = "WIN";
            p1Msg = p1Finished ? "Thua! (Đối thủ xong 5/5 đơn)" : "Thua! (Hết giờ, ít đơn hơn)";
            p2Msg = p2Finished ? "Thắng! (Hoàn thành 5/5 đơn trước)" : "Thắng! (Hết giờ, nhiều đơn hơn)";
        } else {
            p1Result = p2Result = "DRAW";
            p1Msg = p2Msg = "Hòa! (Hết giờ, số đơn bằng nhau)";
        }

        matchRepository.updateMatchResult(
                player1.getUserId(), player2.getUserId(),
                p1Result, p2Result,
                (p1Result.equals("WIN")), (p2Result.equals("WIN")),
                p1Progress, p2Progress
        );

        updateLocalStats();

        player1.sendMessage(String.format("ROUND_RESULT:%s:%s (%d/5)", p1Result, p1Msg, p1Progress));
        player2.sendMessage(String.format("ROUND_RESULT:%s:%s (%d/5)", p2Result, p2Msg, p2Progress));
    }

    // Đồng bộ thống kê thắng/thua/hòa mới nhất từ DB vào phiên client
    private void updateLocalStats() {
        UserRepository.User p1Stats = server.getUserRepository().getUserStats(player1.getUsername());
        UserRepository.User p2Stats = server.getUserRepository().getUserStats(player2.getUsername());
        if (p1Stats != null) player1.getSession().updateStats(p1Stats.totalWins, p1Stats.totalDraws, p1Stats.totalLosses);
        if (p2Stats != null) player2.getSession().updateStats(p2Stats.totalWins, p2Stats.totalDraws, p2Stats.totalLosses);
    }

    // Xử lý nộp kết quả đơn hàng của một người chơi
    public synchronized void receivePlayerSubmission(ClientController player, String submissionCsv) {
        if (matchModel.isMatchEnded()) return;

        int currentProgress;
        ClientController opponent;

        if (player == player1) {
            currentProgress = matchModel.getPlayer1Progress();
            opponent = player2;
        } else {
            currentProgress = matchModel.getPlayer2Progress();
            opponent = player1;
        }

        if (currentProgress >= MatchModel.getNumOrdersPerMatch()) return;

        String correctOrderCsv = String.join(",", matchModel.getAllOrders().get(currentProgress));

        if (gameService.validateSubmission(submissionCsv, correctOrderCsv)) {
            currentProgress++;

            if (player == player1) matchModel.setPlayer1Progress(currentProgress);
            else matchModel.setPlayer2Progress(currentProgress);

            player.sendMessage("UPDATE_PROGRESS:" + currentProgress);
            opponent.sendMessage("OPPONENT_PROGRESS:" + currentProgress);

            //Nếu làm xong đủ số đơn -> Win
            if (currentProgress == MatchModel.getNumOrdersPerMatch()) {
                System.out.println(player.getUsername() + " finished 5/5.");
                forceEndMatch();
            } else {
                sendNewOrderToPlayer(player, currentProgress);
            }
        } else {
            player.sendMessage("SUBMIT_FAIL:Đơn hàng sai! Vui lòng xếp lại đơn hiện tại.");
        }
    }

    // Xử lý yêu cầu chơi lại sau khi kết thúc một vòng
    public synchronized void playerWantsToPlayAgain(ClientController player) {
        if (player == player1) {
            matchModel.setPlayer1WantsToPlayAgain(true);
            player1.sendMessage("SERVER_MSG:Đã gửi yêu cầu chơi tiếp. Đang chờ đối thủ...");
        } else if (player == player2) {
            matchModel.setPlayer2WantsToPlayAgain(true);
            player2.sendMessage("SERVER_MSG:Đã gửi yêu cầu chơi tiếp. Đang chờ đối thủ...");
        }

        if (matchModel.isPlayer1WantsToPlayAgain() && matchModel.isPlayer2WantsToPlayAgain()) {
            System.out.println("Both players agreed to play again. Starting new match.");
            startMatch();
        } else if (matchModel.isPlayer1WantsToPlayAgain()) {
            player2.sendMessage("PLAY_AGAIN_REQUEST");
        } else if (matchModel.isPlayer2WantsToPlayAgain()) {
            player1.sendMessage("PLAY_AGAIN_REQUEST");
        }
    }

    // Xử lý trường hợp người chơi bỏ cuộc giữa chừng
    public synchronized void playerForfeited(ClientController forfeitingPlayer) {
        if (matchModel.isMatchEnded() || sessionEnded) return;

        matchModel.setMatchEnded(true);
        sessionEnded = true;

        if (matchTimer != null) matchTimer.cancel();

        ClientController winningPlayer = (forfeitingPlayer == player1) ? player2 : player1;

        if (winningPlayer == null) {
            server.gameSessionEnded(this);
            return;
        }

        System.out.println(forfeitingPlayer.getUsername() + " đã bỏ cuộc. " + winningPlayer.getUsername() + " thắng.");

        String p1Result, p2Result, p1Msg, p2Msg;

        if (forfeitingPlayer == player1) {
            p1Result = "LOSS"; p2Result = "WIN";
            p1Msg = "Bạn đã bỏ cuộc!";
            p2Msg = "Thắng! (Đối thủ bỏ cuộc)";
        } else {
            p1Result = "WIN"; p2Result = "LOSS";
            p1Msg = "Thắng! (Đối thủ bỏ cuộc)";
            p2Msg = "Bạn đã bỏ cuộc!";
        }

        matchRepository.updateMatchResult(
                player1.getUserId(), player2.getUserId(),
                p1Result, p2Result,
                (p1Result.equals("WIN")), (p2Result.equals("WIN")),
                matchModel.getPlayer1Progress(), matchModel.getPlayer2Progress()
        );

        updateLocalStats();

        if (player1 != null) player1.sendMessage("OPPONENT_EXITED:" + p1Msg);
        if (player2 != null) player2.sendMessage("OPPONENT_EXITED:" + p2Msg);

        server.gameSessionEnded(this);
    }

    // Xử lý người chơi rời trận
    public synchronized void playerExited(ClientController player) {
        if (sessionEnded) return;
        sessionEnded = true;

        if (matchTimer != null) matchTimer.cancel();

        try {
            if (player == player1 && player2 != null) {
                player2.sendMessage("OPPONENT_EXITED:Đối thủ đã thoát khỏi trận đấu.");
            } else if (player == player2 && player1 != null) {
                player1.sendMessage("OPPONENT_EXITED:Đối thủ đã thoát khỏi trận đấu.");
            }
        } catch (Exception e) {
            System.out.println("Error sending opponent exited notification: " + e.getMessage());
        }

        server.gameSessionEnded(this);
    }

    // Getter cho hai người chơi trong phiên
    public ClientController getPlayer1() { return player1; }
    public ClientController getPlayer2() { return player2; }
}
