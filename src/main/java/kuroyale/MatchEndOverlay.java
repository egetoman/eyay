package kuroyale;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Lightweight Clash-Royale-like end screen overlay.
 * <p>
 * Implemented as a helper that renders into a provided {@link StackPane} layer.
 */
public final class MatchEndOverlay {

    private MatchEndOverlay() {
    }

    public static void show(
            StackPane overlayLayer,
            String topName,
            int topCrowns,
            String bottomName,
            int bottomCrowns,
            String headline,
            Runnable onPlayAgain,
            Runnable onOk) {
        show(overlayLayer, topName, topCrowns, bottomName, bottomCrowns, headline, onPlayAgain, onOk, 0, 0);
    }

    public static void show(
            StackPane overlayLayer,
            String topName,
            int topCrowns,
            String bottomName,
            int bottomCrowns,
            String headline,
            Runnable onPlayAgain,
            Runnable onOk,
            int comboCount,
            int comboGold) {
        if (overlayLayer == null) {
            return;
        }
        overlayLayer.setVisible(true);
        overlayLayer.setMouseTransparent(false);
        overlayLayer.getChildren().clear();

        Rectangle dim = new Rectangle();
        dim.widthProperty().bind(overlayLayer.widthProperty());
        dim.heightProperty().bind(overlayLayer.heightProperty());
        dim.setFill(Color.color(0, 0, 0, 0.65));

        Label head = new Label(headline != null ? headline : "Match Ended");
        head.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 28));
        head.setTextFill(resolveHeadlineColor(headline));

        Label sub = new Label("Match Summary");
        sub.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        sub.setTextFill(Color.web("#cbd0d6"));

        VBox topBanner = banner(topName, clampCrowns(topCrowns), "#b63a4c");
        VBox bottomBanner = banner(bottomName, clampCrowns(bottomCrowns), "#2a6fd2");

        Label vs = new Label("VS");
        vs.setTextFill(Color.web("#e7e7e7"));
        vs.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        Button playAgain = new Button("Play Again");
        playAgain.setStyle("-fx-background-color: #f2c94c; -fx-text-fill: #1a1a1a; -fx-font-weight: bold;");
        playAgain.setOnAction(e -> {
            if (onPlayAgain != null) {
                onPlayAgain.run();
            }
        });

        Button ok = new Button("OK");
        ok.setStyle("-fx-background-color: #4a90e2; -fx-text-fill: white; -fx-font-weight: bold;");
        ok.setOnAction(e -> {
            if (onOk != null) {
                onOk.run();
            }
        });

        HBox buttons = new HBox(12, playAgain, ok);
        buttons.setAlignment(Pos.CENTER);
        buttons.setPadding(new Insets(8, 0, 0, 0));

        Region divider = new Region();
        divider.setPrefHeight(1);
        divider.setStyle("-fx-background-color: rgba(255,255,255,0.08);");

        VBox card = new VBox(12, head, sub, divider, topBanner, vs, bottomBanner);
        if (comboCount > 0) {
            Label comboLine = new Label("Combos Triggered: " + comboCount + " (+" + comboGold + " gold)");
            comboLine.setFont(Font.font("Arial", FontWeight.BOLD, 16));
            comboLine.setTextFill(Color.web("#ffd54f"));
            card.getChildren().add(comboLine);
        }

        Label tip = new Label("OK returns to menu. Play Again starts a new match.");
        tip.setFont(Font.font("Arial", 11));
        tip.setTextFill(Color.web("#9aa3ad"));

        card.getChildren().add(buttons);
        card.getChildren().add(tip);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(18));
        card.setMaxWidth(440);
        card.setStyle("-fx-background-color: rgba(20, 22, 28, 0.95); -fx-background-radius: 16; -fx-border-color: rgba(255,255,255,0.12); -fx-border-radius: 16;");

        overlayLayer.getChildren().addAll(dim, card);
        StackPane.setAlignment(card, Pos.CENTER);
    }

    private static VBox banner(String name, int crowns, String color) {
        Label n = new Label(name != null && !name.isBlank() ? name : "—");
        n.setTextFill(Color.WHITE);
        n.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 18));

        HBox crownsRow = crownsRow(crowns);

        VBox box = new VBox(6, crownsRow, n);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(12, 16, 12, 16));
        box.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 12;");
        return box;
    }

    private static HBox crownsRow(int crowns) {
        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER);
        for (int i = 0; i < 3; i++) {
            Label c = new Label("♛");
            c.setFont(Font.font("Arial", FontWeight.BOLD, 20));
            boolean filled = i < crowns;
            c.setTextFill(filled ? Color.web("#ffd54f") : Color.web("#6b7280"));
            row.getChildren().add(c);
        }
        return row;
    }

    private static int clampCrowns(int crowns) {
        return Math.max(0, Math.min(3, crowns));
    }

    private static Color resolveHeadlineColor(String headline) {
        if (headline == null) {
            return Color.WHITE;
        }
        String normalized = headline.toLowerCase();
        if (normalized.contains("victory") || normalized.contains("win")) {
            return Color.web("#9be564");
        }
        if (normalized.contains("defeat") || normalized.contains("loss")) {
            return Color.web("#f05a5b");
        }
        return Color.WHITE;
    }
}



