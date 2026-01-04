package kuroyale;

import application.AchievementService;
import application.ArenaLayoutService;
import application.CardUpgradeService;
import application.DeckController;
import application.DeckService;
import application.MatchHistoryService;
import application.MatchService;
import application.QuestService;
import javafx.scene.Scene;
import javafx.stage.Stage;
import kuroyale.domain.ArenaLayout;
import kuroyale.domain.Deck;
import kuroyale.domain.Match;
import kuroyale.domain.Player;

public class ScreenNavigator {

    private final Stage primaryStage;
    private final ArenaLayoutService arenaLayoutService;
    private final DeckController deckController;
    private final MatchService matchService;
    private final CardUpgradeService upgradeService;
    private final QuestService questService;
    private final MatchHistoryService historyService;
    private final AchievementService achievementService;

    private static final double DEFAULT_WIDTH = 800;
    private static final double DEFAULT_HEIGHT = 600;

    public ScreenNavigator(Stage primaryStage, ArenaLayoutService arenaLayoutService, DeckService deckService, 
                          MatchService matchService, CardUpgradeService upgradeService, 
                          QuestService questService, MatchHistoryService historyService,
                          AchievementService achievementService) {
        this.primaryStage = primaryStage;
        this.arenaLayoutService = arenaLayoutService;
        this.deckController = new DeckController(deckService);
        this.matchService = matchService;
        this.upgradeService = upgradeService;
        this.questService = questService;
        this.historyService = historyService;
        this.achievementService = achievementService;
    }

    public void showWelcomeScreen() {
        WelcomeView view = new WelcomeView(this);
        Scene scene = new Scene(view.getRoot(), DEFAULT_WIDTH, DEFAULT_HEIGHT);
        primaryStage.setTitle("KU Royale - Main Menu");
        primaryStage.setScene(scene);
    }

    public void showStartGameScreen() {
        ArenaLayout activeLayout = arenaLayoutService.getActiveLayout();
        Deck playerDeck = deckController.loadDeck();
        Deck opponentDeck = deckController.buildDefaultDeck();
        Player player = new Player("Challenger", playerDeck, 0);
        Player bot = new Player("Bot", opponentDeck, 0);
        Match match = matchService.createMatch(player, bot, activeLayout);
        StartGameView view = new StartGameView(this, match, activeLayout);
        Scene scene = new Scene(view.getRoot(), DEFAULT_WIDTH + 200, DEFAULT_HEIGHT + 200);
        primaryStage.setTitle("KU Royale - Match Preview");
        primaryStage.setScene(scene);
    }

    public void showDeckBuilderScreen() {
        DeckBuilderView view = new DeckBuilderView(this, deckController);
        Scene scene = new Scene(view.getRoot(), DEFAULT_WIDTH + 200, DEFAULT_HEIGHT + 200);
        primaryStage.setTitle("KU Royale - Deck Builder");
        primaryStage.setScene(scene);
    }

    public void showArenaDemoScreen() {
        ArenaView view = new ArenaView(this, arenaLayoutService.getActiveLayout(), this::showWelcomeScreen);
        Scene scene = new Scene(view.getRoot(), DEFAULT_WIDTH, DEFAULT_HEIGHT);
        primaryStage.setTitle("KU Royale - Arena Preview");
        primaryStage.setScene(scene);
    }

    public void showArenaDesignerScreen() {
        ArenaDesignerView view = new ArenaDesignerView(this, arenaLayoutService);
        Scene scene = new Scene(view.getRoot(), DEFAULT_WIDTH + 200, DEFAULT_HEIGHT + 200);
        primaryStage.setTitle("KU Royale - Arena Designer");
        primaryStage.setScene(scene);
    }

    public void showLayoutLibrary() {
        LayoutLibraryView view = new LayoutLibraryView(this, arenaLayoutService);
        Scene scene = new Scene(view.getRoot(), DEFAULT_WIDTH, DEFAULT_HEIGHT);
        primaryStage.setTitle("Saved Arenas");
        primaryStage.setScene(scene);
    }

    public void showArenaSelection() {
        ArenaSelectionView view = new ArenaSelectionView(this, arenaLayoutService);
        Scene scene = new Scene(view.getRoot(), DEFAULT_WIDTH, DEFAULT_HEIGHT);
        primaryStage.setTitle("Select Arena");
        primaryStage.setScene(scene);
    }

    public void showArenaPreview(ArenaLayout layout) {
        showArenaPreview(layout, this::showWelcomeScreen);
    }

    public void showArenaPreview(ArenaLayout layout, Runnable backAction) {
        ArenaView view = new ArenaView(this, layout, backAction);
        Scene scene = new Scene(view.getRoot(), DEFAULT_WIDTH, DEFAULT_HEIGHT);
        primaryStage.setTitle("Preview - " + layout.getName());
        primaryStage.setScene(scene);
    }

    public void showUpgradeCardScreen() {
        UpgradeCardView view = new UpgradeCardView(this, upgradeService);
        Scene scene = new Scene(view.getRoot(), DEFAULT_WIDTH + 200, DEFAULT_HEIGHT + 200);
        primaryStage.setTitle("KU Royale - Upgrade Card");
        primaryStage.setScene(scene);
    }

    public void showDailyQuestScreen() {
        DailyQuestView view = new DailyQuestView(this, questService);
        Scene scene = new Scene(view.getRoot(), DEFAULT_WIDTH, DEFAULT_HEIGHT);
        primaryStage.setTitle("KU Royale - Daily Quests");
        primaryStage.setScene(scene);
    }

    public void showMatchHistoryScreen() {
        MatchHistoryView view = new MatchHistoryView(this, historyService);
        Scene scene = new Scene(view.getRoot(), DEFAULT_WIDTH, DEFAULT_HEIGHT);
        primaryStage.setTitle("KU Royale - Match History & Stats");
        primaryStage.setScene(scene);
    }

    public void showAchievementScreen() {
        AchievementView view = new AchievementView(this, achievementService);
        Scene scene = new Scene(view.getRoot(), DEFAULT_WIDTH, DEFAULT_HEIGHT);
        primaryStage.setTitle("KU Royale - Achievements");
        primaryStage.setScene(scene);
    }

    public void showComboLibraryScreen() {
        ComboLibraryView view = new ComboLibraryView(this);
        Scene scene = new Scene(view.getRoot(), DEFAULT_WIDTH, DEFAULT_HEIGHT);
        primaryStage.setTitle("KU Royale - Combo Library");
        primaryStage.setScene(scene);
    }

    public void showDeckComboScreen() {
        DeckComboView view = new DeckComboView(this, deckController);
        Scene scene = new Scene(view.getRoot(), DEFAULT_WIDTH, DEFAULT_HEIGHT);
        primaryStage.setTitle("KU Royale - Deck Combos");
        primaryStage.setScene(scene);
    }
}




