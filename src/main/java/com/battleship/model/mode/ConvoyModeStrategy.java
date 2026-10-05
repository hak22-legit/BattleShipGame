package com.battleship.model.mode;

import com.battleship.model.Coordinate;
import com.battleship.model.Orientation;
import com.battleship.model.Player;
import com.battleship.model.ShipType;

import java.util.Objects;

/**
 * convoy mode (8x8 and 10x10 boards only):
 * defender has a 3-cell FreighterShip.
 * - Attacker wins immediately if the freighter sinks.
 * - Defender wins if the freighter survives 15 rounds or the attacker fleet sinks.
 * strictly pure domain class with zero JavaFX imports.
 */
public class ConvoyModeStrategy implements GameModeStrategy {

    public static final int ROUND_LIMIT = 15;
    private Player convoyDefender;

    public ConvoyModeStrategy() {
    }

    public ConvoyModeStrategy(Player designatedDefender) {
        this.convoyDefender = designatedDefender;
    }

    @Override
    public String modeName() {
        return "Convoy";
    }

    @Override
    public boolean isBoardSizeAllowed(int boardSize) {
        return boardSize >= 8;
    }

    @Override
    public void initializeMatch(Player player1, Player player2, int boardSize) {
        if (!isBoardSizeAllowed(boardSize)) {
            throw new IllegalArgumentException("Convoy mode is only supported on 8x8 and 10x10 boards, got: " + boardSize);
        }
        if (convoyDefender == null) {
            convoyDefender = player2 != null ? player2 : player1;
        }

        // Deploy 3-cell FreighterShip if not already present on defender's board
        if (convoyDefender != null) {
            boolean hasFreighter = convoyDefender.primaryGrid().fleet().stream()
                    .anyMatch(s -> s.type() == ShipType.FREIGHTER);
            if (!hasFreighter) {
                deployFreighterSafely(convoyDefender, boardSize);
            }
        }
    }

    private void deployFreighterSafely(Player defender, int boardSize) {
        for (int r = 0; r < boardSize; r++) {
            for (int c = 0; c <= boardSize - 3; c++) {
                Coordinate coord = new Coordinate(r, c);
                if (defender.primaryGrid().canDeploy(ShipType.FREIGHTER, coord, Orientation.HORIZONTAL)) {
                    defender.primaryGrid().deploy(ShipType.FREIGHTER, coord, Orientation.HORIZONTAL);
                    return;
                }
            }
        }
        for (int r = 0; r <= boardSize - 3; r++) {
            for (int c = 0; c < boardSize; c++) {
                Coordinate coord = new Coordinate(r, c);
                if (defender.primaryGrid().canDeploy(ShipType.FREIGHTER, coord, Orientation.VERTICAL)) {
                    defender.primaryGrid().deploy(ShipType.FREIGHTER, coord, Orientation.VERTICAL);
                    return;
                }
            }
        }
    }

    @Override
    public VictoryResult evaluateVictory(Player attacker, Player defender, int currentRound) {
        Player actualDefender = (convoyDefender != null) ? convoyDefender : defender;
        Player actualAttacker = (actualDefender == defender) ? attacker : defender;

        // Condition 1: Attacker wins immediately if the freighter sinks
        boolean freighterSunk = actualDefender.primaryGrid().fleet().stream()
                .anyMatch(s -> s.type() == ShipType.FREIGHTER && s.isSunk());
        if (freighterSunk) {
            return VictoryResult.victory(actualAttacker, "Freighter sunk! Convoy intercepted.");
        }

        // Condition 2: Defender wins if attacker fleet is annihilated
        if (actualAttacker.isFleetDestroyed()) {
            return VictoryResult.victory(actualDefender, "Attacker fleet destroyed! Convoy delivered.");
        }

        // Condition 3: Defender wins if freighter survives 15 rounds
        if (currentRound >= ROUND_LIMIT) {
            return VictoryResult.victory(actualDefender, "Freighter survived " + ROUND_LIMIT + " rounds! Convoy delivered.");
        }

        return VictoryResult.inProgress();
    }

    public Player getConvoyDefender() {
        return convoyDefender;
    }
}
