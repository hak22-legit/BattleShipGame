package com.battleship.model.weapon;

/**
 * three-cell line salvo that only unlocks on larger battlefields and carries
 * limited ammunition (3 rounds on a 10x10, 2 on an 8x8, none below).
 */
public final class SalvoBarrage implements Weapon {

    public static final String ID = "LEVEL_2";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "Level 2";
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
        return BlastPattern.of(1, 3); // 1x3 horizontally, 3x1 vertically
    }
}
