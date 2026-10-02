package com.battleship.ai;

import com.battleship.model.AmmoReadout;
import com.battleship.model.Coordinate;
import com.battleship.model.Orientation;
import com.battleship.model.ShotOrder;
import com.battleship.model.ShotResult;
import com.battleship.model.fog.TrackingGrid;
import com.battleship.model.weapon.WeaponCatalog;

/**
 * strategy pattern contract for ai opponents. pure logic — no javafx, and no
 * access to the opponent's real grid.
 *
 * <p>fixes v1.3: the parameter is a {@link trackinggrid} (what the admiral knows)
 * instead of the defender's {@code board}. an ai physically cannot read the
 * enemy's ship layout any more; it reasons from shot outcomes, announced wrecks
 * and the published fleet roster exactly like a human does.</p>
 *
 * <p>ammunition arrives as a read-only {@link ammoreadout} (fixes v1), so a
 * strategy may plan around its stock but can never consume or resupply it.</p>
 */
public interface AIStrategy {

    /** chooses the next cell to fire at, given everything known so far. */
    Coordinate chooseTarget(TrackingGrid knowledge);

    /** optional hook so stateful strategies (hunt/target) can update after each shot. */
    void notifyResult(ShotResult result);

    /**
     * chooses a full firing order (weapon + anchor + orientation) for this turn.
     * the default fires the infinite standard shell through {@link #choosetarget},
     * so a strategy that never overrides this stays on the standard weapon.
     */
    default ShotOrder chooseShotPlan(TrackingGrid knowledge, AmmoReadout ammo) {
        return new ShotOrder(WeaponCatalog.standard(), chooseTarget(knowledge), Orientation.HORIZONTAL);
    }
}
