package application.challenge;

import application.ArenaLayoutService;
import application.DeckService;
import application.MatchService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import kuroyale.domain.Arena;
import kuroyale.domain.ArenaLayout;
import kuroyale.domain.ChallengeProgress;
import kuroyale.domain.Deck;
import kuroyale.domain.Match;
import kuroyale.domain.MatchOutcome;
import kuroyale.domain.Player;
import kuroyale.domain.PlayerProfile;
import kuroyale.domain.Tower;
import kuroyale.domain.TowerOwner;
import kuroyale.infrastructure.PlayerProfileRepository;
import kuroyale.support.Result;

/**
 * Orchestrates Challenge Mode (Phase 2 Feature 4).
 * <p>
 * Responsibilities:
 * - list challenges + progress
 * - validate deck and start a challenge match
 * - compute stars, award gold, unlock next challenge, and persist progress
 */
public class ChallengeService {

    private final PlayerProfileRepository profileRepository;
    private final DeckService deckService;
    private final MatchService matchService;
    private final ArenaLayoutService arenaLayoutService;
    private final ChallengeFactory challengeFactory = new ChallengeFactory();

    public ChallengeService(PlayerProfileRepository profileRepository,
                            DeckService deckService,
                            MatchService matchService,
                            ArenaLayoutService arenaLayoutService) {
        this.profileRepository = profileRepository;
        this.deckService = deckService;
        this.matchService = matchService;
        this.arenaLayoutService = arenaLayoutService;
    }

    public List<ChallengeCard> listChallenges() {
        PlayerProfile profile = profileRepository.load();
        Map<String, ChallengeProgress> progressMap = ensureProgress(profile);
        // Persist initialization (so first run creates default challenge progress on disk)
        profileRepository.save(profile);

        List<ChallengeCard> cards = new ArrayList<>();
        for (ChallengeDefinition def : challengeFactory.allChallenges()) {
            ChallengeProgress progress = progressMap.get(def.getId());
            cards.add(new ChallengeCard(def, progress));
        }
        cards.sort(Comparator.comparingInt(c -> Integer.parseInt(c.getDefinition().getId())));
        return cards;
    }

    public ChallengeDefinition getDefinition(String challengeId) {
        return challengeFactory.byId(challengeId);
    }

    public Result<ChallengeSession> startChallenge(String challengeId) {
        ChallengeDefinition def = challengeFactory.byId(challengeId);
        if (def == null) {
            return Result.fail("Unknown challenge.");
        }
        PlayerProfile profile = profileRepository.load();
        Map<String, ChallengeProgress> progressMap = ensureProgress(profile);
        ChallengeProgress progress = progressMap.get(def.getId());
        if (progress == null || !progress.isUnlocked()) {
            return Result.fail("Challenge is locked.");
        }

        Deck deck = deckService.loadDeck();
        Result<?> validation = def.validateDeck(deck);
        if (!validation.isSuccess()) {
            return Result.fail(validation.getMessage());
        }

        progress.incrementAttempts();
        profile.setChallengeProgress(def.getId(), progress);
        profileRepository.save(profile);

        ArenaLayout layout = arenaLayoutService.getActiveLayout();
        Deck botDeck = deckService.buildDefaultDeck();
        Player player = new Player(profile.getPlayerName() != null ? profile.getPlayerName() : "Player", deck, 0);
        Player bot = new Player("Bot", botDeck, 0);
        Match match = matchService.createMatch(player, bot, layout);
        match.setBotEnabled(true);
        if (def.buildCostPolicy() != null) {
            match.setCardCostPolicy((actingPlayer, card, baseCost) -> {
                // Apply modifiers to the player's side only.
                if (actingPlayer == match.getPlayer()) {
                    return def.buildCostPolicy().resolveCost(actingPlayer, card, baseCost);
                }
                return baseCost;
            });
        }

        ChallengeSession session = new ChallengeSession(def.getId(), match);
        captureInitialPlayerTowerHp(session);
        return Result.ok(session);
    }

    /**
 * Completes a challenge session and calculates rewards based on match outcome.
 * 
 * Requires:
 * - session != null
 * - session.getMatch() != null
 * - session.getChallengeId() is a valid challenge ID that exists in the challenge factory
 * - match.getOutcome() != null (match must have a completed outcome)
 * - match.isOver() == true (match must be finished)
 * 
 * Modifies:
 * - PlayerProfile (loaded from profileRepository): 
 *   - Updates challenge progress for the completed challenge
 *   - Adds gold if player won
 *   - Unlocks next challenge if player won
 *   - Updates best stars and best time if player won
 * - profileRepository: persists the modified PlayerProfile to disk
 * 
 * Effects:
 * - Returns Result<ChallengeCompletion> containing:
 *   - win: true if outcome.getWinner() == TowerOwner.PLAYER, false otherwise
 *   - stars: 0 if loss, otherwise 1-3 based on:
 *     * 1 star: base win
 *     * 2 stars: win + completed within time limit (if time limit > 0)
 *     * 3 stars: win + no tower damage taken
 *   - goldAwarded: def.getRewardGold() if win, 0 otherwise
 * - If win: increments completion count, updates best stars/time, awards gold, unlocks next challenge
 * - If loss: no progress updates, no gold awarded
 * - PlayerProfile is always saved to repository
 * 
 * @param session The challenge session to complete
 * @return Result containing ChallengeCompletion with win status, stars, and gold, or error message
 */

    public Result<ChallengeCompletion> completeChallenge(ChallengeSession session) {
        if (session == null || session.getMatch() == null) {
            return Result.fail("Challenge session is invalid.");
        }
        ChallengeDefinition def = challengeFactory.byId(session.getChallengeId());
        if (def == null) {
            return Result.fail("Unknown challenge.");
        }
        Match match = session.getMatch();
        MatchOutcome outcome = match.getOutcome();
        if (outcome == null || !match.isOver()) {
            return Result.fail("Match is not finished yet.");
        }

        boolean win = outcome.getWinner() == TowerOwner.PLAYER;
        int stars = 0;
        int goldAwarded = 0;
        if (win) {
            stars = 1;
            if (def.getTimeLimitSeconds() > 0 && match.getElapsedSeconds() <= def.getTimeLimitSeconds()) {
                stars = Math.max(stars, 2);
            }
            boolean noDamageTaken = didPlayerTakeNoTowerDamage(session);
            if (noDamageTaken) {
                stars = Math.max(stars, 3);
            }
            goldAwarded = def.getRewardGold();
        }

        PlayerProfile profile = profileRepository.load();
        Map<String, ChallengeProgress> progressMap = ensureProgress(profile);
        ChallengeProgress progress = progressMap.get(def.getId());
        if (progress == null) {
            progress = new ChallengeProgress(def.getId(), false);
        }
        if (win) {
            progress.incrementCompletions();
            progress.setBestStars(Math.max(progress.getBestStars(), stars));
            if (progress.getBestTimeSeconds() <= 0 || match.getElapsedSeconds() < progress.getBestTimeSeconds()) {
                progress.setBestTimeSeconds(match.getElapsedSeconds());
            }
            profile.addGold(goldAwarded);
            unlockNext(progressMap, def.getId());
        }

        profile.setChallengeProgress(def.getId(), progress);
        profileRepository.save(profile);
        return Result.ok(new ChallengeCompletion(win, stars, goldAwarded));
    }

    private void unlockNext(Map<String, ChallengeProgress> progressMap, String completedId) {
        try {
            int id = Integer.parseInt(completedId);
            String next = String.valueOf(id + 1);
            ChallengeProgress nextProg = progressMap.get(next);
            if (nextProg != null) {
                nextProg.setUnlocked(true);
            }
        } catch (Exception ignored) {
        }
    }

    private Map<String, ChallengeProgress> ensureProgress(PlayerProfile profile) {
        Map<String, ChallengeProgress> map = new HashMap<>(profile.getChallengeProgress());
        // Ensure all 5 exist; challenge 1 is unlocked by default
        for (ChallengeDefinition def : challengeFactory.allChallenges()) {
            if (!map.containsKey(def.getId())) {
                boolean unlocked = "1".equals(def.getId());
                map.put(def.getId(), new ChallengeProgress(def.getId(), unlocked));
            }
        }
        profile.setChallengeProgress(map);
        return map;
    }

    private void captureInitialPlayerTowerHp(ChallengeSession session) {
        Match match = session.getMatch();
        Arena arena = match.getArena();
        if (arena == null) {
            return;
        }
        for (Tower t : arena.getTowers()) {
            if (t == null || t.getOwner() != TowerOwner.PLAYER || t.getPosition() == null) {
                continue;
            }
            String key = t.getType() + "@" + t.getPosition().getX() + "," + t.getPosition().getY();
            session.recordInitialTowerHp(key, t.getHp());
        }
    }

    private boolean didPlayerTakeNoTowerDamage(ChallengeSession session) {
        Match match = session.getMatch();
        Arena arena = match.getArena();
        if (arena == null) {
            return false;
        }
        Map<String, Integer> initial = session.getInitialPlayerTowerHp();
        for (Tower t : arena.getTowers()) {
            if (t == null || t.getOwner() != TowerOwner.PLAYER || t.getPosition() == null) {
                continue;
            }
            String key = t.getType() + "@" + t.getPosition().getX() + "," + t.getPosition().getY();
            Integer startHp = initial.get(key);
            if (startHp != null && t.getHp() < startHp) {
                return false;
            }
        }
        return true;
    }

    public static class ChallengeCard {
        private final ChallengeDefinition definition;
        private final ChallengeProgress progress;

        public ChallengeCard(ChallengeDefinition definition, ChallengeProgress progress) {
            this.definition = definition;
            this.progress = progress;
        }

        public ChallengeDefinition getDefinition() {
            return definition;
        }

        public ChallengeProgress getProgress() {
            return progress;
        }
    }

    public static class ChallengeCompletion {
        private final boolean win;
        private final int stars;
        private final int goldAwarded;

        public ChallengeCompletion(boolean win, int stars, int goldAwarded) {
            this.win = win;
            this.stars = stars;
            this.goldAwarded = goldAwarded;
        }

        public boolean isWin() {
            return win;
        }

        public int getStars() {
            return stars;
        }

        public int getGoldAwarded() {
            return goldAwarded;
        }
    }
}


