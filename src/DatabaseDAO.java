import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseDAO {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/gamexephang";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "thangdc2004";

    // Inner class cho User với stats mới
    public static class User {
        public int id;
        public String username;
        public int totalWins;
        public int totalDraws;
        public int totalLosses;
        public int totalMatches;

        public User(int id, String username, int wins, int draws, int losses, int matches) {
            this.id = id;
            this.username = username;
            this.totalWins = wins;
            this.totalDraws = draws;
            this.totalLosses = losses;
            this.totalMatches = matches;
        }

        public int getPoints() {
            return totalWins * 3 + totalDraws; // 3 điểm thắng, 1 điểm hòa
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    // Đăng ký user mới
    public boolean register(String username, String password) {
        String sql = "INSERT INTO users (username, password) VALUES (?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Registration error: " + e.getMessage());
            return false;
        }
    }

    // Đăng nhập
    public User login(String username, String password) {
        String sql = "SELECT u.id, u.username, " +
                "COALESCE(ps.total_wins, 0) as total_wins, " +
                "COALESCE(ps.total_draws, 0) as total_draws, " +
                "COALESCE(ps.total_losses, 0) as total_losses, " +
                "COALESCE(ps.total_matches, 0) as total_matches " +
                "FROM users u " +
                "LEFT JOIN player_stats ps ON u.id = ps.user_id " +
                "WHERE u.username = ? AND u.password = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new User(
                        rs.getInt("id"),

                        rs.getString("username"),
                        rs.getInt("total_wins"),
                        rs.getInt("total_draws"),
                        rs.getInt("total_losses"),
                        rs.getInt("total_matches")
                );
            }
        } catch (SQLException e) {
            System.out.println("Login error: " + e.getMessage());
        }
        return null;
    }

    // Cập nhật kết quả trận đấu
    public void updateMatchResult(int player1Id, int player2Id,
                                  String p1Result, String p2Result,
                                  boolean p1Correct, boolean p2Correct,
                                  long p1TimeMs, long p2TimeMs) {
        Connection conn = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            // Lưu lịch sử trận đấu
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

            //  Cập nhật stats cho player 1
            updatePlayerStats(conn, player1Id, p1Result);

            //  Cập nhật stats cho player 2
            updatePlayerStats(conn, player2Id, p2Result);

            conn.commit();
            System.out.println("Match result updated successfully.");
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

    // Helper method: Cập nhật stats cho một người chơi
    private void updatePlayerStats(Connection conn, int userId, String result) throws SQLException {
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

    // Lấy bảng xếp hạng
    public List<User> getLeaderboard() {
        List<User> leaderboard = new ArrayList<>();
        String sql = "SELECT * FROM leaderboard LIMIT 100";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                leaderboard.add(new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getInt("total_wins"),
                        rs.getInt("total_draws"),
                        rs.getInt("total_losses"),
                        rs.getInt("total_matches")
                ));
            }
        } catch (SQLException e) {
            System.out.println("Error fetching leaderboard: " + e.getMessage());
        }
        return leaderboard;
    }

    // Lấy stats của một người chơi
    public User getUserStats(String username) {
        String sql = "SELECT u.id, u.username, ps.total_wins, ps.total_draws, ps.total_losses, ps.total_matches " +
                "FROM users u " +
                "JOIN player_stats ps ON u.id = ps.user_id " +
                "WHERE u.username = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getInt("total_wins"),
                        rs.getInt("total_draws"),
                        rs.getInt("total_losses"),
                        rs.getInt("total_matches")
                );
            }
        } catch (SQLException e) {
            System.out.println("Error fetching user stats: " + e.getMessage());
        }
        return null;
    }
}
