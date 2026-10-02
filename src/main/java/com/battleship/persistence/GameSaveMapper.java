package com.battleship.persistence;

import com.battleship.model.Coordinate;
import com.battleship.model.GameState;
import com.battleship.model.Player;
import com.battleship.model.projection.ShipSnapshot;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * translates domain player and game state models into persistent dto hierarchies (srp).
 */
public final class GameSaveMapper {

    private GameSaveMapper() { }

    public static GameSaveDTO toDTO(Player player1, Player player2, int boardSize,
                                    GameState state, int currentPlayerIndex) {
        GameSaveDTO.PlayerDTO p1Dto = toPlayerDTO(player1, boardSize);
        GameSaveDTO.PlayerDTO p2Dto = toPlayerDTO(player2, boardSize);

        return GameSaveDTO.builder()
                .version("1.0.0")
                .timestamp(Instant.now().toString())
                .boardSize(boardSize)
                .gameState(state != null ? state.name() : "BATTLE")
                .player1(p1Dto)
                .player2(p2Dto)
                .currentPlayerIndex(currentPlayerIndex)
                .turnHistory(List.of())
                .build();
    }

    public static GameSaveDTO.PlayerDTO toPlayerDTO(Player player, int size) {
        String[][] board = new String[size][size];
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                board[r][c] = player.cellStatus(new Coordinate(r, c)).name();
            }
        }
        List<GameSaveDTO.ShipDTO> ships = new ArrayList<>();
        for (ShipSnapshot s : player.fleet()) {
            List<String> coords = s.cells().stream().map(Coordinate::toString).toList();
            ships.add(new GameSaveDTO.ShipDTO(s.type().name(), s.hitCount(), coords));
        }
        return new GameSaveDTO.PlayerDTO(player.name(), player.isHuman(), board, ships);
    }
}

