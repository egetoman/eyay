package kuroyale.domain;

/**
 * Optional policy hook to modify elixir costs for special modes (e.g., Challenge Mode).
 * <p>
 * Default behavior (when no policy is set) is to use {@link Card#getElixirCost()}.
 */
@FunctionalInterface
public interface CardCostPolicy {
    int resolveCost(Player actingPlayer, Card card, int baseCost);
}


