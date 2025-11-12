package repositories;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

//Ghi lịch sử trận và cập nhật thống kê.
public class MatchRepository {
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
            DatabaseConfig.DB_URL, 
            DatabaseConfig.DB_USER, 
            DatabaseConfig.DB_PASSWORD
        );
    }

    public static class MatchHistory {
        public String opponentName;
        public String myResult;
        public long myScore;
        public long opponentScore;
        public Timestamp playedAt;

        public MatchHistory(String opponentName, String myResult, long myScore, long opponentScore, Timestamp playedAt) {
            this.opponentName = opponentName;
            this.myResult = myResult;
            this.myScore = myScore;
            this.opponentScore = opponentScore;
            this.playedAt = playedAt;
        }
    }

    public void updateMatchResult(int player1Id, int player2Id,
                                  String p1Result, String p2Result,
                                  boolean p1Correct, boolean p2Correct,
                                  long p1TimeMs, long p2TimeMs) {
        // Ghi lịch sử trận và cập nhật thống kê người chơi trong một giao dịch
        Connection conn = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            String insertHistory = "INSERT INTO match_history " +
                    "(player1_id, player2_id, player1_result, player2_result, " +
                    "player1_correct, player2_correct, player1_time_ms, player2_time_ms) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(insertHistory)) {
                stmt.setInt(1, player1Id);
                stmt.setInt(2, player2Id);
                stmt.setString(3, p1Result);
                stmt.setString(4, p2Result);
                stmt.setBoolean(5, p1Correct);
                stmt.setBoolean(6, p2Correct);
                stmt.setLong(7, p1TimeMs);
                stmt.setLong(8, p2TimeMs);
                stmt.executeUpdate();
            }

            updatePlayerStats(conn, player1Id, p1Result);
            updatePlayerStats(conn, player2Id, p2Result);

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) {}
            }
            System.out.println("Error updating match result: " + e.getMessage());
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException e) {}
            }
        }
    }

    private void updatePlayerStats(Connection conn, int userId, String result) throws SQLException {
        // Cộng dồn thống kê dựa trên kết quả (WIN/DRAW/LOSS)
        String updateStats = "UPDATE player_stats SET " +
                "total_matches = total_matches + 1, " +
                (result.equals("WIN") ? "total_wins = total_wins + 1" :
                 result.equals("DRAW") ? "total_draws = total_draws + 1" :
                 "total_losses = total_losses + 1") +
                " WHERE user_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(updateStats)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        }
    }

    public List<MatchHistory> getMatchHistory(String username) {
        //Lấy 20 trận gần nhất của một user (đối thủ, kết quả, điểm, thời gian)
        List<MatchHistory> historyList = new ArrayList<>();
        String sql = "SELECT " +
                "CASE WHEN mh.player1_id = u.id THEN u2.username ELSE u1.username END AS opponent_name, " +
                "CASE WHEN mh.player1_id = u.id THEN mh.player1_result ELSE mh.player2_result END AS my_result, " +
                "CASE WHEN mh.player1_id = u.id THEN mh.player1_time_ms ELSE mh.player2_time_ms END AS my_score, " +
                "CASE WHEN mh.player1_id = u.id THEN mh.player2_time_ms ELSE mh.player1_time_ms END AS opponent_score, " +
                "mh.played_at " +
                "FROM users u " +
                "JOIN match_history mh ON (mh.player1_id = u.id OR mh.player2_id = u.id) " +
                "JOIN users u1 ON mh.player1_id = u1.id " +
                "JOIN users u2 ON mh.player2_id = u2.id " +
                "WHERE u.username = ? ORDER BY mh.played_at DESC LIMIT 20";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                historyList.add(new MatchHistory(
                    rs.getString("opponent_name"),
                    rs.getString("my_result"),
                    rs.getLong("my_score"),
                    rs.getLong("opponent_score"),
                    rs.getTimestamp("played_at")
                ));
            }
        } catch (SQLException e) {
            System.out.println("Error fetching match history: " + e.getMessage());
        }
        return historyList;
    }
}
