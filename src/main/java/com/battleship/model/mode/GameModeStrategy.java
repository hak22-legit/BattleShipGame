package com.battleship.model.mode;

import com.battleship.model.Coordinate;
import com.battleship.model.Player;
import com.battleship.model.ShotResult;

import java.util.List;

/**
 * decoupled strategy pattern interface for naval command game modes.
 * strictly pure domain interface with zero JavaFX imports.
 */
public interface GameModeStrategy {

    String modeName();

    boolean isBoardSizeAllowed(int boardSize);

    void initializeMatch(Player player1, Player player2, int boardSize);

    VictoryResult evaluateVictory(Player attacker, Player defender, int currentRound);

    /**
     * optional post-shot trigger hook (e.g. minefield detonation).
     * returns any additional shot results triggered by secondary explosions.
     */
    default List<ShotResult> onShotResolved(
            Player attacker,
            Player defender,
            Coordinate target,
            List<ShotResult> resolvedShots
    ) {
        return List.of();
    }
}
