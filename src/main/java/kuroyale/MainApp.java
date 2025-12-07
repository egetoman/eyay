package kuroyale;

import application.ArenaLayoutService;
import application.DeckService;
import application.MatchService;
import javafx.application.Application;
import javafx.stage.Stage;
import kuroyale.infrastructure.ArenaRepository;
import kuroyale.infrastructure.CardCatalogRepository;
import kuroyale.infrastructure.DeckRepository;
import kuroyale.infrastructure.MatchRepository;

public class MainApp extends Application {
    // MainApp starts only the UI, not game logic
    // Launches JavaFX application
    @Override
    public void start(Stage primaryStage) {
        ArenaLayoutService layoutService = new ArenaLayoutService(new ArenaRepository());
        DeckService deckService = new DeckService(new CardCatalogRepository(), new DeckRepository());
        MatchService matchService = new MatchService(new MatchRepository());
        ScreenNavigator navigator = new ScreenNavigator(primaryStage, layoutService, deckService, matchService);
        navigator.showWelcomeScreen();
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}