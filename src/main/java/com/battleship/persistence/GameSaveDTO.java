package com.battleship.persistence;

import java.util.List;

/**
 * flat, gson-friendly representation of a full game save, matching the
 * json schema in the spec (version, timestamp, boardsize, players, turnhistory...).
 *
 * encapsulated: all fields are private with read-only accessors, constructed
 * only through the {@link builder}, which validates the invariants
 * (version tag present, positive board size, both players present).
 * nested payload types are immutable records. gson populates the private
 * fields reflectively, so the wire format is unchanged.
 */
public class GameSaveDTO {

    private final String version;
    private final String timestamp;
    private final int boardSize;
    private final String gameState;
    private final PlayerDTO player1;
    private final PlayerDTO player2;
    private final List<TurnRecordDTO> turnHistory;
    private final int currentPlayerIndex;

    private GameSaveDTO(Builder b) {
        this.version = b.version;
        this.timestamp = b.timestamp;
        this.boardSize = b.boardSize;
        this.gameState = b.gameState;
        this.player1 = b.player1;
        this.player2 = b.player2;
        this.turnHistory = b.turnHistory;
        this.currentPlayerIndex = b.currentPlayerIndex;
    }

    public String getVersion() { return version; }
    public String getTimestamp() { return timestamp; }
    public int getBoardSize() { return boardSize; }
    public String getGameState() { return gameState; }
    public PlayerDTO getPlayer1() { return player1; }
    public PlayerDTO getPlayer2() { return player2; }
    public List<TurnRecordDTO> getTurnHistory() { return turnHistory; }
    public int getCurrentPlayerIndex() { return currentPlayerIndex; }

    public static Builder builder() { return new Builder(); }

    /** validates invariants at construction time. */
    public static final class Builder {
        private String version;
        private String timestamp;
        private int boardSize = -1;
        private String gameState;
        private PlayerDTO player1;
        private PlayerDTO player2;
        private List<TurnRecordDTO> turnHistory = List.of();
        private int currentPlayerIndex;

        public Builder version(String v) { this.version = v; return this; }
        public Builder timestamp(String t) { this.timestamp = t; return this; }
        public Builder boardSize(int s) { this.boardSize = s; return this; }
        public Builder gameState(String s) { this.gameState = s; return this; }
        public Builder player1(PlayerDTO p) { this.player1 = p; return this; }
        public Builder player2(PlayerDTO p) { this.player2 = p; return this; }
        public Builder turnHistory(List<TurnRecordDTO> h) { this.turnHistory = h; return this; }
        public Builder currentPlayerIndex(int i) { this.currentPlayerIndex = i; return this; }

        public GameSaveDTO build() {
            if (version == null || version.isBlank()) {
                throw new IllegalStateException("A save requires a version tag.");
            }
            if (boardSize <= 0) {
                throw new IllegalStateException("A save requires a positive board size.");
            }
            if (player1 == null || player2 == null) {
                throw new IllegalStateException("A save requires both players.");
            }
            return new GameSaveDTO(this);
        }
    }

    public record PlayerDTO(String name, boolean isHuman, String[][] ownBoard, List<ShipDTO> ships) { }

    public record ShipDTO(String type, int hits, List<String> coordinates) { } // coordinates: algebraic notation

    public record TurnRecordDTO(String shooter, String target, String coordinate, String result, String shipSunk) { }
}
