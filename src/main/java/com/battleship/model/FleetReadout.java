package com.battleship.model;

import com.battleship.model.projection.ShipSnapshot;

import java.util.List;

/**
 * read-only projection of <em>your own</em> grid, safe to hand to the ui.
 *
 * <p>replaces {@code readonlyboard} (v1.2). two things changed: the fleet is
 * projected as immutable {@link shipsnapshot}s instead of live {@link ship}
 * entities, and the mutating operations are not merely absent from the interface
 * but unreachable, because nothing hands out the underlying aggregate at all.</p>
 */
public interface FleetReadout {

    int size();

    /** resolved state of one of your own cells (may legitimately be ship). */
    CellStatus cellStatus(Coordinate c);

    /** your hulls as immutable snapshots. */
    List<ShipSnapshot> fleet();

    /** true once every one of your hulls has been destroyed. */
    boolean isFleetDestroyed();
}
