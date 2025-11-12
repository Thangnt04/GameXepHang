# Game Xếp Đơn Hàng Siêu Thị 🛒

[![Java](https://img.shields.io/badge/Java-8+-orange.svg)](https://www.java.com/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0+-blue.svg)](https://www.mysql.com/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

Game multiplayer xếp hàng thời gian thực sử dụng Java Swing + Socket + MySQL.

![Game Screenshot](docs/images/screenshot.png) *(Thêm ảnh nếu có)*

## ✨ Tính năng

- ✅ Đăng ký/Đăng nhập tài khoản
- ✅ Lobby hiển thị người chơi online/offline
- ✅ Thách đấu 1v1 realtime
- ✅ Game xếp đơn hàng (5 đơn, 60 giây)
- ✅ Bảng xếp hạng theo điểm (3 điểm thắng, 1 điểm hòa)
- ✅ Lịch sử đấu chi tiết
- ✅ Chơi lại sau mỗi trận

## 🛠 Công nghệ

- **Language:** Java 8+
- **GUI:** Swing
- **Network:** Socket TCP
- **Database:** MySQL 8.0
- **Pattern:** MVC + Repository + Service

## 📁 Cấu trúc Project

```
GameXepHang_test2/
├─ src/
│  ├─ controllers/      # Điều phối logic
│  ├─ models/           # Dữ liệu miền
│  ├─ views/            # Giao diện UI
│  ├─ services/         # Nghiệp vụ
│  └─ repositories/     # Truy cập database
├─ database/            # SQL schema
├─ docs/                # Tài liệu
└─ tools/               # Script tiện ích
```

## 🚀 Quick Start

### 1. Clone Repository

```bash
git clone https://github.com/YOUR_USERNAME/game-xep-hang.git
cd game-xep-hang
```

### 2. Setup Database

```bash
mysql -u root -p < database/schema.sql
```

### 3. Cấu hình

Sửa `src/repositories/DatabaseConfig.java`:

```java
public static final String DB_PASSWORD = "YOUR_PASSWORD"; // Đổi mật khẩu
```

### 4. Compile & Run

```bash
# Compile
javac -d bin -sourcepath src src/controllers/*.java src/models/*.java src/views/*.java src/services/*.java src/repositories/*.java

# Run Server
java -cp bin controllers.ServerController

# Run Client (terminal mới)
java -cp bin controllers.GameClientController
```

## 📖 Tài liệu

- [Setup Instructions](SETUP.md)
- [Run Guide](docs/RUN_GUIDE.md)
- [Git Guide](docs/GIT_GUIDE.md)
- [MVC Structure](docs/STRUCTURE.md)
- [Migration Guide](docs/MIGRATION_GUIDE.md)

## 🎮 Cách chơi

1. **Đăng ký** tài khoản mới
2. **Đăng nhập** vào lobby
3. **Chọn người chơi** trong danh sách và click "Thách đấu"
4. **Đối thủ chấp nhận** → Game bắt đầu
5. **Xếp hàng** theo đúng thứ tự đơn hàng trong 60 giây
6. **Hoàn thành 5 đơn** để thắng hoặc có nhiều đơn hơn khi hết giờ

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork repository
2. Tạo branch (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Open Pull Request

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👥 Authors

- **Your Name** - *Initial work* - [YourGitHub](https://github.com/YOUR_USERNAME)

## 🙏 Acknowledgments

- Java Swing Documentation
- MySQL Community
- Stack Overflow Community
