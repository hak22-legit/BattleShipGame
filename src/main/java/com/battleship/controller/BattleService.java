package com.battleship.controller;

import com.battleship.model.Coordinate;
import com.battleship.model.Player;
import com.battleship.model.ShotOrder;
import com.battleship.model.ShotResult;
import com.battleship.model.Turn;
import com.battleship.model.fog.TrackingGrid;
import com.battleship.model.mode.ClassicModeStrategy;
import com.battleship.model.mode.GameModeStrategy;
import com.battleship.model.mode.VictoryResult;
import com.battleship.model.projection.ShipSnapshot;
import com.battleship.model.weapon.Weapon;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * encapsulates turn management, weapon selection and the firing pipeline.
 * integrates modular GameModeStrategy for dynamic game rules and win conditions.
 * strictly pure domain service with zero JavaFX imports.
 */
public class BattleService {

    private static final SecureRandom RANDOM = new SecureRandom();

    /** shot-resolution strategy, injectable for tests (fixes f5). */
    private final ShotResolution shotResolution;

    private Player player1;
    private Player player2;
    private Turn currentTurn;

    private GameModeStrategy gameModeStrategy = new ClassicModeStrategy();
    private int currentRound = 1;
    private Player winningPlayer;

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
        init(player1, player2, new ClassicModeStrategy());
    }

    public void init(Player player1, Player player2, GameModeStrategy strategy) {
        this.player1 = player1;
        this.player2 = player2;
        this.currentTurn = Turn.PLAYER_1;
        this.battleOver = false;
        this.winningPlayer = null;
        this.currentRound = 1;
        this.gameModeStrategy = strategy != null ? strategy : new ClassicModeStrategy();
        if (player1 != null) {
            this.gameModeStrategy.initializeMatch(player1, player2, player1.size());
        }
    }

    /** cryptographically fair coin flip determines who fires first. */
    public Player rollInitiative() {
        currentTurn = RANDOM.nextBoolean() ? Turn.PLAYER_1 : Turn.PLAYER_2;
        return getCurrentPlayer();
    }

    public Player getCurrentPlayer() {
        if (battleOver && winningPlayer != null) {
            return winningPlayer;
        }
        return currentTurn == Turn.PLAYER_1 ? player1 : player2;
    }

    public Player getOpponent() {
        return currentTurn == Turn.PLAYER_1 ? player2 : player1;
    }

    public GameModeStrategy getGameModeStrategy() {
        return gameModeStrategy;
    }

    public void setGameModeStrategy(GameModeStrategy strategy) {
        if (strategy != null) {
            this.gameModeStrategy = strategy;
            if (player1 != null) {
                this.gameModeStrategy.initializeMatch(player1, player2, player1.size());
            }
        }
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public Player getWinner() {
        return winningPlayer != null ? winningPlayer : getCurrentPlayer();
    }

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
     * coordinates with GameModeStrategy for secondary effects and win conditions.
     */
    public LauncherFireResult fire(Coordinate anchor) {
        battleOver = false;
        Player attacker = getCurrentPlayer();
        Player defender = getOpponent();
        Weapon weapon = attacker.selectedWeapon();
        if (!attacker.hasAmmo(weapon)) throw new IllegalStateException("No ammunition for this shot");

        // Blitz clock update if applicable
        if (gameModeStrategy instanceof com.battleship.model.mode.BlitzModeStrategy blitz) {
            blitz.onShotExecuted(attacker);
            VictoryResult vr = blitz.evaluateVictory(attacker, defender, currentRound);
            if (vr.isGameOver()) {
                battleOver = true;
                winningPlayer = vr.winner();
            }
        }

        LauncherFireResult result = shotResolution.resolve(
                defender, weapon, anchor, attacker.weaponOrientation());

        // Game mode post-shot hooks (e.g. SeaMine Cross detonation)
        List<ShotResult> secondaries = gameModeStrategy.onShotResolved(attacker, defender, anchor, result.results());
        if (!secondaries.isEmpty()) {
            List<ShotResult> combined = new ArrayList<>(result.results());
            combined.addAll(secondaries);
            result = new LauncherFireResult(combined, result.sunkShips());
        }

        recordObservedOutcome(attacker, result);
        attacker.consumeAmmo(weapon);          // infinite weapons: no-op
        attacker.resetWeaponAfterShot();       // must actively re-select each turn (rule 1)
        for (ShotResult shot : result.results()) {
            attacker.observeOwnShot(shot);     // polymorphic: only a machine learns
        }

        // Evaluate victory through the modular strategy
        VictoryResult victoryResult = gameModeStrategy.evaluateVictory(attacker, defender, currentRound);
        if (victoryResult.isGameOver()) {
            battleOver = true;
            winningPlayer = victoryResult.winner();
        } else if (defender.isFleetDestroyed()) {
            battleOver = true;
            winningPlayer = attacker;
        } else {
            if (currentTurn == Turn.PLAYER_2) {
                currentRound++;
            }
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
        if (currentTurn == Turn.PLAYER_2) {
            currentRound++;
        }
        currentTurn = currentTurn.next();
    }
}
