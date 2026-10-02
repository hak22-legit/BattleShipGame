package com.battleship.controller;

import com.battleship.model.*;
import com.battleship.model.weapon.WeaponCatalog;
import com.battleship.net.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MatchRulesTest {
    @Test void exactlyThreeNuclearShotsWithNoResupplyInEveryTheater() {
        for (Theater theater : Theater.values()) {
            Player player = new HumanPlayer("Player", theater);
            var nuclear = WeaponCatalog.nuclear();
            assertEquals(3, player.ammoCount(nuclear));
            for (int i = 0; i < 3; i++) {
                assertTrue(player.selectWeapon(nuclear));
                new NetworkFireService().fireNetworkShot(player, nuclear, new Coordinate(0, 0), Orientation.HORIZONTAL);
                new NetworkFireService().resupplyNuclear(player);
                assertEquals(2 - i, player.ammoCount(nuclear));
            }
            assertFalse(player.selectWeapon(nuclear));
            assertThrows(IllegalStateException.class, () -> player.consumeAmmo(nuclear));
        }
    }
    @Test void skippedTurnDoesNotSpendAmmoOrDamageShips() {
        Player first = new HumanPlayer("First", Theater.SKIRMISH);
        Player second = new HumanPlayer("Second", Theater.SKIRMISH);
        second.deploy(ShipType.PATROL_BOAT, new Coordinate(0, 0), Orientation.HORIZONTAL);
        BattleService battle = new BattleService();
        battle.init(first, second);
        first.selectWeapon(WeaponCatalog.nuclear());
        battle.skipTurn();
        assertSame(second, battle.getCurrentPlayer());
        assertEquals(3, first.ammoCount(WeaponCatalog.nuclear()));
        assertEquals(0, second.fleet().get(0).hitCount());
        assertSame(WeaponCatalog.standard(), first.selectedWeapon());
    }
    @Test void duplicateAndStaleNetworkTimeoutsAreIgnored() {
        NetworkGameSession session = new NetworkGameSession(null, Theater.SKIRMISH, Role.CLIENT,
                new HumanPlayer("Guest", Theater.SKIRMISH));
        session.beginMatch(true);
        assertFalse(session.acceptRemoteTimeout(10));
        assertTrue(session.acceptRemoteTimeout(0));
        assertTrue(session.isMyTurn());
        assertFalse(session.acceptRemoteTimeout(0));
        session.advanceTurn(); session.beginOpponentTurn();
        assertFalse(session.acceptRemoteTimeout(0));
        assertTrue(session.acceptRemoteTimeout(2));
        NetMessageCodec codec = new NetMessageCodec();
        assertEquals(new NetMessage.TurnExpired(2), codec.decode(codec.encode(new NetMessage.TurnExpired(2))));
    }
}
