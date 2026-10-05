package com.battleship.model;

import com.battleship.model.projection.ShipSnapshot;
import com.battleship.model.weapon.WeaponType;

import java.util.List;

/**
 * immutable record containing all affected coordinates, shot outcomes, and sunk ships
 * from an attack execution.
 * strictly pure domain record with zero JavaFX imports.
 */
public record AttackResult(
        Coordinate target,
        WeaponType weaponType,
        List<ShotResult> shotResults,
        List<ShipSnapshot> sunkShips,
        int hits,
        int misses,
        boolean ammoDeducted,
        boolean cancelled
) {

    public AttackResult {
        shotResults = List.copyOf(shotResults);
        sunkShips = List.copyOf(sunkShips);
    }

    public static AttackResult cancelled(Coordinate target, WeaponType weaponType) {
        return new AttackResult(target, weaponType, List.of(), List.of(), 0, 0, false, true);
    }

    public boolean anyHit() {
        return hits > 0;
    }

    public boolean anySunk() {
        return !sunkShips.isEmpty();
    }
}
