package com.battleship.model;

import com.battleship.model.projection.ShipSnapshot;

import java.util.List;

/**
 * the deployment commands a fleet owner accepts during ship placement.
 *
 * <p>fixes smell 5.1 (law of demeter): {@code placementservice} used to do
 * {@code player.getmutableboard().placeship(...)}, reaching through the player's
 * guts into the board. it now talks to this narrow command interface, which
 * {@link player} implements on behalf of its private grid — so the service has
 * no idea a {@code player} (or a {@code primarygrid}) exists, and no caller can
 * obtain a board and wipe it.</p>
 */
public interface FleetDeployment {

    int size();

    /** validates a deployment without mutating anything. */
    boolean canDeploy(ShipType type, Coordinate start, Orientation orientation);

    /** places a ship if the position is legal; returns whether it was placed. */
    boolean deploy(ShipType type, Coordinate start, Orientation orientation);

    /** pulls back whatever hull occupies the given cell. */
    boolean undeployAt(Coordinate c);

    /** Checks a move while allowing the hull to overlap its current cells. */
    boolean canReposition(Coordinate from, Coordinate start, Orientation orientation);

    /** Moves a hull atomically; an invalid destination leaves the original intact. */
    boolean reposition(Coordinate from, Coordinate start, Orientation orientation);

    /** removes every hull (reset in the placement ui). */
    void clearDeployment();

    /** the hulls currently deployed, as immutable snapshots. */
    List<ShipSnapshot> fleet();
}
