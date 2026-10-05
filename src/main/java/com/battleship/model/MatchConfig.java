package com.battleship.model;

/**
 * immutable configuration for a naval command match.
 * pure domain record with strictly zero JavaFX imports.
 */
public record MatchConfig(int boardSize, boolean quizEnabled) {

    public MatchConfig {
        if (boardSize <= 0) {
            throw new IllegalArgumentException("A match requires a positive board size, got: " + boardSize);
        }
    }

    public static MatchConfig standard(int boardSize) {
        return new MatchConfig(boardSize, true);
    }

    public static MatchConfig of(int boardSize, boolean quizEnabled) {
        return new MatchConfig(boardSize, quizEnabled);
    }
}
