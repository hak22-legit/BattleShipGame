package com.battleship.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SaveGameServiceTest {

    @Test
    void saveAndLoadRoundTrip(@TempDir Path tempDir) throws IOException {
        SaveGameService service = new SaveGameService();

        GameSaveDTO.PlayerDTO p1 = new GameSaveDTO.PlayerDTO(
                "Admiral 1", true, new String[10][10], List.of());
        GameSaveDTO.PlayerDTO p2 = new GameSaveDTO.PlayerDTO(
                "Admiral 2", false, new String[10][10], List.of());

        GameSaveDTO original = GameSaveDTO.builder()
                .version("1.0.0")
                .timestamp("2026-09-19T12-00-00Z")
                .boardSize(10)
                .gameState("BATTLE")
                .player1(p1)
                .player2(p2)
                .currentPlayerIndex(0)
                .build();

        Path savedFile = service.save(original, tempDir);
        assertTrue(savedFile.toFile().exists());

        GameSaveDTO loaded = service.load(savedFile);
        assertNotNull(loaded);
        assertEquals(original.getVersion(), loaded.getVersion());
        assertEquals(original.getBoardSize(), loaded.getBoardSize());
        assertEquals(original.getGameState(), loaded.getGameState());
        assertEquals(original.getPlayer1().name(), loaded.getPlayer1().name());
        assertEquals(original.getPlayer2().name(), loaded.getPlayer2().name());
    }
}

