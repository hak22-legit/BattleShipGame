package com.battleship.model;

import com.battleship.model.projection.ShipSnapshot;

/**
 * immutable domain value object holding end-of-game statistics calculated from a fleet.
 */
public record MatchStatistics(int totalShots, int hits, int misses, double accuracy, long shipsSunk) {

    public static MatchStatistics from(FleetReadout defenderFleet) {
        int hits = 0;
        int misses = 0;
        int size = defenderFleet.size();
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                CellStatus status = defenderFleet.cellStatus(new Coordinate(r, c));
                if (status == CellStatus.HIT || status == CellStatus.SUNK) hits++;
                else if (status == CellStatus.MISS) misses++;
            }
        }
        int total = hits + misses;
        double accuracy = total == 0 ? 0.0 : (100.0 * hits / total);
        long shipsSunk = defenderFleet.fleet().stream().filter(ShipSnapshot::isSunk).count();
        return new MatchStatistics(total, hits, misses, accuracy, shipsSunk);
    }
}

