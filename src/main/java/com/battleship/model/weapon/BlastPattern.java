package com.battleship.model.weapon;

import com.battleship.model.Coordinate;
import com.battleship.model.Orientation;

import java.util.ArrayList;
import java.util.List;

/**
 * a first-class blast geometry: the cell footprint a weapon covers from an anchor.
 *
 * <p>replaces {@code launchertype.patterndimensions()} (v3.1), which leaked raw
 * {@code int[][]} arrays and forced callers to guess what the two numbers meant.
 * a pattern is authored in its horizontal layout and transposed for vertical
 * fire, so every weapon describes its shape exactly once.</p>
 *
 * @param rows cells covered vertically in the horizontal layout
 * @param cols cells covered horizontally in the horizontal layout
 */
public record BlastPattern(int rows, int cols) {

    public BlastPattern {
        if (rows <= 0 || cols <= 0) {
            throw new IllegalArgumentException("A blast pattern needs positive dimensions, got "
                    + rows + "x" + cols);
        }
    }

    public static BlastPattern of(int rows, int cols) {
        return new BlastPattern(rows, cols);
    }

    /** the same footprint rotated into the requested firing orientation. */
    public BlastPattern rotatedTo(Orientation orientation) {
        return orientation.isHorizontal() ? this : new BlastPattern(cols, rows);
    }

    /** total cells covered from the anchor. */
    public int cellCount() {
        return rows * cols;
    }

    /** the orientation this pattern was authored in (used by ai block scoring). */
    public Orientation naturalOrientation() {
        return rows <= cols ? Orientation.HORIZONTAL : Orientation.VERTICAL;
    }

    /**
     * coordinates covered when this pattern is already oriented.
     */
    public List<Coordinate> coverage(Coordinate anchor) {
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
