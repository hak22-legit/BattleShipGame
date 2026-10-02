package com.battleship.model;

import com.battleship.model.projection.ShipSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * a placed ship: tracks its occupied coordinates and hit state.
 *
 * <p><strong>package-private on purpose.</strong> fixes v1.2 — the hull used to be
 * public and was handed to views and ai through {@code readonlyboard.getships()},
 * letting any caller run {@code registerhit(...)} and corrupt the defender's
 * fleet. outside this package a ship is only ever seen as a {@link shipsnapshot}.</p>
 */
final class Ship {

    private final ShipType type;
    private final List<Coordinate> occupiedCells;
    private final Orientation orientation;
    private final Set<Coordinate> hitCells = new HashSet<>();

    Ship(ShipType type, List<Coordinate> occupiedCells, Orientation orientation) {
        if (occupiedCells.size() != type.getSize()) {
            throw new IllegalArgumentException(
                    "Expected " + type.getSize() + " cells for " + type + ", got " + occupiedCells.size());
        }
        this.type = type;
        this.occupiedCells = new ArrayList<>(occupiedCells);
        this.orientation = orientation;
    }

    /** registers a hit at the given coordinate if it belongs to this ship. idempotent. */
    boolean registerHit(Coordinate c) {
        if (occupiedCells.contains(c)) {
            hitCells.add(c);
            return true;
        }
        return false;
    }

    boolean isSunk() {
        return hitCells.size() >= type.getSize();
    }

    /** immutable projection of this hull, safe to hand outside the domain. */
    ShipSnapshot snapshot() {
        return new ShipSnapshot(type, occupiedCells, orientation, hitCells.size(), isSunk());
    }

    ShipType type() {
        return type;
    }

    List<Coordinate> occupiedCells() {
        return Collections.unmodifiableList(occupiedCells);
    }
}
