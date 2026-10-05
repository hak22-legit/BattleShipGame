package com.battleship.model;

/**
 * hidden neutral sea mine seeded in water.
 * striking a mine triggers an automatic secondary Cross (+) detonation.
 * strictly pure domain model with zero JavaFX imports.
 */
public record SeaMine(Coordinate coordinate, boolean detonated) {

    public SeaMine(Coordinate coordinate) {
        this(coordinate, false);
    }

    public SeaMine detonate() {
        return new SeaMine(coordinate, true);
    }
}
