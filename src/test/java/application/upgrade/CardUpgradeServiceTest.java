package application.upgrade;

import application.CardUpgradeService;
import application.DefaultUpgradePolicy;
import kuroyale.domain.Card;
import kuroyale.domain.CardProgression;
import kuroyale.domain.CardStats;
import kuroyale.domain.CardTarget;
import kuroyale.domain.CardType;
import kuroyale.domain.PlayerProfile;
import kuroyale.domain.Rarity;
import kuroyale.infrastructure.CardCatalogRepository;
import kuroyale.infrastructure.PlayerProfileRepository;
import kuroyale.support.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CardUpgradeServiceTest {

    private CardUpgradeService upgradeService;
    private FakePlayerProfileRepository profileRepository;
    private FakeCardCatalogRepository cardCatalogRepository;
    private DefaultUpgradePolicy upgradePolicy;

    @BeforeEach
    void setUp() {
        profileRepository = new FakePlayerProfileRepository();
        cardCatalogRepository = new FakeCardCatalogRepository();
        upgradePolicy = new DefaultUpgradePolicy();
        upgradeService = new CardUpgradeService(profileRepository, cardCatalogRepository, upgradePolicy);
    }

    @Test
    void upgradeCard_successfulUpgrade_level1ToLevel2() {
        // Setup: Player has enough gold, card exists at level 1
        String cardId = "card_knight";
        PlayerProfile profile = new PlayerProfile("TestPlayer", 1000);
        CardProgression progression = new CardProgression(cardId, 1, Rarity.COMMON);
        profile.setCardProgression(cardId, progression);
        profileRepository.setProfile(profile);

        Card knight = new Card(cardId, "Knight", 3, CardType.TROOP, 
            new CardStats(600, 75, 0, 0, 0), CardTarget.GROUND, "Test card");
        cardCatalogRepository.addCard(knight);

        // Execute
        Result<CardProgression> result = upgradeService.upgradeCard(cardId);

        // Verify
        assertTrue(result.isSuccess(), "Upgrade should succeed");
        CardProgression updated = result.getData();
        assertNotNull(updated);
        assertEquals(2, updated.getLevel(), "Card should be upgraded to level 2");
        
        // Verify gold was deducted (Common L1->L2: 50 * (1+1) = 100)
        PlayerProfile savedProfile = profileRepository.getLastSaved();
        assertNotNull(savedProfile);
        assertEquals(900, savedProfile.getGold(), "Gold should be reduced by 100");
        
        // Verify progression was saved
        CardProgression savedProgression = savedProfile.getCardProgression(cardId);
        assertNotNull(savedProgression);
        assertEquals(2, savedProgression.getLevel());
    }

    @Test
    void upgradeCard_cardNotFound_returnsFailure() {
        // Setup: Card doesn't exist in catalog
        String invalidCardId = "card_nonexistent";
        PlayerProfile profile = new PlayerProfile("TestPlayer", 1000);
        profileRepository.setProfile(profile);

        // Execute
        Result<CardProgression> result = upgradeService.upgradeCard(invalidCardId);

        // Verify
        assertFalse(result.isSuccess(), "Upgrade should fail for non-existent card");
        assertEquals("Card not found", result.getMessage());
        
        // Verify no changes were made
        PlayerProfile savedProfile = profileRepository.getLastSaved();
        if (savedProfile != null) {
            assertEquals(1000, savedProfile.getGold(), "Gold should not change");
        }
    }

    @Test
    void upgradeCard_insufficientGold_returnsFailure() {
        // Setup: Player has insufficient gold for upgrade
        String cardId = "card_knight";
        PlayerProfile profile = new PlayerProfile("TestPlayer", 50); // Not enough (needs 100)
        CardProgression progression = new CardProgression(cardId, 1, Rarity.COMMON);
        profile.setCardProgression(cardId, progression);
        profileRepository.setProfile(profile);

        Card knight = new Card(cardId, "Knight", 3, CardType.TROOP, 
            new CardStats(600, 75, 0, 0, 0), CardTarget.GROUND, "Test card");
        cardCatalogRepository.addCard(knight);

        // Execute
        Result<CardProgression> result = upgradeService.upgradeCard(cardId);

        // Verify
        assertFalse(result.isSuccess(), "Upgrade should fail with insufficient gold");
        assertEquals("Insufficient gold", result.getMessage());
        
        // Verify no changes were made
        PlayerProfile savedProfile = profileRepository.getLastSaved();
        if (savedProfile != null) {
            assertEquals(50, savedProfile.getGold(), "Gold should not change");
            CardProgression savedProgression = savedProfile.getCardProgression(cardId);
            if (savedProgression != null) {
                assertEquals(1, savedProgression.getLevel(), "Level should not change");
            }
        }
    }

    @Test
    void upgradeCard_maxLevelReached_returnsFailure() {
        // Setup: Card is already at max level (3)
        String cardId = "card_knight";
        PlayerProfile profile = new PlayerProfile("TestPlayer", 1000);
        CardProgression progression = new CardProgression(cardId, 3, Rarity.COMMON);
        profile.setCardProgression(cardId, progression);
        profileRepository.setProfile(profile);

        Card knight = new Card(cardId, "Knight", 3, CardType.TROOP, 
            new CardStats(600, 75, 0, 0, 0), CardTarget.GROUND, "Test card");
        cardCatalogRepository.addCard(knight);

        // Execute
        Result<CardProgression> result = upgradeService.upgradeCard(cardId);

        // Verify
        assertFalse(result.isSuccess(), "Upgrade should fail at max level");
        assertEquals("Card is already at max level", result.getMessage());
        
        // Verify no changes were made
        PlayerProfile savedProfile = profileRepository.getLastSaved();
        if (savedProfile != null) {
            assertEquals(1000, savedProfile.getGold(), "Gold should not change");
            CardProgression savedProgression = savedProfile.getCardProgression(cardId);
            if (savedProgression != null) {
                assertEquals(3, savedProgression.getLevel(), "Level should remain at 3");
            }
        }
    }

    @Test
    void upgradeCard_saveFailure_revertsChanges() {
        // Setup: Profile exists, card is valid, but save will fail
        String cardId = "card_knight";
        PlayerProfile profile = new PlayerProfile("TestPlayer", 1000);
        CardProgression progression = new CardProgression(cardId, 1, Rarity.COMMON);
        profile.setCardProgression(cardId, progression);
        profileRepository.setProfile(profile);
        profileRepository.setSaveShouldFail(true); // Simulate save failure

        Card knight = new Card(cardId, "Knight", 3, CardType.TROOP, 
            new CardStats(600, 75, 0, 0, 0), CardTarget.GROUND, "Test card");
        cardCatalogRepository.addCard(knight);

        // Execute
        Result<CardProgression> result = upgradeService.upgradeCard(cardId);

        // Verify
        assertFalse(result.isSuccess(), "Upgrade should fail when save fails");
        assertTrue(result.getMessage().contains("Save failed - reverted"), 
            "Error message should indicate rollback");
        
        // Verify rollback: gold and level should be reverted
        PlayerProfile profileAfter = profileRepository.load();
        assertEquals(1000, profileAfter.getGold(), "Gold should be reverted to original");
        CardProgression revertedProgression = profileAfter.getCardProgression(cardId);
        assertNotNull(revertedProgression);
        assertEquals(1, revertedProgression.getLevel(), "Level should be reverted to 1");
    }

    // Test doubles (Fake implementations for testing)

    private static class FakePlayerProfileRepository extends PlayerProfileRepository {
        private PlayerProfile profile;
        private PlayerProfile lastSaved;
        private boolean saveShouldFail = false;

        public FakePlayerProfileRepository() {
            // No need to call super() - we override all methods
        }

        public void setProfile(PlayerProfile profile) {
            this.profile = profile;
        }

        public PlayerProfile getLastSaved() {
            return lastSaved;
        }

        public void setSaveShouldFail(boolean shouldFail) {
            this.saveShouldFail = shouldFail;
        }

        @Override
        public PlayerProfile load() {
            return profile != null ? profile : new PlayerProfile("Default", 1000);
        }

        @Override
        public void save(PlayerProfile profile) {
            if (saveShouldFail) {
                throw new RuntimeException("Simulated save failure");
            }
            this.lastSaved = profile;
            this.profile = profile; // Update stored profile
        }
    }

    private static class FakeCardCatalogRepository extends CardCatalogRepository {
        private final List<Card> cards = new ArrayList<>();

        public FakeCardCatalogRepository() {
            // No need to call super() - we override all methods
        }

        public void addCard(Card card) {
            cards.add(card);
        }

        @Override
        public List<Card> findAll() {
            return new ArrayList<>(cards);
        }
    }
}

