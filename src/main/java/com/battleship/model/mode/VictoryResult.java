package com.battleship.model.mode;

import com.battleship.model.Player;

/**
 * immutable evaluation of match victory state.
 * strictly pure domain model with zero JavaFX imports.
 */
public record VictoryResult(boolean isGameOver, Player winner, String victoryReason) {

    public static VictoryResult inProgress() {
        return new VictoryResult(false, null, "");
    }

    public static VictoryResult victory(Player winner, String reason) {
        return new VictoryResult(true, winner, reason);
    }
}
