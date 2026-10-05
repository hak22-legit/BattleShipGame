package com.battleship.controller;

import com.battleship.model.*;
import com.battleship.model.weapon.WeaponCatalog;
import com.battleship.net.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MatchRulesTest {
    @Test void dynamicAmmunitionMatrixStrictlyBoundToBoardSize() {
        Player p5 = new HumanPlayer("P5", Theater.SKIRMISH);
        assertEquals(0, p5.ammoCount(WeaponCatalog.crossBomb()));
        assertEquals(0, p5.ammoCount(WeaponCatalog.nuclear()));
        assertTrue(p5.isAmmoInfinite(WeaponCatalog.standard()));

        Player p8 = new HumanPlayer("P8", Theater.ENGAGEMENT);
        assertEquals(2, p8.ammoCount(WeaponCatalog.crossBomb()));
        assertEquals(0, p8.ammoCount(WeaponCatalog.nuclear()));

        Player p10 = new HumanPlayer("P10", Theater.FLEET_ACTION);
        assertEquals(3, p10.ammoCount(WeaponCatalog.crossBomb()));
        assertEquals(1, p10.ammoCount(WeaponCatalog.nuclear()));

        var nuclear = WeaponCatalog.nuclear();
        assertTrue(p10.selectWeapon(nuclear));
        new NetworkFireService().fireNetworkShot(p10, nuclear, new Coordinate(0, 0), Orientation.HORIZONTAL);
        new NetworkFireService().resupplyNuclear(p10);
        assertEquals(0, p10.ammoCount(nuclear), "Nuclear rounds cannot be refilled during match");
        assertFalse(p10.selectWeapon(nuclear));
        assertThrows(IllegalStateException.class, () -> p10.consumeAmmo(nuclear));
    }
    @Test void skippedTurnDoesNotSpendAmmoOrDamageShips() {
        Player first = new HumanPlayer("First", Theater.FLEET_ACTION);
        Player second = new HumanPlayer("Second", Theater.FLEET_ACTION);
        second.deploy(ShipType.PATROL_BOAT, new Coordinate(0, 0), Orientation.HORIZONTAL);
        BattleService battle = new BattleService();
        battle.init(first, second);
        first.selectWeapon(WeaponCatalog.nuclear());
        battle.skipTurn();
        assertSame(second, battle.getCurrentPlayer());
        assertEquals(1, first.ammoCount(WeaponCatalog.nuclear()));
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
