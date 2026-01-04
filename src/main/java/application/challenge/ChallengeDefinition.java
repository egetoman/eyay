package application.challenge;

import kuroyale.domain.CardCostPolicy;
import kuroyale.domain.Deck;
import kuroyale.support.Result;

/**
 * Strategy interface for Challenge rules (Phase 2 Feature 4).
 * Each challenge encapsulates deck validation, optional match modifiers, and star evaluation rules.
 */
public interface ChallengeDefinition {
    String getId();
    String getName();
    String getRulesSummary();
    int getRewardGold();

    /**
     * For 2-star rating: complete within this time. Use 0 to disable.
     */
    int getTimeLimitSeconds();

    /**
     * Validates the player's deck before starting the match.
     */
    Result<?> validateDeck(Deck deck);

    /**
     * Optional: modify card costs during the match (e.g., Spell Barrage).
     * Return null to keep default costs.
     */
    CardCostPolicy buildCostPolicy();
}


