package com.battleship.model.mode;

import com.battleship.model.Coordinate;
import com.battleship.model.Player;
import com.battleship.model.ShotResult;

import java.util.List;

/**
 * blitz mode:
 * enforces a 90-second global countdown chess clock per player (+2s bonus per shot).
 * flag-fall (0:00) causes an instant loss.
 * strictly pure domain class with zero JavaFX imports.
 */
public class BlitzModeStrategy implements GameModeStrategy {

    public static final long INITIAL_TIME_MILLIS = 90_000L; // 90 seconds
    public static final long BONUS_PER_SHOT_MILLIS = 2_000L; // +2s increment bonus

    private Player player1;
    private Player player2;
    private long player1RemainingMillis = INITIAL_TIME_MILLIS;
    private long player2RemainingMillis = INITIAL_TIME_MILLIS;
    private long turnStartTime = 0L;
    private Player currentTurnPlayer;
    private boolean flagged = false;
    private Player flaggedPlayer = null;

    @Override
    public String modeName() {
        return "Blitz";
    }

    @Override
    public boolean isBoardSizeAllowed(int boardSize) {
        return boardSize > 0;
    }

    @Override
    public void initializeMatch(Player player1, Player player2, int boardSize) {
        this.player1 = player1;
        this.player2 = player2;
        this.player1RemainingMillis = INITIAL_TIME_MILLIS;
        this.player2RemainingMillis = INITIAL_TIME_MILLIS;
        this.flagged = false;
        this.flaggedPlayer = null;
        this.turnStartTime = System.currentTimeMillis();
        this.currentTurnPlayer = player1;
    }

    public void onTurnStart(Player player) {
        this.currentTurnPlayer = player;
        this.turnStartTime = System.currentTimeMillis();
    }

    /**
     * called when a shot is executed by the shooter.
     * calculates elapsed time, deducts it, and adds the +2s bonus.
     */
    public void onShotExecuted(Player shooter) {
        long now = System.currentTimeMillis();
        long elapsed = turnStartTime > 0 ? (now - turnStartTime) : 0;
        recordTimeDeductionAndBonus(shooter, elapsed);
        this.turnStartTime = now;
    }

    /**
     * deducts elapsed milliseconds and awards the +2s bonus.
     * if the player's clock drops to 0 or below, flag-fall occurs immediately.
     */
    public void recordTimeDeductionAndBonus(Player player, long elapsedMillis) {
        if (player == null || flagged) return;
        if (player.equals(player1)) {
            player1RemainingMillis = player1RemainingMillis - elapsedMillis + BONUS_PER_SHOT_MILLIS;
            if (player1RemainingMillis <= 0) {
                player1RemainingMillis = 0;
                flagged = true;
                flaggedPlayer = player1;
            }
        } else if (player.equals(player2)) {
            player2RemainingMillis = player2RemainingMillis - elapsedMillis + BONUS_PER_SHOT_MILLIS;
            if (player2RemainingMillis <= 0) {
                player2RemainingMillis = 0;
                flagged = true;
                flaggedPlayer = player2;
            }
        }
    }

    /** triggers flag-fall explicitly (e.g. clock timer tick reached 0). */
    public void triggerFlagFall(Player player) {
        this.flagged = true;
        this.flaggedPlayer = player;
        if (player != null && player.equals(player1)) {
            player1RemainingMillis = 0;
        } else if (player != null && player.equals(player2)) {
            player2RemainingMillis = 0;
        }
    }

    public long getRemainingTimeMillis(Player player) {
        if (player == null) return 0;
        return player.equals(player1) ? player1RemainingMillis : player2RemainingMillis;
    }

    public double getRemainingTimeSeconds(Player player) {
        return Math.max(0.0, getRemainingTimeMillis(player) / 1000.0);
    }

    public boolean isFlagged() {
        return flagged;
    }

    public Player getFlaggedPlayer() {
        return flaggedPlayer;
    }

    @Override
    public VictoryResult evaluateVictory(Player attacker, Player defender, int currentRound) {
        // Flag-fall condition: instant loss for flagged player
        if (flagged && flaggedPlayer != null) {
            Player winner = flaggedPlayer.equals(player1) ? player2 : player1;
            if (winner == null) {
                winner = flaggedPlayer.equals(attacker) ? defender : attacker;
            }
            return VictoryResult.victory(winner, "Flag-fall! Global countdown expired at 0:00.");
        }

        // Fleet annihilation condition
        if (defender != null && defender.isFleetDestroyed()) {
            return VictoryResult.victory(attacker, "Hostile fleet annihilated");
        }
        if (attacker != null && attacker.isFleetDestroyed()) {
            return VictoryResult.victory(defender, "Friendly fleet annihilated");
        }

        return VictoryResult.inProgress();
    }
}
