package com.battleship.controller;

import com.battleship.model.CellStatus;
import com.battleship.model.Coordinate;
import com.battleship.model.Orientation;
import com.battleship.model.Player;
import com.battleship.model.ShotOrder;
import com.battleship.model.ShotResult;
import com.battleship.model.Turn;
import com.battleship.model.fog.TrackingGrid;
import com.battleship.model.projection.ShipSnapshot;

import com.battleship.model.weapon.Weapon;

import java.security.SecureRandom;

/**
 * encapsulates turn management, weapon selection and the firing pipeline.
 * extracted from gamecontroller so the controller can stay a thin mediator (srp).
 *
 * <p>two architectural fixes:</p>
 * <ul>
 *   <li>the ai branch {@code if (aistrategy != null && attacker == player2)} is gone:
 *       whoever holds the turn is asked for a {@link shotorder} through the
 *       polymorphic {@link player#decideautonomousshot()} (v2.2).</li>
 *   <li>every shot updates the <em>shooter's</em> {@link trackinggrid} — the only
 *       fog-of-war model in the game — instead of the shooter reading the
 *       defender's grid (v1.3 / smell 5.2).</li>
 * </ul>
 */
public class BattleService {

    private static final SecureRandom RANDOM = new SecureRandom();

    /** shot-resolution strategy, injectable for tests (fixes f5). */
    private final ShotResolution shotResolution;

    private Player player1;
    private Player player2;
    private Turn currentTurn;

    /** true only between a fire() that ended the match and the controller reacting to it. */
    private boolean battleOver;

    /** production constructor — uses the standard shot resolver. */
    public BattleService() {
        this(ShotResolver.STANDARD);
    }

    /** testable constructor — inject the shot-resolution strategy (dip, fixes f5). */
    public BattleService(ShotResolution shotResolution) {
        this.shotResolution = shotResolution;
    }

    public void init(Player player1, Player player2) {
        this.player1 = player1;
        this.player2 = player2;
        this.currentTurn = Turn.PLAYER_1;
        this.battleOver = false;
    }

    /** cryptographically fair coin flip determines who fires first. */
    public Player rollInitiative() {
        currentTurn = RANDOM.nextBoolean() ? Turn.PLAYER_1 : Turn.PLAYER_2;
        return getCurrentPlayer();
    }

    public Player getCurrentPlayer() { return currentTurn == Turn.PLAYER_1 ? player1 : player2; }
    public Player getOpponent() { return currentTurn == Turn.PLAYER_1 ? player2 : player1; }

    /** true when the player holding the turn acts on its own (no ui click expected). */
    public boolean isAiTurn() {
        return getCurrentPlayer().isAutonomous();
    }

    public boolean selectWeapon(Player player, Weapon weapon) {
        return player.selectWeapon(weapon);
    }

    public void toggleOrientation(Player player) {
        player.toggleWeaponOrientation();
    }

    public int getAmmoRemaining(Player player, Weapon weapon) {
        return player.ammoCount(weapon);
    }

    /**
     * fires the current player's selected weapon, anchored at the given cell.
     * the pattern resolution is delegated to the shared {@link shotresolver};
     * this method owns only the turn-level concerns: knowledge bookkeeping,
     * ammo consumption, weapon reset, shooter feedback and turn advancement
     * (unless the defender just lost).
     */
    public LauncherFireResult fire(Coordinate anchor) {
        battleOver = false;
        Player attacker = getCurrentPlayer();
        Player defender = getOpponent();
        Weapon weapon = attacker.selectedWeapon();
        if (!attacker.hasAmmo(weapon)) throw new IllegalStateException("No ammunition for this shot");

        LauncherFireResult result = shotResolution.resolve(
                defender, weapon, anchor, attacker.weaponOrientation());

        recordObservedOutcome(attacker, result);
        attacker.consumeAmmo(weapon);          // infinite weapons: no-op
        attacker.resetWeaponAfterShot();       // must actively re-select each turn (rule 1)
        for (ShotResult shot : result.results()) {
            attacker.observeOwnShot(shot);     // polymorphic: only a machine learns
        }

        if (defender.isFleetDestroyed()) {
            battleOver = true; // turn stays with the winner for game-over reporting
        } else {
            currentTurn = currentTurn.next();
        }
        return result;
    }

    /** has the current player choose a weapon + target on its own, then fires it. */
    public LauncherFireResult fireAiLauncher() {
        Player attacker = getCurrentPlayer();
        ShotOrder order = attacker.decideAutonomousShot().orElseThrow(() ->
                new IllegalStateException(attacker.name() + " needs a human to choose a shot."));
        attacker.armWeapon(order.weapon(), order.orientation());
        return fire(order.anchor());
    }

    /** copies everything the shooter just observed into their own knowledge grid. */
    private void recordObservedOutcome(Player attacker, LauncherFireResult result) {
        TrackingGrid knowledge = attacker.trackingGrid();
        for (ShotResult shot : result.results()) {
            knowledge.recordShotOutcome(shot.coordinate(), shot.outcome());
        }
        for (ShipSnapshot sunk : result.sunkShips()) {
            knowledge.recordWreck(sunk.type(), sunk.cells(), sunk.orientation());
        }
    }

    public boolean isBattleOver() { return battleOver; }

    /** A timeout spends no ammunition and makes no changes to either board. */
    public void skipTurn() {
        if (battleOver) return;
        getCurrentPlayer().resetWeaponAfterShot();
        currentTurn = currentTurn.next();
    }
}
