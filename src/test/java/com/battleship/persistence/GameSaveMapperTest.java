package com.battleship.persistence;

import com.battleship.model.Coordinate;
import com.battleship.model.GameState;
import com.battleship.model.HumanPlayer;
import com.battleship.model.Orientation;
import com.battleship.model.Player;
import com.battleship.model.ShipType;
import com.battleship.model.Theater;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameSaveMapperTest {

    @Test
    void mapsPlayerAndGameStateToDTO() {
        Player p1 = new HumanPlayer("Admiral One", Theater.SKIRMISH);
        Player p2 = new HumanPlayer("Admiral Two", Theater.SKIRMISH);
        p1.deploy(ShipType.PATROL_BOAT, new Coordinate(0, 0), Orientation.HORIZONTAL);

        GameSaveDTO dto = GameSaveMapper.toDTO(p1, p2, 5, GameState.BATTLE, 1);

        assertNotNull(dto);
        assertEquals("1.0.0", dto.getVersion());
        assertEquals(5, dto.getBoardSize());
        assertEquals("BATTLE", dto.getGameState());
        assertEquals(1, dto.getCurrentPlayerIndex());

        assertEquals("Admiral One", dto.getPlayer1().name());
        assertTrue(dto.getPlayer1().isHuman());
        assertEquals(1, dto.getPlayer1().ships().size());
        assertEquals("PATROL_BOAT", dto.getPlayer1().ships().get(0).type());

        assertEquals("Admiral Two", dto.getPlayer2().name());
        assertTrue(dto.getPlayer2().ships().isEmpty());
    }
}

