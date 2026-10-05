package com.battleship.model.weapon;

import com.battleship.model.Coordinate;
import com.battleship.model.Orientation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * a first-class blast geometry: the cell footprint a weapon covers from an anchor.
 * strictly pure domain model with zero JavaFX imports.
 *
 * @param rows cells covered vertically in the horizontal layout
 * @param cols cells covered horizontally in the horizontal layout
 * @param relativeOffsets optional custom offsets relative to the target anchor (e.g. for cross or centered patterns)
 */
public record BlastPattern(int rows, int cols, List<Coordinate> relativeOffsets) {

    public BlastPattern {
        if (rows <= 0 || cols <= 0) {
            throw new IllegalArgumentException("A blast pattern needs positive dimensions, got "
                    + rows + "x" + cols);
        }
        if (relativeOffsets != null) {
            relativeOffsets = List.copyOf(relativeOffsets);
        }
    }

    public BlastPattern(int rows, int cols) {
        this(rows, cols, null);
    }

    public static BlastPattern of(int rows, int cols) {
        return new BlastPattern(rows, cols);
    }

    public static BlastPattern single() {
        return new BlastPattern(1, 1, List.of(new Coordinate(0, 0)));
    }

    public static BlastPattern cross() {
        List<Coordinate> offsets = List.of(
                new Coordinate(0, 0),   // center
                new Coordinate(-1, 0),  // north
                new Coordinate(1, 0),   // south
                new Coordinate(0, -1),  // west
                new Coordinate(0, 1)    // east
        );
        return new BlastPattern(3, 3, offsets);
    }

    public static BlastPattern nuclear() {
        List<Coordinate> offsets = new ArrayList<>(9);
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                offsets.add(new Coordinate(dr, dc));
            }
        }
        return new BlastPattern(3, 3, offsets);
    }

    /** the same footprint rotated into the requested firing orientation. */
    public BlastPattern rotatedTo(Orientation orientation) {
        if (orientation.isHorizontal()) {
            return this;
        }
        if (relativeOffsets != null) {
            List<Coordinate> transposed = new ArrayList<>(relativeOffsets.size());
            for (Coordinate c : relativeOffsets) {
                transposed.add(new Coordinate(c.getCol(), c.getRow()));
            }
            return new BlastPattern(cols, rows, transposed);
        }
        return new BlastPattern(cols, rows);
    }

    /** total cells covered from the anchor. */
    public int cellCount() {
        return relativeOffsets != null ? relativeOffsets.size() : (rows * cols);
    }

    /** the orientation this pattern was authored in (used by ai block scoring). */
    public Orientation naturalOrientation() {
        return rows <= cols ? Orientation.HORIZONTAL : Orientation.VERTICAL;
    }

    /**
     * coordinates covered when this pattern is already oriented.
     */
    public List<Coordinate> coverage(Coordinate anchor) {
        if (relativeOffsets != null) {
            List<Coordinate> cells = new ArrayList<>(relativeOffsets.size());
            for (Coordinate offset : relativeOffsets) {
                cells.add(new Coordinate(anchor.getRow() + offset.getRow(), anchor.getCol() + offset.getCol()));
            }
            return cells;
        }
        List<Coordinate> cells = new ArrayList<>(cellCount());
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                cells.add(new Coordinate(anchor.getRow() + r, anchor.getCol() + c));
            }
        }
        return cells;
    }

    /**
     * every coordinate covered when the pattern is anchored at {@code anchor}.
     * cells outside the board are still returned — callers clip them, which keeps
     * the pattern itself independent of any particular battlefield size.
     */
    public List<Coordinate> coverage(Coordinate anchor, Orientation orientation) {
        return rotatedTo(orientation).coverage(anchor);
    }

    /**
     * coordinates covered and safely clipped to stay within the given board size.
     * coordinates out of bounds are discarded without throwing IndexOutOfBoundsException.
     */
    public List<Coordinate> coverageWithinBounds(Coordinate anchor, Orientation orientation, int boardSize) {
        List<Coordinate> all = coverage(anchor, orientation);
        List<Coordinate> inBounds = new ArrayList<>(all.size());
        for (Coordinate c : all) {
            if (c.isWithinBounds(boardSize)) {
                inBounds.add(c);
            }
        }
        return inBounds;
    }

    /**
     * the anchor offset that keeps the whole pattern inside a board of the given size
     * when a player clicks near the right/bottom edge, or {@code null} when the
     * pattern simply cannot fit.
     */
    public Coordinate clampAnchor(Coordinate anchor, Orientation orientation, int boardSize) {
        BlastPattern laid = rotatedTo(orientation);
        if (laid.rows() > boardSize || laid.cols() > boardSize) return null;
        int row = Math.min(anchor.getRow(), boardSize - laid.rows());
        int col = Math.min(anchor.getCol(), boardSize - laid.cols());
        return new Coordinate(row, col);
    }
}
