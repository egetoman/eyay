package main.java.kuroyale;

import javafx.scene.Scene;
import javafx.stage.Stage;

public class ScreenNavigator {

    private final Stage primaryStage;
    private static final double DEFAULT_WIDTH = 800;
    private static final double DEFAULT_HEIGHT = 600;

    public ScreenNavigator(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public void showWelcomeScreen() {
        WelcomeView view = new WelcomeView(this);
        Scene scene = new Scene(view.getRoot(), DEFAULT_WIDTH, DEFAULT_HEIGHT);
        primaryStage.setTitle("KU Royale - Main Menu");
        primaryStage.setScene(scene);
    }

    public void showStartGameScreen() {
        StartGameView view = new StartGameView(this);
        Scene scene = new Scene(view.getRoot(), DEFAULT_WIDTH, DEFAULT_HEIGHT);
        primaryStage.setTitle("KU Royale - Start Game");
        primaryStage.setScene(scene);
    }

    public void showDeckBuilderScreen() {
        DeckBuilderView view = new DeckBuilderView(this);
        Scene scene = new Scene(view.getRoot(), DEFAULT_WIDTH, DEFAULT_HEIGHT);
        primaryStage.setTitle("KU Royale - Deck Builder");
        primaryStage.setScene(scene);
    }
}




