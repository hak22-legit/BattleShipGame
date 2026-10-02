package com.battleship.model;

import com.battleship.model.projection.ShipSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * aggregate root for one player's own waters: their hulls, their hit state, and
 * the fleet-deployment invariants.
 *
 * <p>renamed from {@code board} (v1.3): there is no such thing as "the board" any
 * more. each player owns a {@code primarygrid} (their real fleet) and a
 * {@link com.battleship.model.fog.trackinggrid} (what they know about the enemy).
 * opponents, views and ai never receive this object — {@link player} keeps it
 * private and exposes only the narrow {@link fleetreadout}, {@link fleetdeployment}
 * and {@link shottarget} command surfaces.</p>
 */
public class PrimaryGrid implements FleetReadout, FleetDeployment, ShotTarget {

    private final int size;
    private final CellStatus[][] grid;
    private final Ship[][] shipGrid; // null where no ship
    private final List<Ship> ships = new ArrayList<>();

    public PrimaryGrid(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("A grid needs a positive size, got " + size);
        }
        this.size = size;
        this.grid = new CellStatus[size][size];
        this.shipGrid = new Ship[size][size];
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                grid[r][c] = CellStatus.EMPTY;
            }
        }
    }

    private List<Coordinate> computeCells(ShipType type, Coordinate start, Orientation orientation) {
        boolean horizontal = orientation.isHorizontal();
        List<Coordinate> cells = new ArrayList<>();
        for (int i = 0; i < type.getSize(); i++) {
            int r = horizontal ? start.getRow() : start.getRow() + i;
            int c = horizontal ? start.getCol() + i : start.getCol();
            cells.add(new Coordinate(r, c));
        }
        return cells;
    }

    // ---------- fleetdeployment ----------

    @Override
    public boolean canDeploy(ShipType type, Coordinate start, Orientation orientation) {
        for (Coordinate c : computeCells(type, start, orientation)) {
            if (!c.isWithinBounds(size)) return false;
            if (shipGrid[c.getRow()][c.getCol()] != null) return false;
        }
        return true;
    }

    @Override
    public boolean deploy(ShipType type, Coordinate start, Orientation orientation) {
        if (!canDeploy(type, start, orientation)) return false;
        List<Coordinate> cells = computeCells(type, start, orientation);
        Ship ship = new Ship(type, cells, orientation);
        for (Coordinate c : cells) {
            shipGrid[c.getRow()][c.getCol()] = ship;
            grid[c.getRow()][c.getCol()] = CellStatus.SHIP;
        }
        ships.add(ship);
        return true;
    }

    /** removes whatever hull is currently occupying coordinate {@code c}. */
    @Override
    public boolean undeployAt(Coordinate c) {
        if (c == null || !c.isWithinBounds(size)) return false;
        Ship ship = shipGrid[c.getRow()][c.getCol()];
        if (ship == null || !ships.contains(ship)) return false;
        for (Coordinate occupied : ship.occupiedCells()) {
            shipGrid[occupied.getRow()][occupied.getCol()] = null;
            grid[occupied.getRow()][occupied.getCol()] = CellStatus.EMPTY;
        }
        ships.remove(ship);
        return true;
    }

    @Override
    public void clearDeployment() {
        ships.clear();
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                shipGrid[r][c] = null;
                grid[r][c] = CellStatus.EMPTY;
            }
        }
    }

    @Override
    public boolean canReposition(Coordinate from, Coordinate start, Orientation orientation) {
        if (from == null || !from.isWithinBounds(size)) return false;
        Ship ship = shipGrid[from.getRow()][from.getCol()];
        if (ship == null || ship.snapshot().hitCount() != 0) return false;
        for (Coordinate c : computeCells(ship.type(), start, orientation)) {
            if (!c.isWithinBounds(size)) return false;
            Ship occupying = shipGrid[c.getRow()][c.getCol()];
            if (occupying != null && occupying != ship) return false;
        }
        return true;
    }

    @Override
    public boolean reposition(Coordinate from, Coordinate start, Orientation orientation) {
        if (!canReposition(from, start, orientation)) return false;
        ShipType type = shipGrid[from.getRow()][from.getCol()].type();
        undeployAt(from);
        return deploy(type, start, orientation);
    }

    // ---------- shottarget ----------

    @Override
    public boolean isCellResolved(Coordinate c) {
        CellStatus status = cellStatus(c);
        return status == CellStatus.HIT || status == CellStatus.MISS || status == CellStatus.SUNK;
    }

    /** resolves a shot at the given coordinate and updates grid state. */
    @Override
    public ShotResult receiveShot(Coordinate c) {
        if (!c.isWithinBounds(size)) {
            throw new IllegalArgumentException("Coordinate " + c + " is out of bounds for grid of size " + size);
        }
        CellStatus existing = grid[c.getRow()][c.getCol()];
        if (existing == CellStatus.HIT || existing == CellStatus.MISS || existing == CellStatus.SUNK) {
            return new ShotResult(c, existing, null); // already resolved: no-op
        }

        Ship ship = shipGrid[c.getRow()][c.getCol()];
        if (ship == null) {
            grid[c.getRow()][c.getCol()] = CellStatus.MISS;
            return new ShotResult(c, CellStatus.MISS, null);
        }
        ship.registerHit(c);
        if (ship.isSunk()) {
            for (Coordinate sunkCell : ship.occupiedCells()) {
                grid[sunkCell.getRow()][sunkCell.getCol()] = CellStatus.SUNK;
            }
            return new ShotResult(c, CellStatus.SUNK, ship.snapshot());
        }
        grid[c.getRow()][c.getCol()] = CellStatus.HIT;
        return new ShotResult(c, CellStatus.HIT, null);
    }

    // ---------- fleetreadout ----------

    @Override
    public int size() {
        return size;
    }

    @Override
    public CellStatus cellStatus(Coordinate c) {
        if (!c.isWithinBounds(size)) {
            throw new IllegalArgumentException("Coordinate " + c + " is out of bounds for grid of size " + size);
        }
        return grid[c.getRow()][c.getCol()];
    }

    @Override
    public List<ShipSnapshot> fleet() {
        List<ShipSnapshot> snapshots = new ArrayList<>(ships.size());
        for (Ship ship : ships) snapshots.add(ship.snapshot());
        return Collections.unmodifiableList(snapshots);
    }

    @Override
    public boolean isFleetDestroyed() {
        if (ships.isEmpty()) return false;
        return ships.stream().allMatch(Ship::isSunk);
    }

    /** cells of this grid that have not been shot at yet. */
    public List<Coordinate> unshotCells() {
        List<Coordinate> result = new ArrayList<>();
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                CellStatus status = grid[r][c];
                if (status == CellStatus.EMPTY || status == CellStatus.SHIP) {
                    result.add(new Coordinate(r, c));
                }
            }
        }
        return result;
    }

    /** number of hulls currently deployed (used by the placement counter). */
    public int deployedShipCount() {
        return ships.size();
    }
}
