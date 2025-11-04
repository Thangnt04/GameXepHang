-- Tạo database + user kết nối (chạy an toàn nhiều lần)
CREATE DATABASE IF NOT EXISTS gamexephang CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'user'@'localhost' IDENTIFIED BY 'user';
GRANT ALL PRIVILEGES ON gamexephang.* TO 'user'@'localhost';
FLUSH PRIVILEGES;
USE gamexephang;

-- Cho phép chạy lại script không lỗi
DROP VIEW IF EXISTS leaderboard;
DROP TRIGGER IF EXISTS create_player_stats_after_user_insert;
DROP TABLE IF EXISTS match_history;
DROP TABLE IF EXISTS player_stats;
DROP TABLE IF EXISTS users;

-- Database Schema cho Game Xếp Hàng Siêu Thị

-- Bảng người dùng
CREATE TABLE users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_username (username)
);

-- Bảng thống kê người chơi (thay vì lưu trong users)
CREATE TABLE player_stats (
    user_id INT PRIMARY KEY,
    total_wins INT DEFAULT 0,
    total_draws INT DEFAULT 0,
    total_losses INT DEFAULT 0,
    total_matches INT DEFAULT 0,
    last_online TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Bảng lịch sử trận đấu (chi tiết từng ván)
CREATE TABLE match_history (
    id INT PRIMARY KEY AUTO_INCREMENT,
    player1_id INT NOT NULL,
    player2_id INT NOT NULL,
    player1_result ENUM('WIN', 'DRAW', 'LOSS') NOT NULL,
    player2_result ENUM('WIN', 'DRAW', 'LOSS') NOT NULL,
    player1_correct BOOLEAN DEFAULT FALSE,
    player2_correct BOOLEAN DEFAULT FALSE,
    player1_time_ms BIGINT DEFAULT 0,
    player2_time_ms BIGINT DEFAULT 0,
    played_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (player1_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (player2_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_player1 (player1_id),
    INDEX idx_player2 (player2_id),
    INDEX idx_played_at (played_at)
);

-- View để dễ truy vấn bảng xếp hạng
-- (Sửa: tổng hợp từ match_history, luôn hiển thị tất cả users bằng LEFT JOIN)
DROP VIEW IF EXISTS leaderboard;
CREATE VIEW leaderboard AS
SELECT
    u.id,
    u.username,
    COALESCE(s.total_wins, 0) AS total_wins,
    COALESCE(s.total_draws, 0) AS total_draws,
    COALESCE(s.total_losses, 0) AS total_losses,
    COALESCE(s.total_matches, 0) AS total_matches,
    (COALESCE(s.total_wins, 0) * 3 + COALESCE(s.total_draws, 0)) AS points
FROM users u
LEFT JOIN (
    SELECT
        t.user_id,
        SUM(CASE WHEN t.result = 'WIN'  THEN 1 ELSE 0 END) AS total_wins,
        SUM(CASE WHEN t.result = 'DRAW' THEN 1 ELSE 0 END) AS total_draws,
        SUM(CASE WHEN t.result = 'LOSS' THEN 1 ELSE 0 END) AS total_losses,
        COUNT(*) AS total_matches
    FROM (
        SELECT player1_id AS user_id, player1_result AS result
        FROM match_history
        UNION ALL
        SELECT player2_id AS user_id, player2_result AS result
        FROM match_history
    ) t
    GROUP BY t.user_id
) s ON s.user_id = u.id
ORDER BY points DESC, total_wins DESC, total_matches ASC, u.username ASC;

-- View nhanh để hiển thị username + password (dùng trong Workbench)
DROP VIEW IF EXISTS players_credentials;
CREATE VIEW players_credentials AS
SELECT id, username, password, created_at
FROM users
ORDER BY username;

-- Trigger tự động tạo player_stats khi có user mới
DELIMITER //
CREATE TRIGGER create_player_stats_after_user_insert
AFTER INSERT ON users
FOR EACH ROW
BEGIN
    INSERT INTO player_stats (user_id) VALUES (NEW.id);
END//
DELIMITER ;

-- Dữ liệu mẫu (để test)
INSERT INTO users (username, password) VALUES 
    ('player1', 'pass1'),
    ('player2', 'pass2'),
    ('player3', 'pass3');
