package com.battleship.model;

import java.util.List;

/**
 * convoy mode 3-cell freighter ship.
 * strictly pure domain model with zero JavaFX imports.
 */
public class FreighterShip {

    public static final ShipType TYPE = ShipType.FREIGHTER;
    public static final int SIZE = 3;

    private final List<Coordinate> cells;

    public FreighterShip(List<Coordinate> cells) {
        if (cells == null || cells.size() != SIZE) {
            throw new IllegalArgumentException("FreighterShip must have exactly " + SIZE + " cells.");
        }
        this.cells = List.copyOf(cells);
    }

    public List<Coordinate> cells() {
        return cells;
    }
}
