package com.battleship.model.mode;

import com.battleship.model.Player;

/**
 * classic naval battle mode: standard fleet annihilation victory condition.
 * strictly pure domain class with zero JavaFX imports.
 */
public class ClassicModeStrategy implements GameModeStrategy {

    @Override
    public String modeName() {
        return "Classic";
    }

    @Override
    public boolean isBoardSizeAllowed(int boardSize) {
        return boardSize > 0;
    }

    @Override
    public void initializeMatch(Player player1, Player player2, int boardSize) {
        // Standard fleet deployment handled by players
    }

    @Override
    public VictoryResult evaluateVictory(Player attacker, Player defender, int currentRound) {
        if (defender != null && defender.isFleetDestroyed()) {
            return VictoryResult.victory(attacker, "Hostile fleet annihilated");
        }
        if (attacker != null && attacker.isFleetDestroyed()) {
            return VictoryResult.victory(defender, "Friendly fleet annihilated");
        }
        return VictoryResult.inProgress();
    }
}
