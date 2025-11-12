# Setup Instructions

## 1. Clone Repository

```bash
git clone <your-repo-url>
cd GameXepHang_test2
```

## 2. Setup Database

### Yêu cầu
- MySQL 8.0 trở lên
- MySQL Connector/J (JDBC Driver)

### Cài đặt

1. Chạy MySQL Server
2. Import schema:

```bash
mysql -u root -p < database/schema.sql
```

Hoặc chạy thủ công trong MySQL Workbench/CLI.

## 3. Cấu hình Database

Sửa file `src/repositories/DatabaseConfig.java`:

```java
public static final String DB_PASSWORD = "YOUR_PASSWORD_HERE"; // Đổi mật khẩu
```

## 4. Compile

```bash
# Tạo thư mục bin
mkdir bin

# Compile tất cả file
javac -d bin -sourcepath src src/controllers/*.java src/models/*.java src/views/*.java src/services/*.java src/repositories/*.java
```

## 5. Run

### Chạy Server
```bash
java -cp bin controllers.ServerController
```

### Chạy Client
```bash
java -cp bin controllers.GameClientController
```

## 6. Test

1. Đăng ký 2 tài khoản
2. Đăng nhập 2 client
3. Thách đấu và chơi game

## Troubleshooting

### MySQL Connection Error
- Kiểm tra MySQL đang chạy
- Kiểm tra thông tin trong `DatabaseConfig.java`
- Kiểm tra database `gamexephang` đã được tạo

### Compilation Error
- Đảm bảo JDK 8+ đã được cài
- Kiểm tra cấu trúc thư mục `src/`

### Port Already in Use
- Đổi PORT trong `ServerController.java`
- Hoặc kill process cũ
