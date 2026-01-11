package kuroyale;

import application.MatchController;
import application.MatchService;
import kuroyale.domain.*;
import kuroyale.support.Result;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import javafx.application.Platform;
import java.awt.GraphicsEnvironment;
import org.junit.jupiter.api.Assumptions;

import java.lang.reflect.Method;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;


public class LocalPvPGameViewTest {

    private static volatile boolean javaFxAvailable = true;

    private static boolean shouldSkipJavaFx() {
        // In CI / headless test runs, JavaFX can crash/hang while probing Screen devices.
        // Surefire sets -Dtestfx.headless=true in this project, so respect that.
        if (Boolean.getBoolean("testfx.headless")) {
            return true;
        }
        return GraphicsEnvironment.isHeadless();
    }

    private LocalPvPGameView gameView;
    private Match match;
    private Arena arena;
    private Player bottomPlayer;
    private Player topPlayer;
    private MatchController controller;
    private ArenaLayout layout;

    @BeforeAll
    static void initJavaFX() {
        if (shouldSkipJavaFx()) {
            javaFxAvailable = false;
            return;
        }

        try {
            Platform.startup(() -> {
            });
        } catch (IllegalStateException e) {
            // Already initialized, which is fine
            javaFxAvailable = true;
        } catch (Throwable t) {
            // If JavaFX cannot initialize (e.g., no screens in headless sandbox), skip these UI tests.
            javaFxAvailable = false;
        }
    }

    @BeforeEach
    void setUp() {
        Assumptions.assumeTrue(javaFxAvailable, "JavaFX not available in this environment");

        // Create arena with standard dimensions (height = 32)
        arena = new Arena(18, 32);
        layout = ArenaLayout.defaultLayout();

        // Create players
        bottomPlayer = new Player("Player 1", new Deck(new ArrayList<>()), 0);
        topPlayer = new Player("Player 2", new Deck(new ArrayList<>()), 0);

        // Create match
        match = new Match(bottomPlayer, topPlayer, arena);

        // Create controller
        MatchService matchService = new MatchService(null);
        controller = new MatchController(matchService, match);

        // Create game view - wrap in try-catch to handle JavaFX initialization failures
        // This will attempt to initialize JavaFX when the first JavaFX class is loaded
        try {
            gameView = new LocalPvPGameView(null, controller, layout);
        } catch (ExceptionInInitializerError | NoClassDefFoundError | RuntimeException e) {
            System.err.println("Failed to create LocalPvPGameView - JavaFX not available: " + e.getMessage());
            gameView = null;
            Assumptions.assumeTrue(false, "JavaFX not available in this environment");
        }
    }

    @Test
    void validateDeploySide_nullActingPlayer_returnsFailure() throws Exception {
        // Setup
        Position tile = new Position(5, 10);

        // Execute
        Result<?> result = invokeValidateDeploySide(null, tile);

        // Verify
        assertFalse(result.isSuccess(), "Should fail with null player");
        assertEquals("Invalid deployment.", result.getMessage());
    }

    @Test
    void validateDeploySide_nullTile_returnsFailure() throws Exception {
        // Setup
        Player acting = bottomPlayer;

        // Execute
        Result<?> result = invokeValidateDeploySide(acting, null);

        // Verify
        assertFalse(result.isSuccess(), "Should fail with null tile");
        assertEquals("Invalid deployment.", result.getMessage());
    }

    @Test
    void validateDeploySide_nullMatch_returnsFailure() throws Exception {
        // Setup: Create gameView with null match
        LocalPvPGameView viewWithNullMatch = new LocalPvPGameView(null, null, layout);
        Position tile = new Position(5, 10);

        // Execute
        Result<?> result = invokeValidateDeploySide(viewWithNullMatch, bottomPlayer, tile);

        // Verify
        assertFalse(result.isSuccess(), "Should fail with null match");
        assertEquals("Invalid deployment.", result.getMessage());
    }

    @Test
    void validateDeploySide_deployOnRiverTop_returnsFailure() throws Exception {
        // Setup: River top row = height/2 - 1 = 32/2 - 1 = 15
        int riverTop = arena.getHeight() / 2 - 1; // 15
        Position riverTile = new Position(5, riverTop);

        // Execute
        Result<?> result = invokeValidateDeploySide(bottomPlayer, riverTile);

        // Verify
        assertFalse(result.isSuccess(), "Should fail when deploying on river top");
        assertEquals("Cannot deploy on the river.", result.getMessage());
    }

    @Test
    void validateDeploySide_deployOnRiverBottom_returnsFailure() throws Exception {
        // Setup: River bottom row = riverTop + 1 = 16
        int riverBottom = arena.getHeight() / 2; // 16
        Position riverTile = new Position(5, riverBottom);

        // Execute
        Result<?> result = invokeValidateDeploySide(bottomPlayer, riverTile);

        // Verify
        assertFalse(result.isSuccess(), "Should fail when deploying on river bottom");
        assertEquals("Cannot deploy on the river.", result.getMessage());
    }

    @Test
    void validateDeploySide_bottomPlayerOnTopSide_returnsFailure() throws Exception {
        // Setup: Bottom player trying to deploy on top side
        // River top = 15, so bottom player can only deploy on Y <= 14
        // Y = 5 is clearly on top side (above river)
        Position invalidTile = new Position(5, 5);

        // Execute
        Result<?> result = invokeValidateDeploySide(bottomPlayer, invalidTile);

        // Verify
        assertFalse(result.isSuccess(), "Bottom player should not deploy on top side");
        assertEquals("Player 1 can only deploy on the bottom side.", result.getMessage());
    }

    @Test
    void validateDeploySide_topPlayerOnBottomSide_returnsFailure() throws Exception {
        // Setup: Top player trying to deploy on bottom side
        // River bottom = 16, so top player can only deploy on Y >= 17
        // Y = 20 is clearly on bottom side (below river)
        Position invalidTile = new Position(5, 20);

        // Execute
        Result<?> result = invokeValidateDeploySide(topPlayer, invalidTile);

        // Verify
        assertFalse(result.isSuccess(), "Top player should not deploy on bottom side");
        assertEquals("Player 2 can only deploy on the top side.", result.getMessage());
    }

    @Test
    void validateDeploySide_bottomPlayerOnBottomSide_returnsSuccess() throws Exception {
        // Setup: Bottom player deploying on valid bottom side
        // Bottom side: Y >= riverBottom + 1 = 17
        int riverBottom = arena.getHeight() / 2; // 16
        Position validTile = new Position(5, riverBottom + 1); // Y = 17, valid for bottom player

        // Execute
        Result<?> result = invokeValidateDeploySide(bottomPlayer, validTile);

        // Verify
        assertTrue(result.isSuccess(), "Bottom player should deploy on bottom side");
    }

    @Test
    void validateDeploySide_topPlayerOnTopSide_returnsSuccess() throws Exception {
        // Setup: Top player deploying on valid top side
        // Top side: Y <= riverTop - 1 = 14
        int riverTop = arena.getHeight() / 2 - 1; // 15
        Position validTile = new Position(5, riverTop - 1); // Y = 14, valid for top player

        // Execute
        Result<?> result = invokeValidateDeploySide(topPlayer, validTile);

        // Verify
        assertTrue(result.isSuccess(), "Top player should deploy on top side");
    }

    @Test
    void validateDeploySide_bottomPlayerAtBoundary_returnsSuccess() throws Exception {
        // Setup: Bottom player at the boundary (Y = riverBottom + 1 = 17)
        int riverBottom = arena.getHeight() / 2; // 16
        Position boundaryTile = new Position(5, riverBottom + 1); // Y = 17

        // Execute
        Result<?> result = invokeValidateDeploySide(bottomPlayer, boundaryTile);

        // Verify
        assertTrue(result.isSuccess(), "Boundary position should be valid for bottom player");
    }

    @Test
    void validateDeploySide_topPlayerAtBoundary_returnsSuccess() throws Exception {
        // Setup: Top player at the boundary (Y = riverTop - 1 = 14)
        int riverTop = arena.getHeight() / 2 - 1; // 15
        Position boundaryTile = new Position(5, riverTop - 1); // Y = 14

        // Execute
        Result<?> result = invokeValidateDeploySide(topPlayer, boundaryTile);

        // Verify
        assertTrue(result.isSuccess(), "Boundary position should be valid for top player");
    }

    // Helper method to invoke private validateDeploySide using reflection
    private Result<?> invokeValidateDeploySide(Player acting, Position tile) throws Exception {
        if (gameView == null) {
            throw new IllegalStateException("LocalPvPGameView not initialized - JavaFX may not be available");
        }
        return invokeValidateDeploySide(gameView, acting, tile);
    }

    private Result<?> invokeValidateDeploySide(LocalPvPGameView view, Player acting, Position tile) throws Exception {
        if (view == null) {
            throw new IllegalStateException("LocalPvPGameView is null - JavaFX may not be available");
        }
        Method method = LocalPvPGameView.class.getDeclaredMethod("validateDeploySide", Player.class, Position.class);
        method.setAccessible(true);
        return (Result<?>) method.invoke(view, acting, tile);
    }
}