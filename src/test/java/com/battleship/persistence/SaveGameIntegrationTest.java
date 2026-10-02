package com.battleship.persistence;

import com.battleship.controller.GameController;
import com.battleship.model.GameMode;
import com.battleship.model.Theater;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SaveGameIntegrationTest {

    @Test
    void gameControllerSaveAndLoadIntegration(@TempDir Path tempDir) throws IOException {
        GameController controller = new GameController();
        controller.setMode(GameMode.AI_NORMAL);
        controller.setTheater(Theater.FLEET_ACTION);

        // deploy fleet for player 1 and advance to battle
        controller.autoPlaceRemaining(controller.getPlacingPlayer());
        controller.confirmReady();

        Path saveFile = controller.saveGame(tempDir);

        assertNotNull(saveFile);
        assertTrue(Files.exists(saveFile));
        assertTrue(Files.size(saveFile) > 0);

        GameSaveDTO loaded = controller.loadGame(saveFile);
        assertNotNull(loaded);
        assertEquals("1.0.0", loaded.getVersion());
        assertEquals(Theater.FLEET_ACTION.getBoardSize(), loaded.getBoardSize());
        assertEquals("BATTLE", loaded.getGameState());

        // player 1 assertions
        assertNotNull(loaded.getPlayer1());
        assertTrue(loaded.getPlayer1().isHuman());
        assertFalse(loaded.getPlayer1().ships().isEmpty());

        // player 2 assertions (ai)
        assertNotNull(loaded.getPlayer2());
        assertFalse(loaded.getPlayer2().isHuman());
        assertFalse(loaded.getPlayer2().ships().isEmpty());
    }
}
