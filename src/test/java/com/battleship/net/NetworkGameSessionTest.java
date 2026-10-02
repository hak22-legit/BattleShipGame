package com.battleship.net;

import com.battleship.model.HumanPlayer;
import com.battleship.model.Player;
import com.battleship.model.Theater;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** the role enum replaces the raw boolean ishost flag (p3). */
class NetworkGameSessionTest {

    private NetworkGameSession sessionFor(Role role) {
        Player me = new HumanPlayer("Me", Theater.SKIRMISH);
        return new NetworkGameSession(null, Theater.SKIRMISH, role, me);
    }

    @Test
    void hostRoleBehaviour() {
        NetworkGameSession host = sessionFor(Role.HOST);
        assertTrue(host.isHost());
        assertTrue(host.getRole() == Role.HOST);
        host.beginMatch(true);
        assertTrue(host.isMyTurn());
        host.beginMatch(false);
        assertFalse(host.isMyTurn());
    }

    @Test
    void clientRoleBehaviour() {
        NetworkGameSession client = sessionFor(Role.CLIENT);
        assertFalse(client.isHost());
        client.beginMatch(true);
        assertFalse(client.isMyTurn());
        client.beginMatch(false);
        assertTrue(client.isMyTurn());
    }

    @Test
    void storesAndRetrievesRevealedEnemyFleet() {
        NetworkGameSession session = sessionFor(Role.HOST);
        org.junit.jupiter.api.Assertions.assertNull(session.getRevealedEnemyFleet());

        java.util.List<com.battleship.model.projection.ShipSnapshot> ships = java.util.List.of(
                new com.battleship.model.projection.ShipSnapshot(
                        com.battleship.model.ShipType.PATROL_BOAT,
                        java.util.List.of(new com.battleship.model.Coordinate(0, 0), new com.battleship.model.Coordinate(0, 1)),
                        com.battleship.model.Orientation.HORIZONTAL,
                        0,
                        false
                )
        );
        session.setRevealedEnemyFleet(ships);
        org.junit.jupiter.api.Assertions.assertEquals(ships, session.getRevealedEnemyFleet());
    }
}
