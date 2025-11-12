package models;

import java.util.ArrayList;
import java.util.List;

public class GameState {
    private String username;
    private String currentOpponent;
    private List<String> currentOrder = new ArrayList<>();
    private List<String> currentPackingTray = new ArrayList<>();
    private int myProgress = 0;
    private int opponentProgress = 0;
    private int timeLeft = 0;
    private boolean hasAskedToPlayAgain = false;

    public void reset() {
        currentOpponent = null;
        currentOrder.clear();
        currentPackingTray.clear();
        myProgress = 0;
        opponentProgress = 0;
        hasAskedToPlayAgain = false;
    }

    // Getters & Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getCurrentOpponent() { return currentOpponent; }
    public void setCurrentOpponent(String opponent) { this.currentOpponent = opponent; }
    public List<String> getCurrentOrder() { return currentOrder; }
    public List<String> getCurrentPackingTray() { return currentPackingTray; }
    public int getMyProgress() { return myProgress; }
    public void setMyProgress(int progress) { this.myProgress = progress; }
    public int getOpponentProgress() { return opponentProgress; }
    public void setOpponentProgress(int progress) { this.opponentProgress = progress; }
    public int getTimeLeft() { return timeLeft; }
    public void setTimeLeft(int time) { this.timeLeft = time; }
    public boolean isHasAskedToPlayAgain() { return hasAskedToPlayAgain; }
    public void setHasAskedToPlayAgain(boolean asked) { this.hasAskedToPlayAgain = asked; }
}
