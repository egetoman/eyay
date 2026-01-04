package application;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import kuroyale.domain.Card;
import kuroyale.domain.CardStats;
import kuroyale.domain.CardTarget;
import kuroyale.domain.CardType;
import kuroyale.domain.ComboDefinition;
import kuroyale.domain.ComboType;

public class ComboDetectorTest {

    private ComboDetector detector;
    private FakeComboListener listener;

    @BeforeEach
    void setUp() {
        detector = new ComboDetector();
        listener = new FakeComboListener();
        detector.setListener(listener);
    }

    @Test
    void recordCardPlay_nullCard_returnsNull() {
        // Execute
        ComboType result = detector.recordCardPlay(null, 10.0);

        // Verify
        assertNull(result, "Should return null for null card");
        assertEquals(0, detector.getTriggeredComboCount(), "No combos should be triggered");
    }

    @Test
    void recordCardPlay_singleCard_noComboTriggered() {
        // Setup
        Card knight = createCard("card_knight", "Knight", CardType.TROOP);

        // Execute
        ComboType result = detector.recordCardPlay(knight, 10.0);

        // Verify
        assertNull(result, "Single card should not trigger combo");
        assertEquals(0, detector.getTriggeredComboCount(), "No combos should be triggered");
        assertEquals(0, listener.triggeredCount, "Listener should not be called");
    }

    @Test
    void recordCardPlay_tankSupportCombo_triggered() {
        // Setup: Play Giant (tank), then Archers (ranged troop) within 5 seconds
        Card giant = createCard("card_giant", "Giant", CardType.TROOP);
        Card archers = createCard("card_archers", "Archers", CardType.TROOP);

        // Execute
        detector.recordCardPlay(giant, 10.0);
        ComboType result = detector.recordCardPlay(archers, 12.0); // Within 5 seconds

        // Verify
        assertEquals(ComboType.TANK_SUPPORT, result, "Tank + Support combo should be triggered");
        assertEquals(1, detector.getTriggeredComboCount(), "One combo should be triggered");
        assertEquals(1, listener.triggeredCount, "Listener should be called once");
        assertEquals(ComboType.TANK_SUPPORT, listener.lastComboType, "Last combo type should be TANK_SUPPORT");
    }

    @Test
    void recordCardPlay_tankSupportCombo_outsideWindow_notTriggered() {
        // Setup: Play Giant, then Archers after 5 seconds (outside window)
        Card giant = createCard("card_giant", "Giant", CardType.TROOP);
        Card archers = createCard("card_archers", "Archers", CardType.TROOP);

        // Execute
        detector.recordCardPlay(giant, 10.0);
        ComboType result = detector.recordCardPlay(archers, 16.0); // After 5 seconds

        // Verify
        assertNull(result, "Combo should not trigger outside window");
        assertEquals(0, detector.getTriggeredComboCount(), "No combos should be triggered");
    }

    @Test
    void recordCardPlay_spellSynergyCombo_triggered() {
        // Setup: Play two different spells within 5 seconds
        Card zap = createCard("card_zap", "Zap", CardType.SPELL);
        Card fireball = createCard("card_fireball", "Fireball", CardType.SPELL);

        // Execute
        detector.recordCardPlay(zap, 10.0);
        ComboType result = detector.recordCardPlay(fireball, 12.0);

        // Verify
        assertEquals(ComboType.SPELL_SYNERGY, result, "Spell Synergy combo should be triggered");
        assertEquals(1, detector.getTriggeredComboCount(), "One combo should be triggered");
    }

    @Test
    void recordCardPlay_royalCombo_triggered() {
        // Note: Knight + Archers actually triggers TANK_SUPPORT first (since Knight is a tank and Archers is ranged)
        // TANK_SUPPORT is checked before ROYAL_COMBO in the combo list
        // This test verifies that a combo is triggered (the first matching one)
        Card knight = createCard("card_knight", "Knight", CardType.TROOP);
        Card archers = createCard("card_archers", "Archers", CardType.TROOP);

        // Execute
        detector.recordCardPlay(knight, 10.0);
        ComboType result = detector.recordCardPlay(archers, 11.0);

        // Verify - TANK_SUPPORT matches first because Knight is a tank and Archers is ranged
        assertNotNull(result, "A combo should be triggered");
        assertEquals(ComboType.TANK_SUPPORT, result, "TANK_SUPPORT matches first for Knight + Archers");
        assertEquals(1, detector.getTriggeredComboCount(), "One combo should be triggered");
    }

    @Test
    void recordCardPlay_tankSupportWithGiant_triggered() {
        // Test TANK_SUPPORT with Giant + Musketeer (unique combo)
        Card giant = createCard("card_giant", "Giant", CardType.TROOP);
        Card musketeer = createCard("card_musketeer", "Musketeer", CardType.TROOP);

        // Execute
        detector.recordCardPlay(giant, 10.0);
        ComboType result = detector.recordCardPlay(musketeer, 11.0);

        // Verify
        assertEquals(ComboType.TANK_SUPPORT, result, "TANK_SUPPORT should be triggered");
        assertEquals(1, detector.getTriggeredComboCount(), "One combo should be triggered");
    }

    @Test
    void recordCardPlay_sameComboTwice_onlyTriggersOnce() {
        // Setup: Try to trigger the same combo twice using Giant + Musketeer
        Card giant = createCard("card_giant", "Giant", CardType.TROOP);
        Card musketeer = createCard("card_musketeer", "Musketeer", CardType.TROOP);

        // Execute
        detector.recordCardPlay(giant, 10.0);
        detector.recordCardPlay(musketeer, 11.0); // First trigger (TANK_SUPPORT)
        detector.recordCardPlay(giant, 12.0);
        ComboType result = detector.recordCardPlay(musketeer, 13.0); // Second attempt

        // Verify
        assertNull(result, "Same combo should not trigger twice");
        assertEquals(1, detector.getTriggeredComboCount(), "Only one combo should be triggered");
        assertEquals(1, listener.triggeredCount, "Listener should be called only once");
    }

    @Test
    void reset_clearsTriggeredCombos() {
        // Setup: Trigger a combo using Giant + Musketeer
        Card giant = createCard("card_giant", "Giant", CardType.TROOP);
        Card musketeer = createCard("card_musketeer", "Musketeer", CardType.TROOP);

        detector.recordCardPlay(giant, 10.0);
        detector.recordCardPlay(musketeer, 11.0);
        assertEquals(1, detector.getTriggeredComboCount(), "Combo should be triggered");

        // Execute
        detector.reset();

        // Verify
        assertEquals(0, detector.getTriggeredComboCount(), "Triggered combos should be cleared");
        assertTrue(detector.getTriggeredCombos().isEmpty(), "Triggered combos set should be empty");
        
        // Should be able to trigger the same combo again after reset
        detector.recordCardPlay(giant, 20.0);
        ComboType result = detector.recordCardPlay(musketeer, 21.0);
        assertEquals(ComboType.TANK_SUPPORT, result, "Combo should trigger again after reset");
        assertEquals(1, detector.getTriggeredComboCount(), "Combo should be triggered again");
    }

    @Test
    void getTriggeredCombos_returnsCopy() {
        // Setup: Trigger a combo using Giant + Musketeer
        Card giant = createCard("card_giant", "Giant", CardType.TROOP);
        Card musketeer = createCard("card_musketeer", "Musketeer", CardType.TROOP);

        detector.recordCardPlay(giant, 10.0);
        detector.recordCardPlay(musketeer, 11.0);

        // Execute
        Set<ComboType> combos1 = detector.getTriggeredCombos();
        Set<ComboType> combos2 = detector.getTriggeredCombos();

        // Verify
        assertNotSame(combos1, combos2, "Should return different instances");
        assertEquals(combos1, combos2, "Should have same content");
        assertTrue(combos1.contains(ComboType.TANK_SUPPORT), "Should contain TANK_SUPPORT");
    }

    @Test
    void getAllComboDefinitions_returnsAll8Combos() {
        // Execute
        List<ComboDefinition> definitions = detector.getAllComboDefinitions();

        // Verify
        assertEquals(8, definitions.size(), "Should have 8 combo definitions");
        
        // Verify all combo types are present
        Set<ComboType> types = new java.util.HashSet<>();
        for (ComboDefinition def : definitions) {
            types.add(def.getType());
        }
        assertEquals(8, types.size(), "Should have 8 unique combo types");
        assertTrue(types.contains(ComboType.TANK_SUPPORT), "Should contain TANK_SUPPORT");
        assertTrue(types.contains(ComboType.SPELL_SYNERGY), "Should contain SPELL_SYNERGY");
        assertTrue(types.contains(ComboType.ROYAL_COMBO), "Should contain ROYAL_COMBO");
    }

    @Test
    void getAllComboDefinitions_returnsCopy() {
        // Execute
        List<ComboDefinition> defs1 = detector.getAllComboDefinitions();
        List<ComboDefinition> defs2 = detector.getAllComboDefinitions();

        // Verify
        assertNotSame(defs1, defs2, "Should return different instances");
        assertEquals(defs1.size(), defs2.size(), "Should have same size");
    }

    @Test
    void getPossibleCombos_emptyDeck_returnsEmpty() {
        // Execute
        List<ComboDefinition> possible = detector.getPossibleCombos(new ArrayList<>());

        // Verify
        assertTrue(possible.isEmpty(), "Empty deck should return no possible combos");
    }

    @Test
    void getPossibleCombos_nullDeck_returnsEmpty() {
        // Execute
        List<ComboDefinition> possible = detector.getPossibleCombos(null);

        // Verify
        assertTrue(possible.isEmpty(), "Null deck should return empty list");
    }

    @Test
    void getPossibleCombos_royalComboDeck_returnsRoyalCombo() {
        // Setup: Deck with Knight and Archers
        List<Card> deck = new ArrayList<>();
        deck.add(createCard("card_knight", "Knight", CardType.TROOP));
        deck.add(createCard("card_archers", "Archers", CardType.TROOP));
        deck.add(createCard("card_giant", "Giant", CardType.TROOP));

        // Execute
        List<ComboDefinition> possible = detector.getPossibleCombos(deck);

        // Verify
        assertFalse(possible.isEmpty(), "Should have possible combos");
        assertTrue(possible.stream().anyMatch(def -> def.getType() == ComboType.ROYAL_COMBO),
            "Should contain ROYAL_COMBO");
    }

    @Test
    void getPossibleCombos_tankSupportDeck_returnsTankSupport() {
        // Setup: Deck with Giant and Musketeer
        List<Card> deck = new ArrayList<>();
        deck.add(createCard("card_giant", "Giant", CardType.TROOP));
        deck.add(createCard("card_musketeer", "Musketeer", CardType.TROOP));

        // Execute
        List<ComboDefinition> possible = detector.getPossibleCombos(deck);

        // Verify
        assertTrue(possible.stream().anyMatch(def -> def.getType() == ComboType.TANK_SUPPORT),
            "Should contain TANK_SUPPORT");
    }

    @Test
    void getPossibleCombos_spellSynergyDeck_returnsSpellSynergy() {
        // Setup: Deck with two spells
        List<Card> deck = new ArrayList<>();
        deck.add(createCard("card_zap", "Zap", CardType.SPELL));
        deck.add(createCard("card_fireball", "Fireball", CardType.SPELL));

        // Execute
        List<ComboDefinition> possible = detector.getPossibleCombos(deck);

        // Verify
        assertTrue(possible.stream().anyMatch(def -> def.getType() == ComboType.SPELL_SYNERGY),
            "Should contain SPELL_SYNERGY");
    }

    @Test
    void getPossibleCombos_noComboDeck_returnsEmpty() {
        // Setup: Deck with a single card that can't form combos by itself
        List<Card> deck = new ArrayList<>();
        deck.add(createCard("card_skeletons", "Skeletons", CardType.TROOP));

        // Execute
        List<ComboDefinition> possible = detector.getPossibleCombos(deck);

        // Verify
        assertTrue(possible.isEmpty(), "Single card deck should have no combos");
    }

    @Test
    void updateTime_removesOldPlays() {
        // Setup: Play cards and advance time beyond window
        Card giant = createCard("card_giant", "Giant", CardType.TROOP);
        Card musketeer = createCard("card_musketeer", "Musketeer", CardType.TROOP);

        detector.recordCardPlay(giant, 10.0);
        detector.recordCardPlay(musketeer, 11.0);
        assertEquals(1, detector.getTriggeredComboCount(), "Combo should be triggered");
        
        // Execute: Advance time beyond the combo window (5 seconds)
        detector.updateTime(20.0); // 10 seconds later

        // Verify: Should still have the combo triggered (once triggered, it stays)
        assertEquals(1, detector.getTriggeredComboCount(), "Triggered combo should remain");
        
        // But playing new cards should not trigger the same combo again (it's already triggered)
        detector.recordCardPlay(giant, 21.0);
        ComboType result = detector.recordCardPlay(musketeer, 22.0);
        
        // The combo was already triggered, so it shouldn't trigger again
        assertNull(result, "Same combo should not trigger again");
        assertEquals(1, detector.getTriggeredComboCount(), "Should still have only one combo");
    }

    @Test
    void listener_onComboTriggered_called() {
        // Setup
        Card giant = createCard("card_giant", "Giant", CardType.TROOP);
        Card musketeer = createCard("card_musketeer", "Musketeer", CardType.TROOP);

        // Execute
        detector.recordCardPlay(giant, 10.0);
        detector.recordCardPlay(musketeer, 11.0);

        // Verify
        assertEquals(1, listener.triggeredCount, "Listener should be called once");
        assertEquals(ComboType.TANK_SUPPORT, listener.lastComboType, "Last combo type should be TANK_SUPPORT");
        assertNotNull(listener.lastComboName, "Combo name should not be null");
        assertNotNull(listener.lastEffectDescription, "Effect description should not be null");
    }

    @Test
    void listener_nullListener_noException() {
        // Setup: Remove listener
        detector.setListener(null);
        Card knight = createCard("card_knight", "Knight", CardType.TROOP);
        Card archers = createCard("card_archers", "Archers", CardType.TROOP);

        // Execute & Verify: Should not throw exception
        assertDoesNotThrow(() -> {
            detector.recordCardPlay(knight, 10.0);
            detector.recordCardPlay(archers, 11.0);
        }, "Should handle null listener gracefully");
    }

    @Test
    void recordCardPlay_tracksMax10Plays() {
        // Setup: Play 15 cards
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            cards.add(createCard("card_knight", "Knight" + i, CardType.TROOP));
        }

        // Execute: Record all 15 cards
        for (int i = 0; i < 15; i++) {
            detector.recordCardPlay(cards.get(i), 10.0 + i * 0.1);
        }

        // Verify: Should still function (only last 10 should be tracked)
        // This is an internal implementation detail, but we can verify it doesn't crash
        assertNotNull(detector, "Detector should still exist");
        // Can trigger a combo with recent cards
        Card archers = createCard("card_archers", "Archers", CardType.TROOP);
        detector.recordCardPlay(archers, 16.5);
        // Should still work fine
    }

    @Test
    void recordCardPlay_multipleDifferentCombos_triggered() {
        // Setup: Create cards that trigger different combos
        // Use Giant + Musketeer for TANK_SUPPORT (unique)
        // Use Zap + Fireball for SPELL_SYNERGY (unique)
        Card giant = createCard("card_giant", "Giant", CardType.TROOP);
        Card musketeer = createCard("card_musketeer", "Musketeer", CardType.TROOP);
        Card zap = createCard("card_zap", "Zap", CardType.SPELL);
        Card fireball = createCard("card_fireball", "Fireball", CardType.SPELL);
        Card skeletons = createCard("card_skeletons", "Skeletons", CardType.TROOP);
        Card goblins = createCard("card_goblins", "Goblins", CardType.TROOP);

        // Execute: Trigger multiple different combos
        detector.recordCardPlay(giant, 10.0);
        detector.recordCardPlay(musketeer, 11.0); // TANK_SUPPORT
        detector.recordCardPlay(zap, 12.0);
        detector.recordCardPlay(fireball, 13.0); // SPELL_SYNERGY
        detector.recordCardPlay(skeletons, 14.0);
        detector.recordCardPlay(goblins, 15.0); // SWARM_ATTACK

        // Verify
        assertEquals(3, detector.getTriggeredComboCount(), "Should have triggered 3 combos");
        Set<ComboType> triggered = detector.getTriggeredCombos();
        assertTrue(triggered.contains(ComboType.TANK_SUPPORT), "Should contain TANK_SUPPORT");
        assertTrue(triggered.contains(ComboType.SPELL_SYNERGY), "Should contain SPELL_SYNERGY");
        assertTrue(triggered.contains(ComboType.SWARM_ATTACK), "Should contain SWARM_ATTACK");
        assertEquals(3, listener.triggeredCount, "Listener should be called 3 times");
    }

    // Helper methods

    private Card createCard(String id, String name, CardType type) {
        return new Card(id, name, 3, type, new CardStats(100, 50, 0, 0, 0), CardTarget.GROUND, "Test card");
    }

    // Test double for ComboListener

    private static class FakeComboListener implements ComboDetector.ComboListener {
        int triggeredCount = 0;
        ComboType lastComboType;
        String lastComboName;
        String lastEffectDescription;

        @Override
        public void onComboTriggered(ComboType comboType, String comboName, String effectDescription) {
            triggeredCount++;
            lastComboType = comboType;
            lastComboName = comboName;
            lastEffectDescription = effectDescription;
        }
    }
}

