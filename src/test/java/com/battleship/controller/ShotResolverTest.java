package com.battleship.controller;

import com.battleship.model.CellStatus;
import com.battleship.model.Coordinate;
import com.battleship.model.HumanPlayer;
import com.battleship.model.Orientation;
import com.battleship.model.Player;
import com.battleship.model.PrimaryGrid;
import com.battleship.model.ShipType;
import com.battleship.model.ShotResult;
import com.battleship.model.weapon.WeaponCatalog;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** validates the extracted {@link shotresolver} shared by local and network play. */
class ShotResolverTest {

    @Test
    void singleShotOnEmptyCellIsAMiss() {
        PrimaryGrid grid = new PrimaryGrid(10);

        LauncherFireResult result = ShotResolver.STANDARD.resolve(
                grid, WeaponCatalog.standardShell(), new Coordinate(3, 4), Orientation.HORIZONTAL);

        assertEquals(1, result.results().size());
        assertEquals(CellStatus.MISS, result.results().get(0).outcome());
        assertTrue(result.sunkShips().isEmpty());
        assertFalse(result.anyHit());
    }

    @Test
    void alreadyResolvedAndOutOfBoundsCellsAreSkipped() {
        PrimaryGrid grid = new PrimaryGrid(10);
        // salvo anchored at (5,8) horizontally covers (5,8),(5,9),(5,10) — the last is oob.
        LauncherFireResult first = ShotResolver.STANDARD.resolve(
                grid, WeaponCatalog.salvoBarrage(), new Coordinate(5, 8), Orientation.HORIZONTAL);
        assertEquals(2, first.results().size());

        // re-resolving the same pattern must yield nothing: all live cells were resolved.
        LauncherFireResult second = ShotResolver.STANDARD.resolve(
                grid, WeaponCatalog.salvoBarrage(), new Coordinate(5, 8), Orientation.HORIZONTAL);
        assertTrue(second.results().isEmpty());
        assertTrue(second.sunkShips().isEmpty());
    }

    @Test
    void areaShotSinksShipAndReportsItOnce() {
        PrimaryGrid grid = new PrimaryGrid(10);
        assertTrue(grid.deploy(ShipType.PATROL_BOAT, new Coordinate(0, 0), Orientation.HORIZONTAL));

        // nuclear pattern (2x3 from anchor) covers both patrol boat cells.
        LauncherFireResult result = ShotResolver.STANDARD.resolve(
                grid, WeaponCatalog.nuclearWarhead(), new Coordinate(0, 0), Orientation.HORIZONTAL);

        assertEquals(6, result.results().size());
        assertEquals(1, result.sunkShips().size());
        assertTrue(result.results().stream().anyMatch(r -> r.outcome() == CellStatus.SUNK));
        assertTrue(grid.isFleetDestroyed());
    }

    @Test
    void shotResultsExposeCoordinate() {
        PrimaryGrid grid = new PrimaryGrid(10);
        ShotResult r = ShotResolver.STANDARD.resolve(grid, WeaponCatalog.standardShell(), new Coordinate(2, 2),
                Orientation.HORIZONTAL).results().get(0);
        assertEquals(new Coordinate(2, 2), r.coordinate());
    }
}
