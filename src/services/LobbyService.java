package services;

import controllers.ClientController;
import controllers.GameSessionController;
import repositories.UserRepository;
import java.util.List;
import java.util.Map;

public class LobbyService {
    private UserRepository userRepository;

    public LobbyService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void broadcastOnlineList(Map<String, ClientController> onlineUsers,
                                    Map<String, GameSessionController> sessionsByUser) {
        StringBuilder userListMessage = new StringBuilder("ONLINE_LIST:");
        try {
            List<UserRepository.User> allUsers = userRepository.getLeaderboard();

            allUsers.sort((u1, u2) -> {
                ClientController h1 = onlineUsers.get(u1.username);
                ClientController h2 = onlineUsers.get(u2.username);
                boolean online1 = (h1 != null);
                boolean online2 = (h2 != null);
                boolean busy1 = (online1 && h1.isInGame());
                boolean busy2 = (online2 && h2.isInGame());

                if (online1 && !online2) return -1;
                if (!online1 && online2) return 1;

                if (online1 && online2) {
                    if (!busy1 && busy2) return -1;
                    if (busy1 && !busy2) return 1;
                }

                return u1.username.compareToIgnoreCase(u2.username);
            });

            for (UserRepository.User u : allUsers) {
                ClientController h = onlineUsers.get(u.username);
                boolean online = (h != null);
                String presence = online ? "ONLINE" : "OFFLINE";
                String activity = (online && h.isInGame()) ? "BUSY" : "IDLE";
                userListMessage
                        .append(u.username)
                        .append("(").append(u.totalWins).append("W-")
                        .append(u.totalDraws).append("D-")
                        .append(u.totalLosses).append("L")
                        .append("):").append(presence).append(":").append(activity)
                        .append(",");
            }
        } catch (Exception e) {
            for (ClientController handler : onlineUsers.values()) {
                if (handler.getUsername() != null) {
                    String status = handler.isInGame() ? "BUSY" : "IDLE";
                    userListMessage.append(handler.getUsername())
                            .append("(")
                            .append(handler.getSession().getTotalWins()).append("W-")
                            .append(handler.getSession().getTotalDraws()).append("D-")
                            .append(handler.getSession().getTotalLosses()).append("L")
                            .append("):ONLINE:").append(status)
                            .append(",");
                }
            }
        }
        if (userListMessage.length() > "ONLINE_LIST:".length()) {
            userListMessage.deleteCharAt(userListMessage.length() - 1);
        }

        for (ClientController handler : onlineUsers.values()) {
            handler.sendMessage(userListMessage.toString());
        }
    }

    public void broadcastLeaderboard(Map<String, ClientController> onlineUsers) {
        List<UserRepository.User> leaderboard = userRepository.getLeaderboard();
        StringBuilder leaderboardMessage = new StringBuilder("LEADERBOARD:");
        int rank = 1;
        for (UserRepository.User user : leaderboard) {
            // Chỉnh sửa để phù hợp với định dạng: Hạng|Tên|Điểm|Thắng|Hòa|Thua
            leaderboardMessage.append(rank++).append("|") // Hạng
                    .append(user.username).append("|")     // Tên
                    .append(user.getPoints()).append("|")  // Điểm
                    .append(user.totalWins).append("|")    // Thắng
                    .append(user.totalDraws).append("|")   // Hòa
                    .append(user.totalLosses);             // Thua
            leaderboardMessage.append(";;"); // Ký tự phân tách giữa các dòng (row)
        }

        // Gỡ ký tự ";;" cuối cùng nếu danh sách không rỗng
        if (leaderboardMessage.length() > "LEADERBOARD:".length()) {
            leaderboardMessage.setLength(leaderboardMessage.length() - 2);
        }

        for (ClientController handler : onlineUsers.values()) {
            handler.sendMessage(leaderboardMessage.toString());
        }
    }
}
