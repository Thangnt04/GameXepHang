package services;

import models.MatchModel;
import java.util.*;

public class GameSessionService {
    private static final String[] ALL_ITEMS = {
        "Táo", "Chuối", "Cam", "Nho", "Sữa", "Bánh Mì", "Trứng", "Phô Mai", "Thịt Gà", "Cá"
    };
    private static final int ORDER_SIZE = 5;

    public void generateOrders(MatchModel model) {
        model.reset();
        for (int i = 0; i < MatchModel.getNumOrdersPerMatch(); i++) {
            List<String> allItemsList = new ArrayList<>(Arrays.asList(ALL_ITEMS));
            Collections.shuffle(allItemsList);
            List<String> order = new ArrayList<>(allItemsList.subList(0, ORDER_SIZE));

            List<String> shelf = new ArrayList<>(order);
            while (shelf.size() < ALL_ITEMS.length) {
                shelf.add(allItemsList.get(shelf.size()));
            }
            Collections.shuffle(shelf);

            model.getAllOrders().add(order);
            model.getAllShelves().add(shelf);
        }
    }

    public boolean validateSubmission(String submissionCsv, String correctOrderCsv) {
        return correctOrderCsv.equals(submissionCsv);
    }

    public String calculateMatchResult(int p1Progress, int p2Progress, boolean p1Finished, boolean p2Finished) {
        if (p1Finished && !p2Finished) return "P1_WIN";
        if (!p1Finished && p2Finished) return "P2_WIN";
        if (p1Progress > p2Progress) return "P1_WIN";
        if (p2Progress > p1Progress) return "P2_WIN";
        return "DRAW";
    }
}
