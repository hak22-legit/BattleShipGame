package com.battleship.model.weapon;

import com.battleship.model.Coordinate;
import com.battleship.model.Orientation;

import java.util.List;

/**
 * canonical single-cell artillery (1x1 target cell): unlimited ammo (∞) on all board sizes.
 * strictly pure domain class with zero JavaFX imports.
 */
public final class StandardShell implements Weapon {

    public static final String ID = "SINGLE";
    public static final String LEGACY_ID = "DEFAULT";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "Default";
    }

    @Override
    public int startingAmmo(int boardSize) {
        return Integer.MAX_VALUE; // infinite
    }

    @Override
    public boolean availableFor(int boardSize) {
        return true;
    }

    @Override
    public boolean hasInfiniteAmmo() {
        return true;
    }

    @Override
    public BlastPattern blastPattern() {
        return BlastPattern.single();
    }

    @Override
    public List<Coordinate> calculateBlastArea(Coordinate anchor, Orientation orientation) {
        return List.of(anchor);
    }
}
