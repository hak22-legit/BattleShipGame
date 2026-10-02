package com.battleship.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ShipRepositionTest {
    @Test void rotatesAcrossItsOwnCellsWithoutDuplicatingTheShip() {
        PrimaryGrid grid = new PrimaryGrid(5);
        grid.deploy(ShipType.SUBMARINE, new Coordinate(1, 1), Orientation.HORIZONTAL);
        assertTrue(grid.reposition(new Coordinate(1, 2), new Coordinate(1, 1), Orientation.VERTICAL));
        assertEquals(1, grid.fleet().size());
        assertEquals(Orientation.VERTICAL, grid.fleet().get(0).orientation());
        assertEquals(CellStatus.EMPTY, grid.cellStatus(new Coordinate(1, 2)));
        assertEquals(CellStatus.SHIP, grid.cellStatus(new Coordinate(3, 1)));
    }

    @Test void invalidMovesPreserveBothShipsAndAllCells() {
        PrimaryGrid grid = new PrimaryGrid(5);
        grid.deploy(ShipType.SUBMARINE, new Coordinate(0, 0), Orientation.HORIZONTAL);
        grid.deploy(ShipType.PATROL_BOAT, new Coordinate(2, 0), Orientation.HORIZONTAL);
        var before = grid.fleet();
        assertFalse(grid.reposition(new Coordinate(0, 0), new Coordinate(0, 0), Orientation.VERTICAL));
        assertFalse(grid.reposition(new Coordinate(0, 0), new Coordinate(4, 4), Orientation.HORIZONTAL));
        assertEquals(before, grid.fleet());
        assertEquals(CellStatus.SHIP, grid.cellStatus(new Coordinate(0, 2)));
        assertEquals(CellStatus.SHIP, grid.cellStatus(new Coordinate(2, 1)));
    }

    @Test void cannotMoveADamagedShip() {
        PrimaryGrid grid = new PrimaryGrid(5);
        grid.deploy(ShipType.PATROL_BOAT, new Coordinate(0, 0), Orientation.HORIZONTAL);
        grid.receiveShot(new Coordinate(0, 0));
        assertFalse(grid.reposition(new Coordinate(0, 1), new Coordinate(3, 0), Orientation.HORIZONTAL));
        assertEquals(1, grid.fleet().get(0).hitCount());
    }
}
