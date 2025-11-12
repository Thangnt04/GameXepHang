-- Database Schema for Game Xếp Hàng

CREATE DATABASE IF NOT EXISTS gamexephang;
USE gamexephang;

-- Table: users
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table: player_stats
CREATE TABLE IF NOT EXISTS player_stats (
    user_id INT PRIMARY KEY,
    total_wins INT DEFAULT 0,
    total_draws INT DEFAULT 0,
    total_losses INT DEFAULT 0,
    total_matches INT DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Table: match_history
CREATE TABLE IF NOT EXISTS match_history (
    id INT AUTO_INCREMENT PRIMARY KEY,
    player1_id INT NOT NULL,
    player2_id INT NOT NULL,
    player1_result VARCHAR(10),
    player2_result VARCHAR(10),
    player1_correct BOOLEAN,
    player2_correct BOOLEAN,
    player1_time_ms BIGINT,
    player2_time_ms BIGINT,
    played_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (player1_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (player2_id) REFERENCES users(id) ON DELETE CASCADE
);

-- View: leaderboard
CREATE OR REPLACE VIEW leaderboard AS
SELECT 
    u.id,
    u.username,
    COALESCE(ps.total_wins, 0) AS total_wins,
    COALESCE(ps.total_draws, 0) AS total_draws,
    COALESCE(ps.total_losses, 0) AS total_losses,
    COALESCE(ps.total_matches, 0) AS total_matches,
    (COALESCE(ps.total_wins, 0) * 3 + COALESCE(ps.total_draws, 0)) AS points
FROM users u
LEFT JOIN player_stats ps ON u.id = ps.user_id
ORDER BY points DESC, total_wins DESC, username ASC;

-- Trigger: Tự động tạo player_stats khi user mới đăng ký
DELIMITER $$
CREATE TRIGGER after_user_insert
AFTER INSERT ON users
FOR EACH ROW
BEGIN
    INSERT INTO player_stats (user_id) VALUES (NEW.id);
END$$
DELIMITER ;
