package com.battleship.model;

import com.battleship.model.projection.ShipSnapshot;

/**
 * outcome of a single shot. {@code shipsunk} is null unless this shot sank a ship,
 * and it is an immutable {@link shipsnapshot} rather than the live entity, so the
 * defender's internal hull cannot be mutated through a result object (v1.2).
 */
public record ShotResult(Coordinate coordinate, CellStatus outcome, ShipSnapshot shipSunk) {

    public boolean isHit() {
        return outcome == CellStatus.HIT || outcome == CellStatus.SUNK;
    }
}
