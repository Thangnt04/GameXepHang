package models;

public class ClientSession {
    private String username;
    private int userId;
    private int totalWins;
    private int totalDraws;
    private int totalLosses;
    private int totalMatches;
    private boolean inGame;

    public ClientSession(String username, int userId, int wins, int draws, int losses) {
        this.username = username;
        this.userId = userId;
        this.totalWins = wins;
        this.totalDraws = draws;
        this.totalLosses = losses;
        this.totalMatches = wins + draws + losses;
        this.inGame = false;
    }

    public void updateStats(int wins, int draws, int losses) {
        this.totalWins = wins;
        this.totalDraws = draws;
        this.totalLosses = losses;
        this.totalMatches = wins + draws + losses;
    }

    // Getters
    public String getUsername() { return username; }
    public int getUserId() { return userId; }
    public int getTotalWins() { return totalWins; }
    public int getTotalDraws() { return totalDraws; }
    public int getTotalLosses() { return totalLosses; }
    public int getTotalMatches() { return totalMatches; }
    public boolean isInGame() { return inGame; }
    public void setInGame(boolean inGame) { this.inGame = inGame; }
}
