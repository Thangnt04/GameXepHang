package repositories;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

//Đăng ký, đăng nhập, lấy bảng xếp hạng, thống kê người dùng.
public class UserRepository {
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
            DatabaseConfig.DB_URL, 
            DatabaseConfig.DB_USER, 
            DatabaseConfig.DB_PASSWORD
        );
    }

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
            return totalWins * 3 + totalDraws;
        }
    }

    public boolean register(String username, String password) {
        // Tạo tài khoản mới
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

    public User login(String username, String password) {
        // Đăng nhập và lấy thống kê kèm theo
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

    public List<User> getLeaderboard() {
        // Lấy bảng xếp hạng (view/table tổng hợp)
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

    public User getUserStats(String username) {
        // Lấy thống kê chi tiết của một user
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
