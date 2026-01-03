package kuroyale;

import application.AchievementService;
import application.ArenaLayoutService;
import application.CardUpgradeService;
import application.DefaultQuestResetPolicy;
import application.DefaultUpgradePolicy;
import application.DeckService;
import application.MatchHistoryService;
import application.MatchService;
import application.NetworkService;
import application.QuestGenerator;
import application.QuestResetPolicy;
import application.QuestService;
import application.UpgradePolicy;
import application.network.NetworkConfigService;
import javafx.application.Application;
import javafx.stage.Stage;
import kuroyale.infrastructure.AchievementRepository;
import kuroyale.infrastructure.ArenaRepository;
import kuroyale.infrastructure.CardCatalogRepository;
import kuroyale.infrastructure.DeckRepository;
import kuroyale.infrastructure.MatchHistoryRepository;
import kuroyale.infrastructure.MatchRepository;
import kuroyale.infrastructure.PlayerProfileRepository;
import kuroyale.infrastructure.QuestRepository;

public class MainApp extends Application {
    // MainApp starts only the UI, not game logic
    // Launches JavaFX application
    @Override
    public void start(Stage primaryStage) {
        ArenaLayoutService layoutService = new ArenaLayoutService(new ArenaRepository());
        DeckService deckService = new DeckService(new CardCatalogRepository(), new DeckRepository());
        MatchService matchService = new MatchService(new MatchRepository());
        
        // Phase 2 services
        CardCatalogRepository cardCatalog = new CardCatalogRepository();
        PlayerProfileRepository profileRepository = new PlayerProfileRepository();
        UpgradePolicy upgradePolicy = new DefaultUpgradePolicy();
        CardUpgradeService upgradeService = new CardUpgradeService(profileRepository, cardCatalog, upgradePolicy);
        
        QuestRepository questRepository = new QuestRepository();
        QuestGenerator questGenerator = new QuestGenerator();
        QuestResetPolicy resetPolicy = new DefaultQuestResetPolicy();
        QuestService questService = new QuestService(questRepository, profileRepository, questGenerator, resetPolicy);
        
        MatchHistoryRepository historyRepository = new MatchHistoryRepository();
        MatchHistoryService historyService = new MatchHistoryService(historyRepository);
        
        AchievementRepository achievementRepository = new AchievementRepository();
        AchievementService achievementService = new AchievementService(achievementRepository, profileRepository);

        // Phase 2 network service
        NetworkService networkService = new NetworkService(new NetworkConfigService());
        
        ScreenNavigator navigator = new ScreenNavigator(primaryStage, layoutService, deckService, matchService,
            upgradeService, questService, historyService, achievementService, networkService);
        navigator.showWelcomeScreen();
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}