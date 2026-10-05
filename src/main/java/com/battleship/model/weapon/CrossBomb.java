package com.battleship.model.weapon;

import com.battleship.model.Coordinate;
import com.battleship.model.Orientation;

import java.util.ArrayList;
import java.util.List;

/**
 * five-cell '+' area weapon: center + north, south, east, west.
 * unlocks on medium/large boards with limited ammunition (2 on 8x8, 3 on 10x10, 0 on 5x5).
 * coordinates out of bounds are clipped safely without throwing IndexOutOfBoundsException.
 * strictly pure domain class with zero JavaFX imports.
 */
public final class CrossBomb implements Weapon {

    public static final String ID = "CROSS";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "Cross Bomb";
    }

    @Override
    public int startingAmmo(int boardSize) {
        if (boardSize >= 10) return 3;
        if (boardSize >= 8) return 2;
        return 0;
    }

    @Override
    public boolean availableFor(int boardSize) {
        return boardSize >= 8;
    }

    @Override
    public boolean hasInfiniteAmmo() {
        return false;
    }

    @Override
    public BlastPattern blastPattern() {
        return BlastPattern.cross();
    }

    @Override
    public List<Coordinate> calculateBlastArea(Coordinate anchor, Orientation orientation) {
        List<Coordinate> cells = new ArrayList<>(5);
        cells.add(anchor); // center
        cells.add(new Coordinate(anchor.getRow() - 1, anchor.getCol())); // north
        cells.add(new Coordinate(anchor.getRow() + 1, anchor.getCol())); // south
        cells.add(new Coordinate(anchor.getRow(), anchor.getCol() - 1)); // west
        cells.add(new Coordinate(anchor.getRow(), anchor.getCol() + 1)); // east
        return cells;
    }

    @Override
    public void playFiringSound(com.battleship.view.SfxAudio audio) {
        audio.playFire();
    }
}
