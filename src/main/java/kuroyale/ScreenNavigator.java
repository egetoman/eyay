package kuroyale;

import application.AchievementService;
import application.ArenaLayoutService;
import application.CardUpgradeService;
import application.DeckController;
import application.DeckService;
import application.MatchController;
import application.MatchHistoryService;
import application.MatchService;
import application.NetworkService;
import application.QuestService;
import application.challenge.ChallengeService;
import application.replay.ReplayRecorder;
import application.network.NetworkMatchController;
import application.network.NetworkLobbyController;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import kuroyale.domain.ArenaLayout;
import kuroyale.domain.Deck;
import kuroyale.domain.Match;
import kuroyale.domain.Player;
import kuroyale.domain.MatchRecord;
import kuroyale.domain.MatchReplay;
import java.time.LocalDateTime;

public class ScreenNavigator {

    private final Stage primaryStage;
    private final ArenaLayoutService arenaLayoutService;
    private final DeckController deckController;
    private final MatchService matchService;
    private final CardUpgradeService upgradeService;
    private final QuestService questService;
    private final MatchHistoryService historyService;
    private final AchievementService achievementService;
    private final NetworkService networkService;
    private final ChallengeService challengeService;

    private static final double DEFAULT_WIDTH = 800;
    private static final double DEFAULT_HEIGHT = 600;

    public ScreenNavigator(Stage primaryStage, ArenaLayoutService arenaLayoutService, DeckService deckService,
            MatchService matchService, CardUpgradeService upgradeService,
            QuestService questService, MatchHistoryService historyService,
            AchievementService achievementService,
            NetworkService networkService,
            ChallengeService challengeService) {
        this.primaryStage = primaryStage;
        this.arenaLayoutService = arenaLayoutService;
        this.deckController = new DeckController(deckService);
        this.matchService = matchService;
        this.upgradeService = upgradeService;
        this.questService = questService;
        this.historyService = historyService;
        this.achievementService = achievementService;
        this.networkService = networkService;
        this.challengeService = challengeService;
    }

    private Scene createScene(Parent root, double width, double height) {
        Scene scene = new Scene(root, width, height);
        // Apply Global Theme
        try {
            scene.getStylesheets().add(getClass().getResource("/theme.css").toExternalForm());
        } catch (Exception e) {
            System.err.println("Failed to load theme.css: " + e.getMessage());
        }
        return scene;
    }

    public void showWelcomeScreen() {
        WelcomeView view = new WelcomeView(this);
        Scene scene = createScene(view.getRoot(), DEFAULT_WIDTH, DEFAULT_HEIGHT);
        primaryStage.setTitle("KU Royale - Main Menu");
        primaryStage.setScene(scene);
    }

    public void showSplashScreen() {
        SplashScreenView view = new SplashScreenView(this);
        Scene scene = createScene(view.getRoot(), DEFAULT_WIDTH, DEFAULT_HEIGHT);
        primaryStage.setTitle("KU Royale");
        primaryStage.setScene(scene);
    }

    public void showStartGameScreen() {
        ArenaLayout activeLayout = arenaLayoutService.getActiveLayout();
        Deck playerDeck = deckController.loadDeck();
        Deck opponentDeck = deckController.buildDefaultDeck();
        Player player = new Player("Challenger", playerDeck, 0);
        Player bot = new Player("Bot", opponentDeck, 0);
        Match match = matchService.createMatch(player, bot, activeLayout);
        MatchController controller = new MatchController(matchService, match);
        StartGameView view = new StartGameView(this, controller, activeLayout);
        Scene scene = createScene(view.getRoot(), DEFAULT_WIDTH + 200, DEFAULT_HEIGHT + 200);
        primaryStage.setTitle("KU Royale - Match Preview");
        primaryStage.setScene(scene);
    }

    public void showLocalPvPSetupScreen() {
        LocalPvPSetupView view = new LocalPvPSetupView(this, arenaLayoutService, deckController, matchService);
        Scene scene = createScene(view.getRoot(), DEFAULT_WIDTH + 200, DEFAULT_HEIGHT + 200);
        primaryStage.setTitle("KU Royale - Local PvP Setup");
        primaryStage.setScene(scene);
    }

    public void showLocalPvPMatch(ArenaLayout layout, Player player1, Player player2) {
        Match match = matchService.createMatch(player1, player2, layout);
        match.setBotEnabled(false);
        MatchController controller = new MatchController(matchService, match);
        LocalPvPGameView view = new LocalPvPGameView(this, controller, layout);
        Scene scene = createScene(view.getRoot(), DEFAULT_WIDTH + 200, DEFAULT_HEIGHT + 200);
        primaryStage.setTitle("KU Royale - Local PvP");
        primaryStage.setScene(scene);
    }

    // Phase 2 Feature 2: Network Multiplayer (implemented next)
    public void showNetworkMenuScreen() {
        NetworkMenuView view = new NetworkMenuView(this, deckController, networkService,
                arenaLayoutService.getActiveLayout());
        Scene scene = createScene(view.getRoot(), DEFAULT_WIDTH + 200, DEFAULT_HEIGHT + 200);
        primaryStage.setTitle("KU Royale - Network Multiplayer");
        primaryStage.setScene(scene);
    }

    public void showNetworkLobbyScreen(application.network.NetworkLobbyController lobbyController) {
        NetworkLobbyView view = new NetworkLobbyView(this, lobbyController);
        Scene scene = createScene(view.getRoot(), DEFAULT_WIDTH + 200, DEFAULT_HEIGHT + 200);
        primaryStage.setTitle("KU Royale - Network Lobby");
        primaryStage.setScene(scene);
    }

    public void showNetworkMatchScreen(NetworkMatchController controller, ArenaLayout layout) {
        NetworkMatchView view = new NetworkMatchView(this, controller, layout);
        Scene scene = createScene(view.getRoot(), DEFAULT_WIDTH + 200, DEFAULT_HEIGHT + 200);
        primaryStage.setTitle("KU Royale - Network Match");
        primaryStage.setScene(scene);
    }

    public void showNetworkMatchFromLobby(NetworkLobbyController lobbyController, boolean hostSide,
            String startPayload) {
        if (lobbyController == null) {
            showWelcomeScreen();
            return;
        }
        Gson gson = new GsonBuilder().create();
        ArenaLayout layout = null;
        String payload = startPayload != null ? startPayload.trim() : "";

        // New protocol: startPayload is primarily the layout ID so both host and client
        // can resolve the same arena via ArenaLayoutService. For backwards
        // compatibility, we still support the older format where the full ArenaLayout
        // JSON was sent instead.
        if (!payload.isEmpty()) {
            // First, try to interpret payload as a layout ID.
            layout = arenaLayoutService.findById(payload).orElse(null);

            // If that fails and the payload looks like JSON, fall back to deserializing
            // the full layout (legacy behavior).
            if (layout == null && payload.startsWith("{")) {
                try {
                    layout = gson.fromJson(payload, ArenaLayout.class);
                } catch (Exception ignored) {
                    // If deserialization fails, we'll fall back to the active layout below.
                }
            }
        }
        if (layout == null) {
            layout = arenaLayoutService.getActiveLayout();
        }

        NetworkMatchController matchController = new NetworkMatchController(
                lobbyController.getAdapter(),
                lobbyController.getConfig(),
                hostSide,
                hostSide ? 1 : 2);
        matchController.attachLayoutForClient(layout);

        // Deck enforcement: local side must map to local player id.
        int localId = hostSide ? 1 : 2;
        int remoteId = hostSide ? 2 : 1;
        matchController.setAllowedDeckIds(localId, lobbyController.getState().getLocalDeckCardIds());
        matchController.setAllowedDeckIds(remoteId, lobbyController.getState().getRemoteDeckCardIds());

        if (!hostSide) {
            // Client reconnect target
            matchController.setReconnectTarget(lobbyController.getConnectHost(), lobbyController.getConnectPort());
        }

        if (hostSide) {
            // Host creates authoritative match with both players; opponent deck is a
            // default deck placeholder.
            Deck hostDeck = deckController.loadDeck();
            Deck opponentDeck = buildDeckFromIds(lobbyController.getState().getRemoteDeckCardIds(),
                    deckController.buildDefaultDeck());
            Player hostPlayer = new Player(lobbyController.getState().getLocalName(), hostDeck, 0);
            Player clientPlayer = new Player(lobbyController.getState().getRemoteName(), opponentDeck, 0);
            Match match = matchService.createMatch(hostPlayer, clientPlayer, layout);
            match.setBotEnabled(false);
            matchController.attachHostMatch(match, layout);

            matchController.setLocalDeck(hostDeck);
            matchController.setAllowedDeckIds(1, lobbyController.getState().getLocalDeckCardIds());
            matchController.setAllowedDeckIds(2, lobbyController.getState().getRemoteDeckCardIds());
        } else {
            // Client uses its own local deck for UI selection
            matchController.setLocalDeck(deckController.loadDeck());
            // If the lobby already received a snapshot, transfer it so the match view
            // renders immediately.
            String snapshotJson = lobbyController.getLastReceivedSnapshotJson();
            if (snapshotJson != null && !snapshotJson.isBlank()) {
                matchController.onMessage(new application.network.NetworkMessage(
                        application.network.NetworkMessageType.STATE_SNAPSHOT, 0, snapshotJson, ""));
            }
        }

        lobbyController.getAdapter().setListener(matchController);
        showNetworkMatchScreen(matchController, layout);
    }

    private Deck buildDeckFromIds(java.util.List<String> ids, Deck fallback) {
        if (ids == null || ids.isEmpty()) {
            return fallback;
        }
        java.util.Map<String, kuroyale.domain.Card> byId = new java.util.HashMap<>();
        for (kuroyale.domain.Card c : new kuroyale.infrastructure.CardCatalogRepository().findAll()) {
            if (c != null && c.getId() != null) {
                byId.put(c.getId(), c);
            }
        }
        java.util.List<kuroyale.domain.Card> cards = new java.util.ArrayList<>();
        for (String id : ids) {
            if (id == null || id.isBlank())
                continue;
            kuroyale.domain.Card c = byId.get(id.trim());
            if (c != null) {
                cards.add(c);
            }
            if (cards.size() >= kuroyale.domain.Deck.MAX_CARDS) {
                break;
            }
        }
        if (cards.size() != kuroyale.domain.Deck.MAX_CARDS) {
            return fallback;
        }
        return new Deck(cards);
    }

    // Phase 2 Feature 4: Challenge Mode
    public void showChallengeModeScreen() {
        ChallengeModeView view = new ChallengeModeView(this, challengeService);
        Scene scene = createScene(view.getRoot(), DEFAULT_WIDTH + 200, DEFAULT_HEIGHT + 200);
        primaryStage.setTitle("KU Royale - Challenge Mode");
        primaryStage.setScene(scene);
    }

    public void showChallengeMatchScreen(application.challenge.ChallengeSession session) {
        ChallengeMatchView view = new ChallengeMatchView(this, challengeService, matchService, session,
                arenaLayoutService.getActiveLayout());
        Scene scene = createScene(view.getRoot(), DEFAULT_WIDTH + 200, DEFAULT_HEIGHT + 200);
        primaryStage.setTitle("KU Royale - Challenge Match");
        primaryStage.setScene(scene);
    }

    public void showDeckBuilderScreen() {
        DeckBuilderView view = new DeckBuilderView(this, deckController);
        Scene scene = createScene(view.getRoot(), DEFAULT_WIDTH + 200, DEFAULT_HEIGHT + 200);
        primaryStage.setTitle("KU Royale - Deck Builder");
        primaryStage.setScene(scene);
    }

    public void showArenaDemoScreen() {
        ArenaView view = new ArenaView(this, arenaLayoutService.getActiveLayout(), this::showWelcomeScreen);
        Scene scene = createScene(view.getRoot(), DEFAULT_WIDTH, DEFAULT_HEIGHT);
        primaryStage.setTitle("KU Royale - Arena Preview");
        primaryStage.setScene(scene);
    }

    public void showArenaDesignerScreen() {
        ArenaDesignerView view = new ArenaDesignerView(this, arenaLayoutService);
        Scene scene = createScene(view.getRoot(), DEFAULT_WIDTH + 200, DEFAULT_HEIGHT + 200);
        primaryStage.setTitle("KU Royale - Arena Designer");
        primaryStage.setScene(scene);
    }

    public void showLayoutLibrary() {
        LayoutLibraryView view = new LayoutLibraryView(this, arenaLayoutService);
        Scene scene = createScene(view.getRoot(), DEFAULT_WIDTH, DEFAULT_HEIGHT);
        primaryStage.setTitle("Saved Arenas");
        primaryStage.setScene(scene);
    }

    public void showArenaSelection() {
        ArenaSelectionView view = new ArenaSelectionView(this, arenaLayoutService);
        Scene scene = createScene(view.getRoot(), DEFAULT_WIDTH, DEFAULT_HEIGHT);
        primaryStage.setTitle("Select Arena");
        primaryStage.setScene(scene);
    }

    public void showArenaPreview(ArenaLayout layout) {
        showArenaPreview(layout, this::showWelcomeScreen);
    }

    public void showArenaPreview(ArenaLayout layout, Runnable backAction) {
        ArenaView view = new ArenaView(this, layout, backAction);
        Scene scene = createScene(view.getRoot(), DEFAULT_WIDTH, DEFAULT_HEIGHT);
        primaryStage.setTitle("Preview - " + layout.getName());
        primaryStage.setScene(scene);
    }

    public void showUpgradeCardScreen() {
        UpgradeCardView view = new UpgradeCardView(this, upgradeService);
        Scene scene = createScene(view.getRoot(), DEFAULT_WIDTH + 200, DEFAULT_HEIGHT + 200);
        primaryStage.setTitle("KU Royale - Upgrade Card");
        primaryStage.setScene(scene);
    }

    public void showDailyQuestScreen() {
        DailyQuestView view = new DailyQuestView(this, questService);
        Scene scene = createScene(view.getRoot(), DEFAULT_WIDTH, DEFAULT_HEIGHT);
        primaryStage.setTitle("KU Royale - Daily Quests");
        primaryStage.setScene(scene);
    }

    public void showMatchHistoryScreen() {
        MatchHistoryView view = new MatchHistoryView(this, historyService);
        Scene scene = createScene(view.getRoot(), DEFAULT_WIDTH, DEFAULT_HEIGHT);
        primaryStage.setTitle("KU Royale - Match History & Stats");
        primaryStage.setScene(scene);
    }

    public void showReplayScreen(MatchRecord record) {
        ReplayView view = new ReplayView(this, arenaLayoutService, record);
        Scene scene = createScene(view.getRoot(), DEFAULT_WIDTH + 200, DEFAULT_HEIGHT + 200);
        primaryStage.setTitle("KU Royale - Replay");
        primaryStage.setScene(scene);
    }

    public void recordMatchWithReplay(Match match, String opponentType, ArenaLayout layout, MatchReplay replay) {
        if (historyService == null || match == null) {
            return;
        }
        String result = "Unknown";
        int crowns = 0;
        var outcome = match.getOutcome();
        if (outcome != null) {
            if (outcome.getWinner() == null) {
                result = "Draw";
            } else if (outcome.getWinner() == kuroyale.domain.TowerOwner.PLAYER) {
                result = "Win";
            } else {
                result = "Loss";
            }
            crowns = outcome.getPlayerCrowns();
        } else if (match.isFinished()) {
            result = "Draw";
        }

        MatchRecord record = new MatchRecord(
                java.util.UUID.randomUUID().toString(),
                LocalDateTime.now(),
                opponentType,
                result,
                crowns,
                0,
                layout != null ? layout.getName() : "Unknown");
        if (layout != null) {
            record.setArenaLayoutId(layout.getId());
        }
        record.setReplay(replay);
        historyService.recordMatch(record);
    }

    public void showAchievementScreen() {
        AchievementView view = new AchievementView(this, achievementService);
        Scene scene = createScene(view.getRoot(), DEFAULT_WIDTH, DEFAULT_HEIGHT);
        primaryStage.setTitle("KU Royale - Achievements");
        primaryStage.setScene(scene);
    }

    public void showComboLibraryScreen() {
        ComboLibraryView view = new ComboLibraryView(this, deckController);
        Scene scene = createScene(view.getRoot(), DEFAULT_WIDTH, DEFAULT_HEIGHT);
        primaryStage.setTitle("KU Royale - Combo Library");
        primaryStage.setScene(scene);
    }

    public void showSettingsScreen() {
        SettingsView view = new SettingsView(this);
        Scene scene = createScene(view.getRoot(), DEFAULT_WIDTH, DEFAULT_HEIGHT);
        primaryStage.setTitle("KU Royale - Settings");
        primaryStage.setScene(scene);
    }

    public void setFullScreen(boolean enabled) {
        primaryStage.setFullScreen(enabled);
    }

    public boolean isFullScreen() {
        return primaryStage.isFullScreen();
    }

    public application.QuestService getQuestService() {
        return questService;
    }

    public application.AchievementService getAchievementService() {
        return achievementService;
    }

    public void showDeckComboScreen() {
        DeckComboView view = new DeckComboView(this, deckController);
        Scene scene = createScene(view.getRoot(), DEFAULT_WIDTH, DEFAULT_HEIGHT);
        primaryStage.setTitle("KU Royale - Deck Combos");
        primaryStage.setScene(scene);
    }
}
