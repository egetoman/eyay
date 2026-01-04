package kuroyale;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import kuroyale.domain.Card;

/**
 * Small UI helpers shared across match-related screens.
 * <p>
 * This avoids copy/pasting the card-slot rendering logic across Local PvP / Network / Replay screens.
 */
final class StartGameUiBits {
    private StartGameUiBits() {
    }

    static StackPane createCardSlot(Card card, boolean large, boolean highlighted) {
        double width = large ? 70 : 54;
        double height = large ? 90 : 70;

        StackPane tile = new StackPane();
        tile.setPrefSize(width, height);
        applySlotStyle(tile, large, highlighted);

        VBox content = new VBox(4);
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(6));

        Label name = new Label(card != null ? card.getName() : "Empty");
        name.setWrapText(true);
        name.setAlignment(Pos.CENTER);
        name.setStyle("-fx-text-fill: white;");
        name.setFont(Font.font("Arial", FontWeight.BOLD, large ? 11 : 9));

        Label hp = new Label(card != null && card.getStats() != null ? "HP " + card.getStats().getHp() : "HP —");
        hp.setStyle("-fx-text-fill: #a6ffcb;");
        hp.setFont(Font.font("Arial", large ? 10 : 8));

        StackPane costChip = buildCostChip(card != null ? card.getElixirCost() : -1, large);
        StackPane.setAlignment(costChip, Pos.BOTTOM_CENTER);
        StackPane.setMargin(costChip, new Insets(0, 0, 4, 0));

        content.getChildren().addAll(name, hp);
        tile.getChildren().addAll(content, costChip);
        return tile;
    }

    private static void applySlotStyle(StackPane tile, boolean large, boolean highlighted) {
        StringBuilder style = new StringBuilder();
        style.append("-fx-background-color: linear-gradient(#2a2f45, #1c1f2e);");
        if (large) {
            style.append("-fx-background-radius: 10; -fx-border-radius: 10;");
        } else {
            style.append("-fx-background-radius: 8; -fx-border-radius: 8;");
        }
        if (highlighted) {
            style.append("-fx-border-color: #ffd54f; -fx-border-width: 3;");
        } else {
            style.append("-fx-border-color: #404459; -fx-border-width: 1;");
        }
        tile.setStyle(style.toString());
    }

    private static StackPane buildCostChip(int cost, boolean large) {
        String text = cost >= 0 ? String.valueOf(cost) : "-";
        Label label = new Label(text);
        label.setFont(Font.font("Arial", FontWeight.BOLD, large ? 14 : 12));
        label.setStyle("-fx-text-fill: white;");

        StackPane chip = new StackPane(label);
        chip.setPadding(new Insets(4, 10, 4, 10));
        chip.setStyle("-fx-background-color: #b259ff; -fx-background-radius: 20;");
        return chip;
    }
}


