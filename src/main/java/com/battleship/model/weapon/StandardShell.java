package com.battleship.model.weapon;

/**
 * infinite single-cell artillery — the weapon every admiral always has.
 */
public final class StandardShell implements Weapon {

    public static final String ID = "DEFAULT";

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
        return BlastPattern.of(1, 1);
    }
}
