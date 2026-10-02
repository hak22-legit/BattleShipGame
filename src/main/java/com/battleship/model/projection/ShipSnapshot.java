package com.battleship.model.projection;

import com.battleship.model.Coordinate;
import com.battleship.model.Orientation;
import com.battleship.model.ShipType;

import java.util.List;

/**
 * immutable projection of a placed ship, safe to hand to ui or ai layers.
 *
 * <p>fixes v1.2: the mutable {@code ship} domain entity is never exposed outside
 * {@code com.battleship.model} any more. callers used to be able to write
 * {@code readonlyboard.getships().get(0).registerhit(c)} and corrupt the game.
 * this record has no mutating methods, and its coordinate list is defensively
 * copied, so it satisfies the read-only contract it is handed out under
 * (liskov substitution principle).</p>
 *
 * @param type       the ship class
 * @param cells      every occupied coordinate, in hull order
 * @param orientation the hull's orientation
 * @param hitcount   how many of the occupied cells have been hit
 * @param issunk     whether the whole hull has been destroyed
 */
public record ShipSnapshot(
        ShipType type,
        List<Coordinate> cells,
        Orientation orientation,
        int hitCount,
        boolean isSunk
) {
    public ShipSnapshot {
        if (cells == null) {
            throw new IllegalArgumentException("A ship snapshot requires its occupied cells.");
        }
        cells = List.copyOf(cells);
    }

    /** convenience: the hull length in cells. */
    public int length() {
        return cells.size();
    }
}
