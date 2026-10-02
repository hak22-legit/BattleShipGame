package com.battleship.model;

import com.battleship.ai.AIStrategy;

import java.util.Map;
import java.util.Optional;

/**
 * a machine admiral: composes an {@link aistrategy} (the "how to think" part) and
 * answers the polymorphic turn contract on its own.
 *
 * <p>fixes v2.2 — the old design kept an {@code aistrategy} field in
 * {@code battleservice} and branched on {@code attacker == player2} to decide who
 * was shooting. now the player object itself knows how to take its turn, so the
 * battle service contains no ai special cases at all.</p>
 */
public final class AiPlayer extends Player {

    private final AIStrategy strategy;

    public AiPlayer(String name, int boardSize, Map<ShipType, Integer> enemyFleetComposition, AIStrategy strategy) {
        super(name, boardSize, enemyFleetComposition);
        if (strategy == null) {
            throw new IllegalArgumentException("An AI player requires a strategy (composition over flags).");
        }
        this.strategy = strategy;
    }

    /** convenience for the standard match flow: board size and roster come from the theater. */
    public AiPlayer(String name, Theater theater, AIStrategy strategy) {
        this(name, theater.getBoardSize(), theater.getFleetComposition(), strategy);
    }

    @Override
    public boolean isAutonomous() {
        return true;
    }

    /**
     * the ai plans against its own knowledge grid and its own ammunition readout —
     * it never receives the opponent's primary grid (v1.3 / smell 5.2).
     */
    @Override
    public Optional<ShotOrder> decideAutonomousShot() {
        return Optional.of(strategy.chooseShotPlan(trackingGrid(), this));
    }

    /** the brain learns only from the shots it actually fired. */
    @Override
    public void observeOwnShot(ShotResult result) {
        strategy.notifyResult(result);
    }

    /** exposed for diagnostics/tests; the strategy itself stays encapsulated. */
    public String strategyName() {
        return strategy.getClass().getSimpleName();
    }
}
