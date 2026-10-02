package com.battleship.view.battle;

import com.battleship.controller.GameController;
import com.battleship.model.CellStatus;
import com.battleship.model.Coordinate;
import com.battleship.model.GameMode;
import com.battleship.model.HumanPlayer;
import com.battleship.model.Player;
import com.battleship.model.Theater;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class BattlePerspectiveTest {

    @Test
    void fixedPerspectiveAlwaysPinsPlayer1() {
        Player player1 = new HumanPlayer("Admiral 1", Theater.SKIRMISH);
        Player player2 = new HumanPlayer("Enemy AI", Theater.SKIRMISH);

        BattlePerspective perspective = new FixedPerspective(player1, player2);

        assertEquals("Admiral 1", perspective.perspectiveName());
        assertSame(player1, perspective.perspectiveFleet());
        assertEquals("Enemy AI", perspective.opponentName());
        assertSame(player1.trackingGrid(), perspective.opponentKnowledge());
    }

    @Test
    void alternatingPerspectiveSwapsWithTurnChanges() {
        Player player1 = new HumanPlayer("Admiral 1", Theater.SKIRMISH);
        Player player2 = new HumanPlayer("Admiral 2", Theater.SKIRMISH);

        AtomicReference<Player> current = new AtomicReference<>(player1);
        AtomicReference<Player> opponent = new AtomicReference<>(player2);

        BattlePerspective perspective = new AlternatingPerspective(current::get, opponent::get);

        // initially player 1
        assertEquals("Admiral 1", perspective.perspectiveName());
        assertSame(player1, perspective.perspectiveFleet());
        assertEquals("Admiral 2", perspective.opponentName());
        assertSame(player1.trackingGrid(), perspective.opponentKnowledge());

        // swap turn to player 2
        current.set(player2);
        opponent.set(player1);

        assertEquals("Admiral 2", perspective.perspectiveName());
        assertSame(player2, perspective.perspectiveFleet());
        assertEquals("Admiral 1", perspective.opponentName());
        assertSame(player2.trackingGrid(), perspective.opponentKnowledge());
    }

    @Test
    void fixedPerspectiveWithGameControllerInVsAi() {
        GameController controller = new GameController();
        controller.setMode(GameMode.AI_EASY);
        controller.setTheater(Theater.SKIRMISH);

        BattlePerspective perspective = new FixedPerspective(controller);

        assertEquals(controller.getPlayerName(1), perspective.perspectiveName());
        assertEquals(controller.getPlayerFleet(1), perspective.perspectiveFleet());
        assertEquals(controller.getPlayerName(2), perspective.opponentName());
        assertEquals(controller.getTrackingGrid(1), perspective.opponentKnowledge());
    }

    @Test
    void alternatingPerspectiveWithGameControllerInHotseat() {
        GameController controller = new GameController();
        controller.setMode(GameMode.HOTSEAT);
        controller.setTheater(Theater.SKIRMISH);

        BattlePerspective perspective = new AlternatingPerspective(controller);

        assertEquals(controller.getCurrentPlayer().name(), perspective.perspectiveName());
        assertEquals(controller.getOpponent().name(), perspective.opponentName());
    }
}
