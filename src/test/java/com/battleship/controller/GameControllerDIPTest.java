package com.battleship.controller;

import com.battleship.model.Coordinate;
import com.battleship.model.GameMode;
import com.battleship.model.Orientation;
import com.battleship.model.Player;
import com.battleship.model.ShipType;
import com.battleship.model.Theater;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameControllerDIPTest {

    @Test
    void supportsConstructorInjection() {
        PlacementService customPlacement = new PlacementService();
        BattleService customBattle = new BattleService();

        GameController controller = new GameController(customPlacement, customBattle);
        assertNotNull(controller);
        assertEquals(com.battleship.model.GameState.MAIN_MENU, controller.getState());
    }

    @Test
    void removeShipAtEncapsulation() {
        GameController controller = new GameController();
        controller.setMode(GameMode.AI_EASY);
        controller.setTheater(Theater.SKIRMISH);

        Player player = controller.getPlayer1();
        Coordinate start = new Coordinate(0, 0);

        boolean placed = controller.placeShip(player, ShipType.PATROL_BOAT, start, Orientation.HORIZONTAL);
        assertTrue(placed);

        boolean removed = controller.removeShipAt(player, start);
        assertTrue(removed);

        // verify removing at empty coordinate returns false cleanly without error
        boolean removedAgain = controller.removeShipAt(player, start);
        assertFalse(removedAgain);
    }

    @Test
    void hotseatPlayerNamingAndInitialization() {
        GameController controller = new GameController();
        controller.setMode(GameMode.HOTSEAT);
        controller.setTheater(Theater.SKIRMISH);

        assertEquals("Admiral 1", controller.getPlayerName(1));
        assertEquals("Admiral 2", controller.getPlayerName(2));
        assertTrue(controller.getPlayer1() instanceof com.battleship.model.HumanPlayer);
        assertTrue(controller.getPlayer2() instanceof com.battleship.model.HumanPlayer);
    }

    @Test
    void onlineModePlayerInitialization() {
        GameController controller = new GameController();
        controller.setMode(GameMode.ONLINE);
        assertDoesNotThrow(() -> controller.setTheater(Theater.SKIRMISH));
        assertTrue(controller.getPlayer1() instanceof com.battleship.model.HumanPlayer);
        assertTrue(controller.getPlayer2() instanceof com.battleship.model.HumanPlayer);
        assertFalse(controller.isAiTurn());
    }

    @Test
    void setTheaterWithoutPriorModeSelectDoesNotThrowNPE() {
        GameController controller = new GameController();
        assertNull(controller.getSelectedMode());
        assertDoesNotThrow(() -> controller.setTheater(Theater.FLEET_ACTION));
        assertTrue(controller.getPlayer1() instanceof com.battleship.model.HumanPlayer);
        assertTrue(controller.getPlayer2() instanceof com.battleship.model.HumanPlayer);
    }

    @Test
    void hotseatPlacementLifecycleFlow() {
        GameController controller = new GameController();
        controller.setMode(GameMode.HOTSEAT);
        controller.setTheater(Theater.SKIRMISH);

        assertEquals(com.battleship.model.GameState.SHIP_PLACEMENT, controller.getState());
        assertEquals("Admiral 1", controller.getPlacingPlayer().getName());

        controller.autoPlaceRemaining(controller.getPlayer1());
        assertTrue(controller.isPlacementComplete(controller.getPlayer1()));

        controller.confirmReady();
        assertEquals(com.battleship.model.GameState.PASS_SCREEN, controller.getState());
        assertEquals("Admiral 2", controller.getPlacingPlayer().getName());

        controller.resumePlacementAfterPass();
        assertEquals(com.battleship.model.GameState.SHIP_PLACEMENT, controller.getState());

        controller.autoPlaceRemaining(controller.getPlayer2());
        assertTrue(controller.isPlacementComplete(controller.getPlayer2()));

        controller.confirmReady();
        assertEquals(com.battleship.model.GameState.BATTLE, controller.getState());
        assertNotNull(controller.getCurrentPlayer());
    }
}

