package repositories;

/**
 * Cấu hình kết nối Database
 * LƯU Ý: Đổi thông tin này theo môi trường của bạn
 */
public class DatabaseConfig {
    public static final String DB_URL = "jdbc:mysql://localhost:3306/gamexephang";
    public static final String DB_USER = "root";
    public static final String DB_PASSWORD = "thangdc2004"; //

    // Kiểm tra kết nối có hợp lệ không
    public static boolean isConfigured() {
        return !DB_PASSWORD.equals("thangdc2004");
    }
}
