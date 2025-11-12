# Hướng dẫn Migration

## Thay đổi chính

### 1. File cần XÓA (đã được thay thế)
- `ClientHandler.java` → `controllers/ClientController.java`
- `GameSession.java` → `controllers/GameSessionController.java`
- `Server.java` → `controllers/ServerController.java`
- `DatabaseDAO.java` → `repositories/UserRepository.java` + `repositories/MatchRepository.java`
- `GameClient.java` → `controllers/GameClientController.java` + 4 Views

### 2. File MỚI cần THÊM
**Models:**
- `models/ClientSession.java`
- `models/MatchModel.java`
- `models/GameState.java`

**Controllers:**
- `controllers/ClientController.java`
- `controllers/ServerController.java`
- `controllers/GameSessionController.java`
- `controllers/GameClientController.java`

**Views:**
- `views/LoginView.java`
- `views/LobbyView.java`
- `views/GameView.java`
- `views/HistoryView.java`

**Services:**
- `services/AuthService.java`
- `services/GameSessionService.java`
- `services/LobbyService.java`

**Repositories:**
- `repositories/UserRepository.java`
- `repositories/MatchRepository.java`

### 3. Cách chạy sau migration

**Chạy Server:**
```bash
# Compile
javac -d bin -sourcepath src src/controllers/ServerController.java

# Run
java -cp bin controllers.ServerController
```

**Chạy Client:**
```bash
# Compile
javac -d bin -sourcepath src src/controllers/GameClientController.java

# Run
java -cp bin controllers.GameClientController
```

### 4. Checklist Migration

- [ ] Tạo tất cả thư mục: models, views, controllers, services, repositories
- [ ] Copy tất cả file mới vào đúng thư mục
- [ ] Xóa hoặc backup các file cũ (ClientHandler, GameSession, Server, DatabaseDAO, GameClient)
- [ ] Kiểm tra kết nối database trong UserRepository (DB_URL, DB_USER, DB_PASSWORD)
- [ ] Compile và test Server trước
- [ ] Compile và test Client sau
- [ ] Test các tính năng: Login, Lobby, Challenge, Game, History

### 5. Lỗi thường gặp

**Lỗi:** `package X does not exist`
**Sửa:** Đảm bảo cấu trúc thư mục đúng và compile từ thư mục gốc

**Lỗi:** `UserRepository.User cannot be resolved`
**Sửa:** Thêm import `repositories.UserRepository;`

**Lỗi:** Server không kết nối được DB
**Sửa:** Kiểm tra MySQL đang chạy và thông tin DB_URL, DB_USER, DB_PASSWORD trong UserRepository

**Lỗi:** Client không kết nối được Server
**Sửa:** Kiểm tra SERVER_ADDRESS và SERVER_PORT trong GameClientController
