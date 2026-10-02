package com.battleship.model;

/**
 * immutable algebraic coordinate (e.g. "b5") on a dynamically sized board.
 * a record: compile-time immutability, plus generated equals/hashcode/tostring.
 * the algebraic {@link #tostring()} is kept for readable logs and messages.
 */
public record Coordinate(int row, int col) {

    /** parses algebraic notation like "a1" or "j10" into a coordinate (0-indexed internally). */
    public static Coordinate fromAlgebraic(String notation) {
        char letter = Character.toUpperCase(notation.charAt(0));
        int col = letter - 'A';
        int row = Integer.parseInt(notation.substring(1)) - 1;
        return new Coordinate(row, col);
    }

    /** backward-compatible accessors (records expose row()/col(); these aliases keep call sites stable). */
    public int getRow() { return row; }
    public int getCol() { return col; }

    public boolean isWithinBounds(int boardSize) {
        return row >= 0 && row < boardSize && col >= 0 && col < boardSize;
    }

    @Override
    public String toString() {
        return String.valueOf((char) ('A' + col)) + (row + 1);
    }
}
