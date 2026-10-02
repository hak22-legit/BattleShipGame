package com.battleship.controller;

import com.battleship.model.CellStatus;
import com.battleship.model.Coordinate;
import com.battleship.model.HumanPlayer;
import com.battleship.model.Orientation;
import com.battleship.model.Player;
import com.battleship.model.ShipType;
import com.battleship.model.Theater;
import com.battleship.model.weapon.WeaponCatalog;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** exercises the refactored turn/ammo flow: turn enum, player ammo delegates, shotresolver. */
class BattleServiceTest {

    /** builds a local battle; forces the given player to be on turn (via a harmless miss if not). */
    private BattleService initFor(Player first, Player p1, Player p2) {
        BattleService service = new BattleService();
        service.init(p1, p2);
        service.rollInitiative();
        if (service.getCurrentPlayer() != first) {
            p1.aimDefault();
            service.fire(new Coordinate(9, 9)); // harmless miss; hands the turn over
            assertFalse(service.isBattleOver());
        }
        return service;
    }

    private Player freshAdmiral(String name, int size) {
        Theater theater = size == 10 ? Theater.FLEET_ACTION : (size == 8 ? Theater.ENGAGEMENT : Theater.SKIRMISH);
        return new HumanPlayer(name, theater);
    }

    @Test
    void defaultShotMissesConsumesInfiniteAmmoAndAdvancesTurn() {
        Player p1 = freshAdmiral("P1", 10);
        Player p2 = freshAdmiral("P2", 10);

        BattleService service = initFor(p1, p1, p2);
        p1.aimDefault();
        Player starter = service.getCurrentPlayer();

        LauncherFireResult result = service.fire(new Coordinate(4, 4));

        assertEquals(1, result.results().size());
        assertEquals(CellStatus.MISS, result.results().get(0).outcome());
        // infinite ammo must be untouched (v1: consume via player delegate).
        assertEquals(Integer.MAX_VALUE, starter.ammoCount(WeaponCatalog.standardShell()));
        // launcher resets after each shot (rule 1).
        assertTrue(starter.selectedWeapon() == WeaponCatalog.standardShell());
        // turn advanced.
        assertNotEquals(starter, service.getCurrentPlayer());
        assertFalse(service.isBattleOver());
    }

    @Test
    void nuclearShotSinksFleetEndsBattleAndKeepsTurnWithWinner() {
        Player p1 = freshAdmiral("P1", 10);
        Player p2 = freshAdmiral("P2", 10);
        assertTrue(p2.deploy(ShipType.PATROL_BOAT, new Coordinate(0, 0), Orientation.HORIZONTAL));

        BattleService service = initFor(p1, p1, p2);
        assertTrue(p1.aimNuclear());

        LauncherFireResult result = service.fire(new Coordinate(0, 0));

        assertTrue(result.results().stream().anyMatch(r -> r.outcome() == CellStatus.SUNK));
        assertEquals(1, result.sunkShips().size());
        assertTrue(p2.isFleetDestroyed());
        assertTrue(service.isBattleOver());
        // ammo was consumed through the delegate; launcher reset.
        assertEquals(2, p1.ammoCount(WeaponCatalog.nuclearWarhead()));
        assertTrue(p1.selectedWeapon() == WeaponCatalog.standardShell());
        // turn stays with the winner so game-over reporting names the right player.
        assertEquals(p1, service.getCurrentPlayer());
    }

    @Test
    void playerAmmoDelegatesNeverExposeTheMutableInventory() {
        Player p = freshAdmiral("P", 8);
        // encapsulation (v1): only read/delegate access; consume & resupply go through the player.
        assertEquals(3, p.ammoCount(WeaponCatalog.nuclearWarhead()));
        p.consumeAmmo(WeaponCatalog.nuclearWarhead());
        p.consumeAmmo(WeaponCatalog.nuclearWarhead());
        p.consumeAmmo(WeaponCatalog.nuclearWarhead());
        assertFalse(p.hasAmmo(WeaponCatalog.nuclearWarhead()));
        p.resupplyAmmo(WeaponCatalog.nuclearWarhead(), 1);
        assertFalse(p.hasAmmo(WeaponCatalog.nuclearWarhead()), "Nuclear rounds cannot be refilled during a match");
        assertTrue(p.isAmmoInfinite(WeaponCatalog.standardShell()));
    }

    @Test
    void aiPlanIsReadOnlyOverPlayerAmmo() {
        Player p = new com.battleship.model.AiPlayer("AI", Theater.FLEET_ACTION, new com.battleship.ai.HuntTargetAI());
        com.battleship.ai.AIStrategy ai = new com.battleship.ai.HuntTargetAI();

        com.battleship.model.ShotOrder plan = ai.chooseShotPlan(p.trackingGrid(), p);

        // planning must never mutate ammo — the ai only reads through delegates (v1).
        assertEquals(3, p.ammoCount(WeaponCatalog.salvoBarrage()));
        assertEquals(3, p.ammoCount(WeaponCatalog.nuclearWarhead()));
        assertTrue(plan.weapon() == WeaponCatalog.standardShell()
                || plan.weapon() == WeaponCatalog.salvoBarrage());
        assertTrue(plan.anchor().isWithinBounds(10));
    }
}
