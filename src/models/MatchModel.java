package models;

import java.util.ArrayList;
import java.util.List;

public class MatchModel {
    private static final int NUM_ORDERS_PER_MATCH = 5;
    private static final int MATCH_TIME_SECONDS = 60;

    private List<List<String>> allOrders = new ArrayList<>();
    private List<List<String>> allShelves = new ArrayList<>();
    private int player1Progress = 0;
    private int player2Progress = 0;
    private boolean player1WantsToPlayAgain = false;
    private boolean player2WantsToPlayAgain = false;
    private boolean matchEnded = false;

    public void reset() {
        allOrders.clear();
        allShelves.clear();
        player1Progress = 0;
        player2Progress = 0;
        player1WantsToPlayAgain = false;
        player2WantsToPlayAgain = false;
        matchEnded = false;
    }

    // Getters & Setters
    public List<List<String>> getAllOrders() { return allOrders; }
    public List<List<String>> getAllShelves() { return allShelves; }
    public int getPlayer1Progress() { return player1Progress; }
    public void setPlayer1Progress(int progress) { this.player1Progress = progress; }
    public int getPlayer2Progress() { return player2Progress; }
    public void setPlayer2Progress(int progress) { this.player2Progress = progress; }
    public boolean isPlayer1WantsToPlayAgain() { return player1WantsToPlayAgain; }
    public void setPlayer1WantsToPlayAgain(boolean wants) { this.player1WantsToPlayAgain = wants; }
    public boolean isPlayer2WantsToPlayAgain() { return player2WantsToPlayAgain; }
    public void setPlayer2WantsToPlayAgain(boolean wants) { this.player2WantsToPlayAgain = wants; }
    public boolean isMatchEnded() { return matchEnded; }
    public void setMatchEnded(boolean ended) { this.matchEnded = ended; }
    public static int getNumOrdersPerMatch() { return NUM_ORDERS_PER_MATCH; }
    public static int getMatchTimeSeconds() { return MATCH_TIME_SECONDS; }
}
