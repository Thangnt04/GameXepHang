# Hướng dẫn Refactor chi tiết

## Các file đã tạo mới

### Models (src/models/)
- `ClientSession.java` - Lưu trạng thái client
- `MatchModel.java` - Lưu trạng thái trận đấu
- `GameState.java` - Lưu trạng thái game phía client

### Services (src/services/)
- `AuthService.java` - Xử lý đăng nhập/đăng ký
- `GameSessionService.java` - Logic nghiệp vụ trận đấu

### Repositories (src/repositories/)
- `UserRepository.java` - Truy cập dữ liệu User
- `MatchRepository.java` - Truy cập dữ liệu Match

## Các bước refactor tiếp theo

### 1. Refactor ClientHandler.java
- Tạo `controllers/ClientController.java`
- Di chuyển logic xử lý message → ClientController
- Di chuyển logic auth → AuthService
- Sử dụng ClientSession model thay vì các biến riêng lẻ

### 2. Refactor GameClient.java
Tách thành:
- `controllers/GameClientController.java` - Điều phối logic
- `views/LoginView.java` - UI đăng nhập
- `views/LobbyView.java` - UI lobby
- `views/GameView.java` - UI game
- `views/HistoryView.java` - UI lịch sử
- Sử dụng GameState model

### 3. Refactor Server.java
- Tạo `controllers/ServerController.java`
- Tạo `services/LobbyService.java` - Logic lobby/leaderboard
- Giữ Server.java làm entry point, gọi controller

### 4. Refactor GameSession.java
- Sử dụng GameSessionService
- Sử dụng MatchModel
- Giữ GameSession làm wrapper, delegate logic sang service

### 5. Xóa DatabaseDAO.java
- Thay thế bằng UserRepository + MatchRepository
- Cập nhật tất cả import

## Lợi ích sau refactor
- Tách biệt trách nhiệm rõ ràng
- Dễ test từng thành phần
- Dễ maintain và mở rộng
- Code ngắn gọn, dễ đọc hơn
