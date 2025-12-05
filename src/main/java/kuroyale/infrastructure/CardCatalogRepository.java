package kuroyale.infrastructure;

import java.util.ArrayList;
import java.util.List;
import kuroyale.domain.Card;
import kuroyale.domain.CardStats;
import kuroyale.domain.CardTarget;
import kuroyale.domain.CardType;

public class CardCatalogRepository {

    public List<Card> findAll() {
        List<Card> cards = new ArrayList<>();

        // Single-target troops
        cards.add(troop("card_knight", "Knight", 3, 600, 75, 1.0, "Medium", 1.1, CardTarget.GROUND, "A tough soldier with a sword. Good for soaking damage."));
        cards.add(troop("card_musketeer", "Musketeer", 4, 340, 100, 6.5, "Medium", 1.1, CardTarget.AIR_AND_GROUND, "A ranged shooter that can hit air and ground."));
        cards.add(troop("card_mini_pekka", "Mini P.E.K.K.A", 4, 600, 325, 1.0, "Slow", 1.8, CardTarget.GROUND, "A powerful armored warrior. Slow but massive damage."));
        cards.add(troop("card_giant", "Giant", 5, 2000, 126, 1.0, "Very Slow", 1.5, CardTarget.BUILDINGS, "Huge tank that targets buildings only."));
        cards.add(troop("card_hog_rider", "Hog Rider", 4, 800, 160, 1.0, "Fast", 1.5, CardTarget.BUILDINGS, "Fast unit that rushes toward buildings."));

        // Area-of-effect troops
        cards.add(troop("card_bomber", "Bomber", 3, 150, 100, 5.0, "Medium", 1.9, CardTarget.GROUND, "Throws bombs that explode on impact."));
        cards.add(troop("card_valkyrie", "Valkyrie", 4, 880, 120, 1.0, "Medium", 1.5, CardTarget.GROUND, "Spins and damages all nearby enemies."));
        cards.add(troop("card_wizard", "Wizard", 5, 340, 130, 5.0, "Medium", 1.7, CardTarget.AIR_AND_GROUND, "Shoots explosive fireballs."));

        // Swarm troops
        cards.add(troop("card_skeletons", "Skeletons", 1, 30, 30, 1.0, "Very Fast", 1.0, CardTarget.GROUND, "Spawns 4 fragile but fast units."));
        cards.add(troop("card_goblins", "Goblins", 2, 80, 50, 1.0, "Fast", 1.1, CardTarget.GROUND, "Spawns 3 fast melee fighters."));
        cards.add(troop("card_spear_goblins", "Spear Goblins", 2, 52, 24, 5.5, "Fast", 1.1, CardTarget.AIR_AND_GROUND, "Spawns 3 ranged goblins."));
        cards.add(troop("card_archers", "Archers", 3, 125, 40, 5.5, "Medium", 1.1, CardTarget.AIR_AND_GROUND, "Spawns 2 ranged soldiers."));
        cards.add(troop("card_minions", "Minions", 3, 90, 40, 2.5, "Very Fast", 1.0, CardTarget.AIR_AND_GROUND, "Spawns 3 flying attackers."));
        cards.add(troop("card_minion_horde", "Minion Horde", 5, 90, 40, 2.5, "Very Fast", 1.0, CardTarget.AIR_AND_GROUND, "Spawns 6 flying attackers."));
        cards.add(troop("card_barbarians", "Barbarians", 5, 300, 75, 1.0, "Fast", 1.5, CardTarget.GROUND, "Spawns 4 tough melee fighters."));

        // Defensive buildings
        cards.add(building("card_cannon", "Cannon", 3, 400, 60, 5.5, 0, CardTarget.GROUND, "Basic defensive tower."));
        cards.add(building("card_tesla", "Tesla", 4, 400, 64, 5.5, 0, CardTarget.AIR_AND_GROUND, "Hidden tower that hits air & ground."));
        cards.add(building("card_mortar", "Mortar", 4, 600, 108, 11.0, 5.0, CardTarget.GROUND, "Long-range artillery."));
        cards.add(building("card_bomb_tower", "Bomb Tower", 5, 900, 100, 6.0, 1.8, CardTarget.GROUND, "Defensive tower with explosive shells."));
        cards.add(building("card_inferno_tower", "Inferno Tower", 5, 800, 200, 6.0, 1.0, CardTarget.AIR_AND_GROUND, "Laser tower that ramps damage."));

        // Spawner / special buildings
        cards.add(building("card_tombstone", "Tombstone", 3, 200, 0, 0, 0, CardTarget.GROUND, "Spawns skeletons over time."));
        cards.add(building("card_goblin_hut", "Goblin Hut", 5, 700, 0, 0, 0, CardTarget.GROUND, "Spawns spear goblins periodically."));
        cards.add(building("card_barbarian_hut", "Barbarian Hut", 7, 1100, 0, 0, 0, CardTarget.GROUND, "Spawns barbarians periodically."));
        cards.add(building("card_elixir_collector", "Elixir Collector", 5, 640, 0, 0, 0, CardTarget.NONE, "Generates elixir over time."));

        // Spells
        cards.add(spell("card_zap", "Zap", 2, 80, 2.5, "Small area damage + 0.5s stun."));
        cards.add(spell("card_arrows", "Arrows", 3, 115, 4.0, "Medium area damage."));
        cards.add(spell("card_fireball", "Fireball", 4, 325, 2.5, "Large area damage for clusters."));
        cards.add(spell("card_rocket", "Rocket", 6, 700, 2.0, "Massive damage in small area."));

        return cards;
    }

    private Card troop(String id, String name, int cost, int hp, int damage, double rangeTiles, String moveSpeed, double hitSpeedSeconds, CardTarget target, String description) {
        return new Card(id, name, cost, CardType.TROOP, stats(hp, damage, rangeTiles, moveSpeed, hitSpeedSeconds), target, description);
    }

    private Card building(String id, String name, int cost, int hp, int damage, double rangeTiles, double hitSpeedSeconds, CardTarget target, String description) {
        return new Card(id, name, cost, CardType.BUILDING, stats(hp, damage, rangeTiles, "None", hitSpeedSeconds), target, description);
    }

    private Card spell(String id, String name, int cost, int areaDamage, double radiusTiles, String description) {
        CardStats stats = new CardStats(0, areaDamage, rangeValue(radiusTiles), 0, 0);
        return new Card(id, name, cost, CardType.SPELL, stats, CardTarget.AIR_AND_GROUND, description);
    }

    private CardStats stats(int hp, int damage, double rangeTiles, String moveSpeed, double hitSpeedSeconds) {
        return new CardStats(
            hp,
            damage,
            rangeValue(rangeTiles),
            speedValue(moveSpeed),
            hitSpeedMillis(hitSpeedSeconds)
        );
    }

    private int rangeValue(double tiles) {
        return (int) Math.round(tiles * 10);
    }

    private int speedValue(String descriptor) {
        if (descriptor == null) {
            return 0;
        }
        switch (descriptor.toLowerCase()) {
            case "very slow":
                return 10;
            case "slow":
                return 20;
            case "medium":
                return 30;
            case "fast":
                return 40;
            case "very fast":
                return 50;
            default:
                return 0;
        }
    }

    private int hitSpeedMillis(double seconds) {
        if (seconds <= 0) {
            return 0;
        }
        return (int) Math.round(seconds * 1000);
    }
}




