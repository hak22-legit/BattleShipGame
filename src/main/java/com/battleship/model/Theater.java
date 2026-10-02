package com.battleship.model;

import java.util.Map;

/**
 * the three playable board configurations: skirmish (5x5), engagement (8x8),
 * fleet action (10x10). each defines board size and required fleet composition.
 */
public enum Theater {

    SKIRMISH(5, Map.of(
            ShipType.PATROL_BOAT, 2,
            ShipType.SUBMARINE, 1
    )),
    ENGAGEMENT(8, Map.of(
            ShipType.DESTROYER, 2,
            ShipType.SUBMARINE, 2,
            ShipType.BATTLESHIP, 1
    )),
    FLEET_ACTION(10, Map.of(
            ShipType.DESTROYER, 2,
            ShipType.SUBMARINE, 2,
            ShipType.CRUISER, 1,
            ShipType.BATTLESHIP, 1,
            ShipType.CARRIER, 1
    ));

    private final int boardSize;
    private final Map<ShipType, Integer> fleetComposition;

    Theater(int boardSize, Map<ShipType, Integer> fleetComposition) {
        this.boardSize = boardSize;
        this.fleetComposition = fleetComposition;
    }

    public int getBoardSize() { return boardSize; }
    public Map<ShipType, Integer> getFleetComposition() { return fleetComposition; }

    /** total number of individual ship instances in the fleet. */
    public int getTotalShipCount() {
        return fleetComposition.values().stream().mapToInt(Integer::intValue).sum();
    }

    /** total hits required across the whole fleet to achieve victory. */
    public int getTotalHitsToWin() {
        return fleetComposition.entrySet().stream()
                .mapToInt(e -> e.getKey().getSize() * e.getValue())
                .sum();
    }

    public String getDisplayName() {
        return switch (this) {
            case SKIRMISH -> "QUICK MATCH";
            case ENGAGEMENT -> "STANDARD";
            case FLEET_ACTION -> "CLASSIC";
        };
    }

    public String getSessionEstimate() {
        return switch (this) {
            case SKIRMISH -> "~5 min";
            case ENGAGEMENT -> "~10 min";
            case FLEET_ACTION -> "~15-20 min";
        };
    }
}
