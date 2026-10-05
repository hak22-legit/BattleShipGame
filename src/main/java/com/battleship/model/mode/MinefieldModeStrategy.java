package com.battleship.model.mode;

import com.battleship.model.CellStatus;
import com.battleship.model.Coordinate;
import com.battleship.model.Player;
import com.battleship.model.SeaMine;
import com.battleship.model.ShotResult;
import com.battleship.model.projection.ShipSnapshot;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * minefield mode:
 * randomly seeds 2-4 hidden neutral SeaMine cells on water.
 * striking a mine immediately triggers an automatic secondary Cross (+) detonation.
 * strictly pure domain class with zero JavaFX imports.
 */
public class MinefieldModeStrategy implements GameModeStrategy {

    private static final SecureRandom RANDOM = new SecureRandom();
    private final List<SeaMine> seaMines = new ArrayList<>();
    private final int explicitMineCount;

    public MinefieldModeStrategy() {
        this(0);
    }

    public MinefieldModeStrategy(int explicitMineCount) {
        this.explicitMineCount = explicitMineCount;
    }

    @Override
    public String modeName() {
        return "Minefield";
    }

    @Override
    public boolean isBoardSizeAllowed(int boardSize) {
        return boardSize >= 5;
    }

    @Override
    public void initializeMatch(Player player1, Player player2, int boardSize) {
        seaMines.clear();
        int count = explicitMineCount > 0 ? explicitMineCount : (2 + RANDOM.nextInt(3)); // 2 to 4 mines

        List<Coordinate> waterCells = new ArrayList<>();
        Player targetPlayer = player2 != null ? player2 : player1;
        if (targetPlayer != null) {
            for (int r = 0; r < boardSize; r++) {
                for (int c = 0; c < boardSize; c++) {
                    Coordinate coord = new Coordinate(r, c);
                    if (targetPlayer.primaryGrid().cellStatus(coord) == CellStatus.EMPTY) {
                        waterCells.add(coord);
                    }
                }
            }
        }

        Collections.shuffle(waterCells, RANDOM);
        for (int i = 0; i < Math.min(count, waterCells.size()); i++) {
            seaMines.add(new SeaMine(waterCells.get(i)));
        }
    }

    public List<SeaMine> getSeaMines() {
        return Collections.unmodifiableList(seaMines);
    }

    public void addSeaMine(Coordinate coordinate) {
        seaMines.add(new SeaMine(coordinate));
    }

    @Override
    public List<ShotResult> onShotResolved(
            Player attacker,
            Player defender,
            Coordinate target,
            List<ShotResult> resolvedShots
    ) {
        List<ShotResult> secondaryResults = new ArrayList<>();
        for (int i = 0; i < seaMines.size(); i++) {
            SeaMine mine = seaMines.get(i);
            if (!mine.detonated() && mine.coordinate().equals(target)) {
                // Detonate the mine!
                seaMines.set(i, mine.detonate());

                // Trigger automatic secondary Cross (+) detonation centered at target
                secondaryResults.addAll(executeSecondaryCrossDetonation(defender, attacker, target));
            }
        }
        return secondaryResults;
    }

    private List<ShotResult> executeSecondaryCrossDetonation(Player defender, Player attacker, Coordinate center) {
        int boardSize = defender.primaryGrid().size();
        List<Coordinate> crossCells = List.of(
                center,
                new Coordinate(center.getRow() - 1, center.getCol()),
                new Coordinate(center.getRow() + 1, center.getCol()),
                new Coordinate(center.getRow(), center.getCol() - 1),
                new Coordinate(center.getRow(), center.getCol() + 1)
        );

        List<ShotResult> results = new ArrayList<>();
        for (Coordinate c : crossCells) {
            if (!c.isWithinBounds(boardSize)) continue;
            if (defender.primaryGrid().isCellResolved(c)) continue;
            ShotResult result = defender.primaryGrid().receiveShot(c);
            results.add(result);

            if (attacker != null && attacker.trackingGrid() != null) {
                attacker.trackingGrid().recordShotOutcome(result.coordinate(), result.outcome());
                if (result.outcome() == CellStatus.SUNK && result.shipSunk() != null) {
                    ShipSnapshot sunk = result.shipSunk();
                    attacker.trackingGrid().recordWreck(sunk.type(), sunk.cells(), sunk.orientation());
                }
            }
        }
        return results;
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
