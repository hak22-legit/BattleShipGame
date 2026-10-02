package com.battleship.net;

import com.battleship.model.Coordinate;
import com.battleship.model.HumanPlayer;
import com.battleship.model.Orientation;
import com.battleship.model.Player;
import com.battleship.model.PrimaryGrid;
import com.battleship.model.ShipType;
import com.battleship.model.Theater;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NetworkBattleMediatorTest {

    @Test
    void resolvesIncomingShotAndReportsOutcome() {
        Player me = new HumanPlayer("Local Admiral", Theater.SKIRMISH);
        me.deploy(ShipType.PATROL_BOAT, new Coordinate(1, 1), Orientation.HORIZONTAL);

        NetworkGameSession session = new NetworkGameSession(null, Theater.SKIRMISH, Role.CLIENT, me);
        NetworkBattleMediator mediator = new NetworkBattleMediator(session);

        // test miss
        NetMessage.Fire missFire = new NetMessage.Fire(
                com.battleship.model.weapon.WeaponCatalog.standardShell().id(),
                new Coordinate(0, 0), Orientation.HORIZONTAL);
        NetworkBattleMediator.IncomingFireOutcome missOutcome = mediator.resolveAndReply(missFire);
        assertFalse(missOutcome.anyHit());
        assertFalse(missOutcome.anySunk());
        assertFalse(missOutcome.lost());

        // test hit 1
        NetMessage.Fire hitFire1 = new NetMessage.Fire(
                com.battleship.model.weapon.WeaponCatalog.standardShell().id(),
                new Coordinate(1, 1), Orientation.HORIZONTAL);
        NetworkBattleMediator.IncomingFireOutcome hitOutcome1 = mediator.resolveAndReply(hitFire1);
        assertTrue(hitOutcome1.anyHit());
        assertFalse(hitOutcome1.anySunk());
        assertFalse(hitOutcome1.lost());

        // test hit 2 and sunk (patrol boat is size 2: (1,1) and (1,2))
        NetMessage.Fire hitFire2 = new NetMessage.Fire(
                com.battleship.model.weapon.WeaponCatalog.standardShell().id(),
                new Coordinate(1, 2), Orientation.HORIZONTAL);
        NetworkBattleMediator.IncomingFireOutcome hitOutcome2 = mediator.resolveAndReply(hitFire2);
        assertTrue(hitOutcome2.anyHit());
        assertTrue(hitOutcome2.anySunk());
        assertTrue(hitOutcome2.lost()); // only ship on board was sunk
    }
}

