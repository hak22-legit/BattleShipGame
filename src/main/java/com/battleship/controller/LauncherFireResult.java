package com.battleship.controller;

import com.battleship.model.ShotResult;
import com.battleship.model.projection.ShipSnapshot;

import java.util.List;

/**
 * outcome of one launcher shot, which may cover multiple cells (salvo, nuclear)
 * and therefore may register multiple hits/misses and sink multiple ships at once.
 *
 * <p>sunk hulls are reported as immutable {@link shipsnapshot}s (v1.2), so a view
 * or a network peer can never reach back into the defender's fleet.</p>
 */
public record LauncherFireResult(List<ShotResult> results, List<ShipSnapshot> sunkShips) {

    public LauncherFireResult {
        results = List.copyOf(results);
        sunkShips = List.copyOf(sunkShips);
    }

    public boolean anyHit() {
        return results.stream().anyMatch(ShotResult::isHit);
    }

    public boolean anySunk() {
        return !sunkShips.isEmpty();
    }
}
