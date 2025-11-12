# Hướng dẫn Chạy Project

## Bước 1: Dọn dẹp file cũ

**QUAN TRỌNG:** Backup hoặc xóa các file cũ để tránh xung đột:

```bash
# Di chuyển vào thư mục backup (hoặc xóa trực tiếp)
mkdir src\backup
move src\GameClient.java src\backup\
move src\Server.java src\backup\
move src\GameSession.java src\backup\
move src\DatabaseDAO.java src\backup\
move src\ClientHandler.java src\backup\
```

## Bước 2: Kiểm tra cấu trúc thư mục

Đảm bảo có đầy đủ:
```
src/
├─ controllers/
│  ├─ ClientController.java
│  ├─ ServerController.java
│  ├─ GameSessionController.java
│  └─ GameClientController.java
├─ models/
│  ├─ ClientSession.java
│  ├─ MatchModel.java
│  └─ GameState.java
├─ views/
│  ├─ LoginView.java
│  ├─ LobbyView.java
│  ├─ GameView.java
│  └─ HistoryView.java
├─ services/
│  ├─ AuthService.java
│  ├─ GameSessionService.java
│  └─ LobbyService.java
└─ repositories/
   ├─ UserRepository.java
   └─ MatchRepository.java
```

## Bước 3: Kiểm tra Database

1. Đảm bảo MySQL đang chạy
2. Kiểm tra thông tin kết nối trong:
   - `repositories/UserRepository.java`
   - `repositories/MatchRepository.java`

```java
private static final String DB_URL = "jdbc:mysql://localhost:3306/gamexephang";
private static final String DB_USER = "root";
private static final String DB_PASSWORD = "thangdc2004"; // ĐỔI MẬT KHẨU CỦA BẠN
```

## Bước 4: Compile

### Cách 1: Sử dụng Command Line

```bash
# Từ thư mục gốc của project
cd c:\Users\ASUS\Desktop\xep_hang_test\GameXepHang_test2

# Tạo thư mục output
mkdir bin

# Compile tất cả file Java
javac -d bin -sourcepath src src/controllers/*.java src/models/*.java src/views/*.java src/services/*.java src/repositories/*.java
```

### Cách 2: Sử dụng IDE (IntelliJ IDEA / Eclipse)

1. Mở project trong IDE
2. Mark thư mục `src` là Source Root
3. Build Project (Ctrl+F9 trong IntelliJ)

## Bước 5: Chạy Server

```bash
# Từ thư mục project
cd c:\Users\ASUS\Desktop\xep_hang_test\GameXepHang_test2

# Chạy Server
java -cp bin controllers.ServerController
```

**Kết quả mong đợi:**
```
Server is running and listening on port 12345...
```

## Bước 6: Chạy Client

Mở terminal/cmd MỚI:

```bash
# Từ thư mục project
cd c:\Users\ASUS\Desktop\xep_hang_test\GameXepHang_test2

# Chạy Client
java -cp bin controllers.GameClientController
```

**Kết quả:** Cửa sổ đăng nhập xuất hiện.

## Bước 7: Test

1. Tạo 2 tài khoản mới (hoặc dùng sẵn)
2. Đăng nhập 2 client khác nhau
3. Thách đấu nhau
4. Chơi game
5. Kiểm tra bảng xếp hạng và lịch sử

## Xử lý lỗi thường gặp

### Lỗi: `package X does not exist`
**Nguyên nhân:** Chưa compile đúng cấu trúc package  
**Giải pháp:** Dùng lệnh compile ở Bước 4

### Lỗi: `ClassNotFoundException: controllers.ServerController`
**Nguyên nhân:** Classpath không đúng  
**Giải pháp:** Chạy từ thư mục gốc và dùng `-cp bin`

### Lỗi: `Communications link failure`
**Nguyên nhân:** MySQL chưa chạy hoặc thông tin kết nối sai  
**Giải pháp:** 
- Kiểm tra MySQL đang chạy
- Kiểm tra DB_URL, DB_USER, DB_PASSWORD
- Kiểm tra database `gamexephang` đã tồn tại

### Lỗi: `Address already in use`
**Nguyên nhân:** Server đã chạy trước đó  
**Giải pháp:** Kill process cũ hoặc đổi PORT trong ServerController

### Lỗi: `Connection refused`
**Nguyên nhân:** Client chạy trước Server  
**Giải pháp:** Chạy Server trước, sau đó mới chạy Client

## Script tự động (Windows)

### start_server.bat

Tạo file `start_server.bat` trong thư mục gốc:

```batch
@echo off
cd /d %~dp0
echo Starting Server...
java -cp bin controllers.ServerController
pause
```

### start_client.bat

Tạo file `start_client.bat`:

```batch
@echo off
cd /d %~dp0
echo Starting Client...
java -cp bin controllers.GameClientController
pause
```

## Ghi chú

- Nếu dùng IDE, có thể chạy trực tiếp từ `main()` method
- Để test multiplayer, cần chạy ít nhất 2 client
- Server phải chạy trước khi client kết nối
- Không đóng terminal/cmd đang chạy Server khi đang test
