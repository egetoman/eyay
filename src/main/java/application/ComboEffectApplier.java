package application;

import java.util.ArrayList;
import java.util.List;

import kuroyale.domain.Arena;
import kuroyale.domain.Card;
import kuroyale.domain.CardType;
import kuroyale.domain.ComboType;
import kuroyale.domain.Player;
import kuroyale.domain.TowerOwner;
import kuroyale.domain.Unit;
import kuroyale.domain.UnitMovementType;

/**
 * Applies combo effects to units and/or player resources.
 * Effects are permanent for the affected units (until the unit dies).
 */
public final class ComboEffectApplier {

    private ComboEffectApplier() {
    }

    public static List<Unit> apply(ComboType comboType, Arena arena, TowerOwner owner, Card latestCard, Unit latestUnit,
            Player ownerPlayer) {
        if (comboType == null || arena == null || owner == null) {
            return List.of();
        }

        switch (comboType) {
            case TANK_SUPPORT:
                return applyTankSupport(owner, latestCard, latestUnit);
            case SPELL_SYNERGY:
                applySpellSynergyRefund(ownerPlayer);
                return List.of();
            case SWARM_ATTACK:
                return applySwarmSpeed(arena, owner);
            case BUILDING_DEFENSE:
                return applyBuildingHp(arena, owner);
            case AIR_ASSAULT:
                return applyAirDamage(arena, owner);
            case ROYAL_COMBO:
                return applyKnightHp(arena, owner);
            case SIEGE_MODE:
                return applyMortarRange(arena, owner);
            case RUSH_ATTACK:
                return applyHogSpeed(arena, owner);
            default:
                return List.of();
        }
    }

    private static List<Unit> applyTankSupport(TowerOwner owner, Card latestCard, Unit latestUnit) {
        if (latestCard == null || latestUnit == null || latestUnit.getOwner() != owner || latestUnit.getCard() == null) {
            return List.of();
        }
        String id = latestCard.getId();
        if (!isRangedTroop(id)) {
            return List.of();
        }
        latestUnit.setDamageMultiplier(1.15);
        return List.of(latestUnit);
    }

    private static void applySpellSynergyRefund(Player ownerPlayer) {
        // Refund happens immediately after the second different spell is cast.
        if (ownerPlayer != null) {
            ownerPlayer.regenerateElixir(1);
        }
    }

    private static List<Unit> applySwarmSpeed(Arena arena, TowerOwner owner) {
        List<Unit> affected = new ArrayList<>();
        for (Unit u : arena.getUnits()) {
            if (u == null || u.isDefeated() || u.getOwner() != owner || u.getCard() == null) {
                continue;
            }
            if (isSwarm(u.getCard().getId())) {
                u.setSpeedMultiplier(1.10);
                affected.add(u);
            }
        }
        return affected;
    }

    private static List<Unit> applyBuildingHp(Arena arena, TowerOwner owner) {
        List<Unit> affected = new ArrayList<>();
        for (Unit u : arena.getUnits()) {
            if (u == null || u.isDefeated() || u.getOwner() != owner || u.getCard() == null) {
                continue;
            }
            if (u.getCard().getType() == CardType.BUILDING) {
                int baseHp = u.getCard().getStats() != null ? u.getCard().getStats().getHp() : 0;
                int bonus = (int) Math.round(baseHp * 0.20);
                if (bonus > 0) {
                    u.applyHPBonus(bonus);
                }
                affected.add(u);
            }
        }
        return affected;
    }

    private static List<Unit> applyAirDamage(Arena arena, TowerOwner owner) {
        List<Unit> affected = new ArrayList<>();
        for (Unit u : arena.getUnits()) {
            if (u == null || u.isDefeated() || u.getOwner() != owner || u.getCard() == null) {
                continue;
            }
            if (u.getCard().getMovementType() == UnitMovementType.FLYING) {
                u.setDamageMultiplier(1.15);
                affected.add(u);
            }
        }
        return affected;
    }

    private static List<Unit> applyKnightHp(Arena arena, TowerOwner owner) {
        for (Unit u : arena.getUnits()) {
            if (u == null || u.isDefeated() || u.getOwner() != owner || u.getCard() == null) {
                continue;
            }
            if ("card_knight".equals(u.getCard().getId())) {
                u.applyHPBonus(100);
                return List.of(u);
            }
        }
        return List.of();
    }

    private static List<Unit> applyMortarRange(Arena arena, TowerOwner owner) {
        for (Unit u : arena.getUnits()) {
            if (u == null || u.isDefeated() || u.getOwner() != owner || u.getCard() == null) {
                continue;
            }
            if ("card_mortar".equals(u.getCard().getId())) {
                u.setRangeBonus(2.0);
                return List.of(u);
            }
        }
        return List.of();
    }

    private static List<Unit> applyHogSpeed(Arena arena, TowerOwner owner) {
        for (Unit u : arena.getUnits()) {
            if (u == null || u.isDefeated() || u.getOwner() != owner || u.getCard() == null) {
                continue;
            }
            if ("card_hog_rider".equals(u.getCard().getId())) {
                u.setSpeedMultiplier(1.20);
                return List.of(u);
            }
        }
        return List.of();
    }

    private static boolean isRangedTroop(String cardId) {
        return "card_musketeer".equals(cardId) || "card_archers".equals(cardId) || "card_spear_goblins".equals(cardId)
                || "card_wizard".equals(cardId);
    }

    private static boolean isSwarm(String cardId) {
        return "card_skeletons".equals(cardId) || "card_goblins".equals(cardId) || "card_spear_goblins".equals(cardId)
                || "card_archers".equals(cardId) || "card_minions".equals(cardId) || "card_minion_horde".equals(cardId)
                || "card_barbarians".equals(cardId);
    }
}

