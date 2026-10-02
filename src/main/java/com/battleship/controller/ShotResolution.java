package com.battleship.controller;

import com.battleship.model.Coordinate;
import com.battleship.model.Orientation;
import com.battleship.model.ShotTarget;
import com.battleship.model.weapon.Weapon;

/**
 * injectable strategy for resolving one weapon shot against a target grid
 * (fixes f5, dip). production uses {@link shotresolver#standard}; tests can
 * substitute a stub/fake without any static coupling.
 *
 * <p>the target is a {@link shottarget} — the narrow "receive a shot" command
 * surface — rather than a mutable board (v1.1).</p>
 */
@FunctionalInterface
public interface ShotResolution {

    /**
     * fires the given weapon's blast pattern against the target.
     * already-resolved cells and out-of-bounds cells within the pattern are
     * skipped; ammunition bookkeeping is the caller's responsibility.
     *
     * @return the aggregated result (per-cell outcomes + hulls sunk by this shot)
     */
    LauncherFireResult resolve(ShotTarget target, Weapon weapon,
                               Coordinate anchor, Orientation orientation);
}
