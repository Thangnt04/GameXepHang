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
CREATE VIEW leaderboard AS
SELECT 
    u.id,
    u.username,
    ps.total_wins,
    ps.total_draws,
    ps.total_losses,
    ps.total_matches,
    (ps.total_wins * 3 + ps.total_draws) AS points
FROM users u
JOIN player_stats ps ON u.id = ps.user_id
ORDER BY points DESC, ps.total_wins DESC, ps.total_matches ASC;

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
