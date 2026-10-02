package com.battleship.model.fog;

/**
 * what a player can know about one enemy cell.
 *
 * <p>deliberately <em>not</em> {@code cellstatus}: the tracking grid can never
 * hold {@code ship} or {@code empty}, because those are facts about the
 * opponent's real grid. modelling knowledge with its own closed set of markers
 * makes the fog-of-war leak (v1.3) impossible to express.</p>
 */
public enum MarkerStatus {
    /** never observed — fog of war. */
    UNKNOWN,
    /** a shot landed in open water. */
    MISS,
    /** a shot hit a hull that is still afloat. */
    HIT,
    /** part of a hull that has been reported sunk. */
    SUNK;

    /** true once a shot has been resolved on this cell. */
    public boolean isShelled() {
        return this != UNKNOWN;
    }

    /** maps a resolved grid cell outcome into a fog-of-war tracking marker. */
    public static MarkerStatus from(com.battleship.model.CellStatus status) {
        return switch (status) {
            case HIT -> HIT;
            case SUNK -> SUNK;
            default -> MISS;
        };
    }
}

