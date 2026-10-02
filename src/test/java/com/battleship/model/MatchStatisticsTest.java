package com.battleship.model;

import com.battleship.model.projection.ShipSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MatchStatisticsTest {

    private record FakeFleet(int size, CellStatus[][] grid, List<ShipSnapshot> ships) implements FleetReadout {
        @Override
        public CellStatus cellStatus(Coordinate c) {
            return grid[c.getRow()][c.getCol()];
        }

        @Override
        public List<ShipSnapshot> fleet() {
            return ships;
        }

        @Override
        public boolean isFleetDestroyed() {
            return ships.stream().allMatch(ShipSnapshot::isSunk);
        }
    }

    @Test
    void calculatesZeroStatsForUnshotFleet() {
        int size = 5;
        CellStatus[][] grid = new CellStatus[size][size];
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                grid[r][c] = CellStatus.EMPTY;
            }
        }
        ShipSnapshot ship = new ShipSnapshot(ShipType.PATROL_BOAT,
                List.of(new Coordinate(0, 0), new Coordinate(0, 1)), Orientation.HORIZONTAL, 0, false);
        FakeFleet fleet = new FakeFleet(size, grid, List.of(ship));

        MatchStatistics stats = MatchStatistics.from(fleet);

        assertEquals(0, stats.totalShots());
        assertEquals(0, stats.hits());
        assertEquals(0, stats.misses());
        assertEquals(0.0, stats.accuracy());
        assertEquals(0, stats.shipsSunk());
    }

    @Test
    void calculatesHitsMissesAccuracyAndSunkShips() {
        int size = 5;
        CellStatus[][] grid = new CellStatus[size][size];
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                grid[r][c] = CellStatus.EMPTY;
            }
        }
        // 2 misses
        grid[1][1] = CellStatus.MISS;
        grid[2][2] = CellStatus.MISS;
        // 1 hit (afloat)
        grid[3][0] = CellStatus.HIT;
        // 2 sunk cells
        grid[0][0] = CellStatus.SUNK;
        grid[0][1] = CellStatus.SUNK;

        ShipSnapshot afloatShip = new ShipSnapshot(ShipType.SUBMARINE,
                List.of(new Coordinate(3, 0), new Coordinate(3, 1), new Coordinate(3, 2)), Orientation.HORIZONTAL, 1, false);
        ShipSnapshot sunkShip = new ShipSnapshot(ShipType.PATROL_BOAT,
                List.of(new Coordinate(0, 0), new Coordinate(0, 1)), Orientation.HORIZONTAL, 2, true);

        FakeFleet fleet = new FakeFleet(size, grid, List.of(afloatShip, sunkShip));

        MatchStatistics stats = MatchStatistics.from(fleet);

        // 3 hits total (1 hit + 2 sunk) + 2 misses = 5 total shots
        assertEquals(5, stats.totalShots());
        assertEquals(3, stats.hits());
        assertEquals(2, stats.misses());
        assertEquals(60.0, stats.accuracy(), 0.001);
        assertEquals(1, stats.shipsSunk());
    }
}

