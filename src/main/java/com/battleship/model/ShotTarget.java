package com.battleship.model;

/**
 * the defensive surface of a fleet: the only operations an opponent's shot is
 * allowed to perform against someone's primary grid.
 *
 * <p>this is the "pass commands to the aggregate" half of the v1.1 fix. the old
 * api handed out a fully mutable {@code board} so that services could place
 * ships, remove ships, shell cells and read the whole layout. a shot actually
 * needs exactly one command — {@link #receiveshot(coordinate)} — plus the
 * ability to skip cells that were already resolved.</p>
 */
public interface ShotTarget {

    int size();

    /** true when the cell has already been hit, miss or sunk. */
    boolean isCellResolved(Coordinate c);

    /** resolves a shot: the grid applies its own hit/sink invariants and reports the outcome. */
    ShotResult receiveShot(Coordinate c);
}
