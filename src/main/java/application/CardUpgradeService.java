package application;

import kuroyale.domain.Card;
import kuroyale.domain.CardProgression;
import kuroyale.domain.CardStats;
import kuroyale.domain.PlayerProfile;
import kuroyale.infrastructure.CardCatalogRepository;
import kuroyale.infrastructure.PlayerProfileRepository;
import kuroyale.support.Result;

public class CardUpgradeService {
    
    private final PlayerProfileRepository profileRepository;
    private final CardCatalogRepository cardCatalogRepository;
    private final UpgradePolicy upgradePolicy;
    
    public CardUpgradeService(PlayerProfileRepository profileRepository, 
                              CardCatalogRepository cardCatalogRepository,
                              UpgradePolicy upgradePolicy) {
        this.profileRepository = profileRepository;
        this.cardCatalogRepository = cardCatalogRepository;
        this.upgradePolicy = upgradePolicy;
    }
    
    public PlayerProfile getPlayerProfile() {
        return profileRepository.load();
    }
    
    /**
     * Upgrades a card to the next level if all validations pass.
     * 
     * Requires:
     *   - cardId is not null
     *   - PlayerProfile exists and can be loaded
     *   - CardCatalogRepository can be accessed
     * 
     * Modifies:
     *   - PlayerProfile: decreases gold balance by upgrade cost
     *   - CardProgression: increases level by 1 (if upgrade succeeds)
     *   - PlayerProfile.cardProgressions map: adds/updates card progression entry
     *   - Persistent storage: saves updated PlayerProfile (if save succeeds)
     * 
     * Effects:
     *   - If card is not found: returns Result.fail("Card not found")
     *   - If card is at max level: returns Result.fail("Card is already at max level")
     *   - If insufficient gold: returns Result.fail("Insufficient gold")
     *   - If save fails: reverts gold and level changes, returns Result.fail with error message
     *   - If all validations pass: upgrades card, deducts gold, saves profile, returns Result.ok with updated CardProgression
     */
    public Result<CardProgression> upgradeCard(String cardId) {
        PlayerProfile profile = profileRepository.load();
        Card card = findCard(cardId);
        
        if (card == null) {
            return Result.fail("Card not found");
        }
        
        CardProgression progression = profile.getCardProgression(cardId);
        kuroyale.domain.Rarity resolvedRarity = resolveRarity(card);
        if (progression == null) {
            progression = new CardProgression(cardId, 1, resolvedRarity);
        } else if (progression.getRarity() == null || progression.getRarity() != resolvedRarity) {
            progression.setRarity(resolvedRarity);
        }
        
        if (!upgradePolicy.canUpgrade(progression.getLevel())) {
            return Result.fail("Card is already at max level");
        }
        
        int upgradeCost = upgradePolicy.getUpgradeCost(progression.getRarity(), progression.getLevel());
        if (!profile.hasEnoughGold(upgradeCost)) {
            return Result.fail("Insufficient gold");
        }
        
        // Perform upgrade
        profile.spendGold(upgradeCost);
        progression.upgrade();
        profile.setCardProgression(cardId, progression);
        
        try {
            profileRepository.save(profile);
            return Result.ok(progression);
        } catch (Exception e) {
            // Revert changes
            profile.addGold(upgradeCost);
            progression.setLevel(progression.getLevel() - 1);
            return Result.fail("Save failed - reverted: " + e.getMessage());
        }
    }
    
    public UpgradePreview getUpgradePreview(String cardId) {
        PlayerProfile profile = profileRepository.load();
        Card card = findCard(cardId);
        
        if (card == null) {
            return null;
        }
        
        CardProgression progression = profile.getCardProgression(cardId);
        kuroyale.domain.Rarity resolvedRarity = resolveRarity(card);
        if (progression == null) {
            progression = new CardProgression(cardId, 1, resolvedRarity);
        } else if (progression.getRarity() == null || progression.getRarity() != resolvedRarity) {
            progression.setRarity(resolvedRarity);
        }
        
        CardStats baseStats = card.getStats();
        CardStats currentStats = upgradePolicy.getStatsCalculator()
            .calculateStatsForLevel(baseStats, progression.getLevel());
        CardStats nextStats = upgradePolicy.getStatsCalculator()
            .previewNextLevel(baseStats, progression.getLevel());
        int upgradeCost = upgradePolicy.getUpgradeCost(progression.getRarity(), progression.getLevel());
        boolean canUpgrade = upgradePolicy.canUpgrade(progression.getLevel()) && 
                            profile.hasEnoughGold(upgradeCost);
        
        return new UpgradePreview(card, progression, currentStats, nextStats, upgradeCost, canUpgrade);
    }
    
    public java.util.List<Card> getAllCards() {
        return cardCatalogRepository.findAll();
    }
    
    private Card findCard(String cardId) {
        return cardCatalogRepository.findAll().stream()
            .filter(c -> c.getId().equals(cardId))
            .findFirst()
            .orElse(null);
    }

    private kuroyale.domain.Rarity resolveRarity(Card card) {
        if (card == null) {
            return kuroyale.domain.Rarity.COMMON;
        }
        return kuroyale.domain.CardRarityCatalog.rarityForCardId(card.getId());
    }
    
    public static class UpgradePreview {
        private final Card card;
        private final CardProgression progression;
        private final CardStats currentStats;
        private final CardStats nextStats;
        private final int upgradeCost;
        private final boolean canUpgrade;
        
        public UpgradePreview(Card card, CardProgression progression, 
                            CardStats currentStats, CardStats nextStats, 
                            int upgradeCost, boolean canUpgrade) {
            this.card = card;
            this.progression = progression;
            this.currentStats = currentStats;
            this.nextStats = nextStats;
            this.upgradeCost = upgradeCost;
            this.canUpgrade = canUpgrade;
        }
        
        public Card getCard() { return card; }
        public CardProgression getProgression() { return progression; }
        public CardStats getCurrentStats() { return currentStats; }
        public CardStats getNextStats() { return nextStats; }
        public int getUpgradeCost() { return upgradeCost; }
        public boolean canUpgrade() { return canUpgrade; }
    }
}

