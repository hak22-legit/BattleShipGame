package com.battleship.controller;

import com.battleship.model.CellStatus;
import com.battleship.model.Coordinate;
import com.battleship.model.Orientation;
import com.battleship.model.ShotResult;
import com.battleship.model.ShotTarget;
import com.battleship.model.projection.ShipSnapshot;
import com.battleship.model.weapon.Weapon;

import java.util.ArrayList;
import java.util.List;

/**
 * reusable resolver for one weapon shot against a target grid
 * (fixes v8 + f5). used by both {@link battleservice} (local match) and the
 * defending side of a network match ({@code networkbattlemediator}), so the
 * fire-resolution rules live in exactly one place instead of being
 * copy-pasted into a view class (dry + srp).
 *
 * <p>implements {@link shotresolution} so it can be injected (and swapped/mocked)
 * via the {@link #standard} singleton; the class is no longer a static-only
 * utility.</p>
 */
public final class ShotResolver implements ShotResolution {

    /** shared production instance for constructor/default injection. */
    public static final ShotResolver STANDARD = new ShotResolver();

    private ShotResolver() { }

    @Override
    public LauncherFireResult resolve(ShotTarget target, Weapon weapon,
                                      Coordinate anchor, Orientation orientation) {

        int size = target.size();
        List<Coordinate> cells = weapon.blastPattern().coverage(anchor, orientation);
        List<ShotResult> results = new ArrayList<>();
        List<ShipSnapshot> sunk = new ArrayList<>();

        for (Coordinate c : cells) {
            if (!c.isWithinBounds(size)) continue;
            if (target.isCellResolved(c)) continue;
            ShotResult r = target.receiveShot(c);
            results.add(r);
            if (r.outcome() == CellStatus.SUNK && r.shipSunk() != null
                    && sunk.stream().noneMatch(already -> already.cells().equals(r.shipSunk().cells()))) {
                sunk.add(r.shipSunk());
            }
        }

        return new LauncherFireResult(results, sunk);
    }
}
