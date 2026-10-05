package com.battleship.model;

import com.battleship.model.fog.MarkerStatus;
import com.battleship.model.fog.TrackingGrid;
import com.battleship.model.weapon.CrossBomb;
import com.battleship.model.weapon.NuclearWarhead;
import com.battleship.model.weapon.StandardShell;
import com.battleship.model.weapon.WeaponCatalog;
import com.battleship.model.weapon.WeaponType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AttackServiceTest {

    private final AttackService attackService = new AttackService();

    @Test
    void dynamicAmmunitionMatrixStrictlyFollowsBoardSize() {
        PlayerArsenal a5 = new PlayerArsenal(5);
        assertEquals(Integer.MAX_VALUE, a5.ammoCount(WeaponType.SINGLE));
        assertEquals(0, a5.ammoCount(WeaponType.CROSS));
        assertEquals(0, a5.ammoCount(WeaponType.NUCLEAR));
        assertTrue(a5.canFire(WeaponType.SINGLE));
        assertFalse(a5.canFire(WeaponType.CROSS));
        assertFalse(a5.canFire(WeaponType.NUCLEAR));

        PlayerArsenal a8 = new PlayerArsenal(8);
        assertEquals(Integer.MAX_VALUE, a8.ammoCount(WeaponType.SINGLE));
        assertEquals(2, a8.ammoCount(WeaponType.CROSS));
        assertEquals(0, a8.ammoCount(WeaponType.NUCLEAR));
        assertTrue(a8.canFire(WeaponType.SINGLE));
        assertTrue(a8.canFire(WeaponType.CROSS));
        assertFalse(a8.canFire(WeaponType.NUCLEAR));

        PlayerArsenal a10 = new PlayerArsenal(10);
        assertEquals(Integer.MAX_VALUE, a10.ammoCount(WeaponType.SINGLE));
        assertEquals(3, a10.ammoCount(WeaponType.CROSS));
        assertEquals(1, a10.ammoCount(WeaponType.NUCLEAR));
        assertTrue(a10.canFire(WeaponType.SINGLE));
        assertTrue(a10.canFire(WeaponType.CROSS));
        assertTrue(a10.canFire(WeaponType.NUCLEAR));
    }

    @Test
    void consumeAmmoDecrementsCorrectly() {
        PlayerArsenal arsenal = new PlayerArsenal(10);
        arsenal.consumeAmmo(WeaponType.CROSS);
        assertEquals(2, arsenal.ammoCount(WeaponType.CROSS));
        arsenal.consumeAmmo(WeaponType.CROSS);
        assertEquals(1, arsenal.ammoCount(WeaponType.CROSS));
        arsenal.consumeAmmo(WeaponType.CROSS);
        assertEquals(0, arsenal.ammoCount(WeaponType.CROSS));
        assertFalse(arsenal.canFire(WeaponType.CROSS));
        assertThrows(IllegalStateException.class, () -> arsenal.consumeAmmo(WeaponType.CROSS));

        arsenal.consumeAmmo(WeaponType.NUCLEAR);
        assertEquals(0, arsenal.ammoCount(WeaponType.NUCLEAR));
        assertFalse(arsenal.canFire(WeaponType.NUCLEAR));
        assertThrows(IllegalStateException.class, () -> arsenal.consumeAmmo(WeaponType.NUCLEAR));

        // Single is infinite, never decrements or throws
        arsenal.consumeAmmo(WeaponType.SINGLE);
        assertEquals(Integer.MAX_VALUE, arsenal.ammoCount(WeaponType.SINGLE));
        assertTrue(arsenal.canFire(WeaponType.SINGLE));
    }

    @Test
    void crossPatternResolves5CellsAndClipsOutOfBoundsWithoutException() {
        PrimaryGrid grid = new PrimaryGrid(8);
        PlayerArsenal arsenal = new PlayerArsenal(8);
        TrackingGrid knowledge = new TrackingGrid(8, Map.of());

        // Cross at top-left corner (0, 0): Center=(0,0), N=(-1,0 OOB), S=(1,0), W=(0,-1 OOB), E=(0,1).
        // Valid in-bounds: 3 cells.
        AttackResult result = attackService.executeAttack(
                arsenal,
                grid,
                knowledge,
                WeaponType.CROSS,
                new Coordinate(0, 0),
                Orientation.HORIZONTAL,
                true,
                new MatchConfig(8, false)
        );

        assertFalse(result.cancelled());
        assertTrue(result.ammoDeducted());
        assertEquals(1, arsenal.ammoCount(WeaponType.CROSS));
        assertEquals(3, result.shotResults().size());
        assertEquals(3, result.misses());
        assertEquals(0, result.hits());

        // Check cells updated to MISS on grid and knowledge
        assertEquals(CellStatus.MISS, grid.cellStatus(new Coordinate(0, 0)));
        assertEquals(CellStatus.MISS, grid.cellStatus(new Coordinate(1, 0)));
        assertEquals(CellStatus.MISS, grid.cellStatus(new Coordinate(0, 1)));
        assertEquals(MarkerStatus.MISS, knowledge.observedStatus(new Coordinate(0, 0)));
        assertEquals(MarkerStatus.MISS, knowledge.observedStatus(new Coordinate(1, 0)));
        assertEquals(MarkerStatus.MISS, knowledge.observedStatus(new Coordinate(0, 1)));
    }

    @Test
    void nuclearStrikeDamagesMultiSegmentShipSimultaneously() {
        PrimaryGrid grid = new PrimaryGrid(10);
        PlayerArsenal arsenal = new PlayerArsenal(10);
        TrackingGrid knowledge = new TrackingGrid(10, Map.of());

        // Deploy Destroyer (size 2) at (4, 4) and (4, 5)
        assertTrue(grid.deploy(ShipType.DESTROYER, new Coordinate(4, 4), Orientation.HORIZONTAL));

        // Nuclear strike centered at (4, 4): covers (3..5, 3..5), which includes both (4,4) and (4,5)
        AttackResult result = attackService.executeAttack(
                arsenal,
                grid,
                knowledge,
                WeaponType.NUCLEAR,
                new Coordinate(4, 4),
                Orientation.HORIZONTAL,
                true,
                new MatchConfig(10, false)
        );

        assertFalse(result.cancelled());
        assertTrue(result.ammoDeducted());
        assertEquals(0, arsenal.ammoCount(WeaponType.NUCLEAR));
        assertEquals(9, result.shotResults().size()); // 3x3 block fully in bounds
        assertEquals(2, result.hits());
        assertEquals(7, result.misses());
        assertEquals(1, result.sunkShips().size());
        assertTrue(grid.isFleetDestroyed());

        // Both destroyer segments sank simultaneously
        assertEquals(CellStatus.SUNK, grid.cellStatus(new Coordinate(4, 4)));
        assertEquals(CellStatus.SUNK, grid.cellStatus(new Coordinate(4, 5)));
        assertEquals(1, knowledge.confirmedSunk().size());
    }

    @Test
    void nuclearQuizGateRejectsStrikeAndPreservesAmmunitionWhenTriviaFails() {
        PrimaryGrid grid = new PrimaryGrid(10);
        PlayerArsenal arsenal = new PlayerArsenal(10);
        arsenal.select(WeaponType.NUCLEAR);
        TrackingGrid knowledge = new TrackingGrid(10, Map.of());

        // Trivia authorization failed (quizAuthorized = false)
        AttackResult result = attackService.executeAttack(
                arsenal,
                grid,
                knowledge,
                WeaponType.NUCLEAR,
                new Coordinate(5, 5),
                Orientation.HORIZONTAL,
                false, // FAILED TRIVIA
                new MatchConfig(10, true) // QUIZ ENABLED
        );

        assertTrue(result.cancelled());
        assertFalse(result.ammoDeducted());
        assertEquals(1, arsenal.ammoCount(WeaponType.NUCLEAR), "Ammo must remain untouched");
        assertEquals(WeaponType.SINGLE, arsenal.selectedType(), "Weapon must revert to SINGLE");
        assertTrue(result.shotResults().isEmpty(), "No shots fired on target grid");
        assertEquals(CellStatus.EMPTY, grid.cellStatus(new Coordinate(5, 5)));
    }
}
