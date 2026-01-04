package application.challenge;

import application.ArenaLayoutService;
import application.DeckService;
import application.MatchService;
import kuroyale.domain.*;
import kuroyale.infrastructure.PlayerProfileRepository;
import kuroyale.support.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import application.challenge.ChallengeService.ChallengeCompletion;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ChallengeService.completeChallenge() method.
 * 
 * Test cases cover:
 * - Invalid session (null or missing match)
 * - Unknown challenge ID
 * - Match not finished
 * - Loss scenario (no stars, no gold)
 * - Win scenario with 1 star (base win)
 * - Win scenario with 2 stars (within time limit)
 * - Win scenario with 3 stars (no damage taken)
 * - Progress updates on win
 * - Next challenge unlocking
 */
public class ChallengeServiceTest {

    private ChallengeService challengeService;
    private FakePlayerProfileRepository profileRepository;
    private FakeDeckService deckService;
    private FakeMatchService matchService;
    private FakeArenaLayoutService arenaLayoutService;

    @BeforeEach
    void setUp() {
        profileRepository = new FakePlayerProfileRepository();
        deckService = new FakeDeckService();
        matchService = new FakeMatchService();
        arenaLayoutService = new FakeArenaLayoutService();
        challengeService = new ChallengeService(
            profileRepository,
            deckService,
            matchService,
            arenaLayoutService
        );
    }

    @Test
    void completeChallenge_nullSession_returnsFailure() {
        // Execute
        Result<ChallengeCompletion> result = challengeService.completeChallenge(null);

        // Verify
        assertFalse(result.isSuccess(), "Should fail with null session");
        assertEquals("Challenge session is invalid.", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    void completeChallenge_sessionWithNullMatch_returnsFailure() {
        // Setup
        ChallengeSession session = new ChallengeSession("1", null);

        // Execute
        Result<ChallengeCompletion> result = challengeService.completeChallenge(session);

        // Verify
        assertFalse(result.isSuccess(), "Should fail with null match");
        assertEquals("Challenge session is invalid.", result.getMessage());
    }

    @Test
    void completeChallenge_matchNotFinished_returnsFailure() {
        // Setup
        Match match = createMatch(false, false); // Not finished, no outcome
        ChallengeSession session = new ChallengeSession("1", match);

        // Execute
        Result<ChallengeCompletion> result = challengeService.completeChallenge(session);

        // Verify
        assertFalse(result.isSuccess(), "Should fail when match not finished");
        assertEquals("Match is not finished yet.", result.getMessage());
    }

    @Test
    void completeChallenge_playerLoss_returnsZeroStarsAndNoGold() {
        // Setup: Player loses
        Match match = createMatch(true, false); // Finished, player lost
        ChallengeSession session = new ChallengeSession("1", match);
        setupInitialProfile("1", true); // Challenge 1 unlocked

        // Execute
        Result<ChallengeCompletion> result = challengeService.completeChallenge(session);

        // Verify
        assertTrue(result.isSuccess(), "Should succeed even on loss");
        ChallengeCompletion completion = result.getData();
        assertNotNull(completion);
        assertFalse(completion.isWin(), "Should indicate loss");
        assertEquals(0, completion.getStars(), "Loss should give 0 stars");
        assertEquals(0, completion.getGoldAwarded(), "Loss should give 0 gold");
        
        // Verify no progress updates on loss
        PlayerProfile profile = profileRepository.getLastSaved();
        ChallengeProgress progress = profile.getChallengeProgress().get("1");
        assertNotNull(progress);
        assertEquals(0, progress.getCompletions(), "Completions should not increment on loss");
    }

    @Test
    void completeChallenge_playerWin_baseWin_returnsOneStar() {
        // Setup: Player wins, over time limit, took damage
        // Challenge 1 has time limit of 180 seconds, so we need > 180 to get only 1 star
        Match match = createMatch(true, true); // Finished, player won
        match.setElapsedSeconds(200.0); // Over time limit (180 seconds)
        ChallengeSession session = new ChallengeSession("1", match);
        setupInitialProfile("1", true);
        setupTowerDamage(session, true); // Player took damage

        // Execute
        Result<ChallengeCompletion> result = challengeService.completeChallenge(session);

        // Verify
        assertTrue(result.isSuccess());
        ChallengeCompletion completion = result.getData();
        assertTrue(completion.isWin());
        assertEquals(1, completion.getStars(), "Base win over time limit should give 1 star");
        assertTrue(completion.getGoldAwarded() > 0, "Win should award gold");
        
        // Verify progress updated
        PlayerProfile profile = profileRepository.getLastSaved();
        ChallengeProgress progress = profile.getChallengeProgress().get("1");
        assertEquals(1, progress.getCompletions(), "Completions should increment");
        assertEquals(1, progress.getBestStars(), "Best stars should be 1");
    }

    @Test
    void completeChallenge_playerWinWithinTimeLimit_returnsTwoStars() {
        // Setup: Player wins within time limit, took damage
        // Challenge 1 has time limit of 180 seconds
        Match match = createMatch(true, true);
        match.setElapsedSeconds(120.0); // Within 180 second limit
        ChallengeSession session = new ChallengeSession("1", match);
        setupInitialProfile("1", true);
        setupTowerDamage(session, true); // Took damage, so can't get 3 stars

        // Execute
        Result<ChallengeCompletion> result = challengeService.completeChallenge(session);

        // Verify
        assertTrue(result.isSuccess());
        ChallengeCompletion completion = result.getData();
        assertTrue(completion.isWin());
        assertEquals(2, completion.getStars(), "Win within time limit should give 2 stars");
        
        // Verify best stars updated
        PlayerProfile profile = profileRepository.getLastSaved();
        ChallengeProgress progress = profile.getChallengeProgress().get("1");
        assertEquals(2, progress.getBestStars(), "Best stars should be 2");
    }

    @Test
    void completeChallenge_playerWinNoDamage_returnsThreeStars() {
        // Setup: Player wins, no damage taken
        Match match = createMatch(true, true);
        match.setElapsedSeconds(200.0); // Over time limit, but no damage
        ChallengeSession session = new ChallengeSession("1", match);
        setupInitialProfile("1", true);
        setupTowerDamage(session, false); // No damage taken

        // Execute
        Result<ChallengeCompletion> result = challengeService.completeChallenge(session);

        // Verify
        assertTrue(result.isSuccess());
        ChallengeCompletion completion = result.getData();
        assertTrue(completion.isWin());
        assertEquals(3, completion.getStars(), "Win with no damage should give 3 stars");
        
        // Verify best stars updated
        PlayerProfile profile = profileRepository.getLastSaved();
        ChallengeProgress progress = profile.getChallengeProgress().get("1");
        assertEquals(3, progress.getBestStars(), "Best stars should be 3");
    }

    @Test
    void completeChallenge_playerWin_unlocksNextChallenge() {
        // Setup: Player wins challenge 1
        Match match = createMatch(true, true);
        ChallengeSession session = new ChallengeSession("1", match);
        setupInitialProfile("1", true);
        setupTowerDamage(session, true);

        // Execute
        Result<ChallengeCompletion> result = challengeService.completeChallenge(session);

        // Verify
        assertTrue(result.isSuccess());
        PlayerProfile profile = profileRepository.getLastSaved();
        ChallengeProgress nextProgress = profile.getChallengeProgress().get("2");
        assertNotNull(nextProgress, "Next challenge progress should exist");
        assertTrue(nextProgress.isUnlocked(), "Next challenge should be unlocked");
    }

    @Test
    void completeChallenge_playerWin_updatesBestTime() {
        // Setup: Player wins with good time
        Match match = createMatch(true, true);
        match.setElapsedSeconds(90.0); // Good time
        ChallengeSession session = new ChallengeSession("1", match);
        setupInitialProfile("1", true);
        setupTowerDamage(session, true);
        
        // Set initial best time to be worse
        PlayerProfile profile = profileRepository.load();
        ChallengeProgress progress = profile.getChallengeProgress().get("1");
        progress.setBestTimeSeconds(200.0); // Worse time
        profileRepository.save(profile);

        // Execute
        Result<ChallengeCompletion> result = challengeService.completeChallenge(session);

        // Verify
        assertTrue(result.isSuccess());
        PlayerProfile savedProfile = profileRepository.getLastSaved();
        ChallengeProgress savedProgress = savedProfile.getChallengeProgress().get("1");
        assertEquals(90.0, savedProgress.getBestTimeSeconds(), 0.1, "Best time should be updated");
    }

    // Helper methods

    private Match createMatch(boolean finished, boolean playerWins) {
        Player player = new Player("Player", new Deck(new ArrayList<>()), 0);
        Player opponent = new Player("Bot", new Deck(new ArrayList<>()), 0);
        Arena arena = new Arena();
        Match match = new Match(player, opponent, arena);
        
        if (finished) {
            match.setElapsedSeconds(360.0); // Match duration
            // Set outcome using reflection for testing
            try {
                java.lang.reflect.Field outcomeField = Match.class.getDeclaredField("outcome");
                outcomeField.setAccessible(true);
                TowerOwner winner = playerWins ? TowerOwner.PLAYER : TowerOwner.OPPONENT;
                MatchOutcome outcome = new MatchOutcome(winner, playerWins ? 3 : 0, playerWins ? 0 : 3, "TEST");
                outcomeField.set(match, outcome);
            } catch (Exception e) {
                throw new RuntimeException("Failed to set match outcome for testing", e);
            }
        }
        
        return match;
    }

    private void setupInitialProfile(String challengeId, boolean unlocked) {
        PlayerProfile profile = new PlayerProfile("TestPlayer", 1000);
        Map<String, ChallengeProgress> progressMap = new HashMap<>();
        ChallengeProgress progress = new ChallengeProgress(challengeId, unlocked);
        progressMap.put(challengeId, progress);
        profile.setChallengeProgress(progressMap);
        profileRepository.setProfile(profile);
    }

    private void setupTowerDamage(ChallengeSession session, boolean tookDamage) {
        // Record initial tower HP
        Match match = session.getMatch();
        Arena arena = match.getArena();
        
        // Add a player tower
        Tower tower = new Tower();
        tower.setOwner(TowerOwner.PLAYER);
        tower.setType(TowerType.CROWN);
        tower.setPosition(new Position(5, 5));
        tower.setHp(100);
        arena.getTowers().add(tower);
        
        // Record initial HP
        String key = tower.getType() + "@5,5";
        session.recordInitialTowerHp(key, 100);
        
        // Set final HP based on damage
        if (tookDamage) {
            tower.setHp(80); // Took 20 damage
        }
        // Otherwise, HP stays at 100 (no damage)
    }

    // Test doubles

    private static class FakePlayerProfileRepository extends PlayerProfileRepository {
        private PlayerProfile profile;
        private PlayerProfile lastSaved;

        public void setProfile(PlayerProfile profile) {
            this.profile = profile;
        }

        public PlayerProfile getLastSaved() {
            return lastSaved;
        }

        @Override
        public PlayerProfile load() {
            return profile != null ? profile : new PlayerProfile("Default", 1000);
        }

        @Override
        public void save(PlayerProfile profile) {
            this.lastSaved = profile;
            this.profile = profile;
        }
    }

    private static class FakeDeckService extends DeckService {
        
        public FakeDeckService() {
            super(null, null);
        }

        @Override
        public Deck loadDeck() {
            return new Deck(new ArrayList<>());
        }

        @Override
        public Deck buildDefaultDeck() {
            return new Deck(new ArrayList<>());
        }
    }

    private static class FakeMatchService extends MatchService {
        
        public FakeMatchService() {
            super(null);
        }

        @Override
        public Match createMatch(Player player, Player opponent, ArenaLayout layout) {
            return new Match(player, opponent, new Arena());
        }
    }

    private static class FakeArenaLayoutService extends ArenaLayoutService {
        public FakeArenaLayoutService() {
            super(new FakeArenaRepository());
        }

        @Override
        public ArenaLayout getActiveLayout() {
            return ArenaLayout.defaultLayout();
        }
    }

    private static class FakeArenaRepository extends kuroyale.infrastructure.ArenaRepository {
        @Override
        public java.util.List<kuroyale.domain.ArenaLayout> loadAll() {
            return new java.util.ArrayList<>();
        }

        @Override
        public void saveAll(java.util.List<kuroyale.domain.ArenaLayout> layouts) {
            // No-op for testing
        }
    }
}