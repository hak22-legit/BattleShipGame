package com.battleship.model.weapon;

/**
 * canonical weapon types for naval command.
 * strictly pure domain enum with zero JavaFX imports.
 */
public enum WeaponType {
    SINGLE("SINGLE", "Default", true),
    CROSS("CROSS", "Cross Bomb", false),
    NUCLEAR("NUCLEAR", "Nuclear", false);

    private final String id;
    private final String displayName;
    private final boolean infiniteAmmo;

    WeaponType(String id, String displayName, boolean infiniteAmmo) {
        this.id = id;
        this.displayName = displayName;
        this.infiniteAmmo = infiniteAmmo;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public boolean hasInfiniteAmmo() {
        return infiniteAmmo;
    }

    /**
     * ammunition allocated at match start according to board size matrix:
     * - 5x5: Single = inf, Cross = 0, Nuclear = 0
     * - 8x8: Single = inf, Cross = 2, Nuclear = 0
     * - 10x10: Single = inf, Cross = 3, Nuclear = 1
     */
    public int startingAmmo(int boardSize) {
        return switch (this) {
            case SINGLE -> Integer.MAX_VALUE;
            case CROSS -> boardSize >= 10 ? 3 : (boardSize >= 8 ? 2 : 0);
            case NUCLEAR -> boardSize >= 10 ? 1 : 0;
        };
    }

    /**
     * whether this weapon is unlocked for a given board size:
     * - 5x5: Single only
     * - 8x8: Single and Cross
     * - 10x10: Single, Cross, and Nuclear
     */
    public boolean availableFor(int boardSize) {
        return switch (this) {
            case SINGLE -> true;
            case CROSS -> boardSize >= 8;
            case NUCLEAR -> boardSize >= 10;
        };
    }

    /** looks up weapon type by string id (case-insensitive). */
    public static WeaponType fromId(String id) {
        if (id == null) return SINGLE;
        for (WeaponType type : values()) {
            if (type.id.equalsIgnoreCase(id) || type.name().equalsIgnoreCase(id)) {
                return type;
            }
        }
        if ("DEFAULT".equalsIgnoreCase(id)) {
            return SINGLE;
        }
        return SINGLE;
    }
}
