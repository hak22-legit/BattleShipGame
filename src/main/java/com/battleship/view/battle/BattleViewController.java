package com.battleship.view.battle;

import com.battleship.controller.GameController;
import com.battleship.controller.LauncherFireResult;
import com.battleship.model.CellStatus;
import com.battleship.model.Coordinate;
import com.battleship.model.Orientation;
import com.battleship.model.Player;
import com.battleship.model.PlayerArsenal;
import com.battleship.model.ShotResult;
import com.battleship.model.mode.GameModeStrategy;
import com.battleship.model.projection.ShipSnapshot;
import com.battleship.model.weapon.Weapon;
import com.battleship.model.weapon.WeaponCatalog;
import com.battleship.model.weapon.WeaponType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Controller synchronizing tactical weapons, player arsenals, game mode strategies,
 * target reticles, and attack log formatting for battle views.
 */
public class BattleViewController {

    public record AttackLogEntry(String message, String type) {}

    private final GameController controller;

    public BattleViewController(GameController controller) {
        this.controller = controller;
    }

    public GameController getController() {
        return controller;
    }

    public GameModeStrategy getGameModeStrategy() {
        return controller != null ? controller.getGameModeStrategy() : null;
    }

    public PlayerArsenal getArsenal(Player player) {
        return player != null ? player.arsenal() : null;
    }

    /**
     * Checks whether the current selected weapon is valid and has ammunition on the given board.
     * If empty or unavailable, automatically reverts selection to canonical SINGLE (Default).
     *
     * @param player    player whose arsenal to check
     * @param boardSize current board dimensions
     * @return currently armed weapon after validation/revert
     */
    public Weapon validateAndRevertWeapon(Player player, int boardSize) {
        if (player == null) return WeaponCatalog.defaultWeapon();
        Weapon current = player.selectedWeapon();
        if (current == null || !current.availableFor(boardSize)
                || (!current.hasInfiniteAmmo() && player.ammoCount(current) <= 0)) {
            player.selectWeapon(WeaponCatalog.defaultWeapon());
            return WeaponCatalog.defaultWeapon();
        }
        return current;
    }

    /**
     * Synchronizes the weapon console with the player's arsenal and current board rules.
     */
    public void synchronizeWeaponConsole(WeaponConsole console, Player player, int boardSize,
                                         boolean turnAllows, Consumer<Weapon> onSelect) {
        if (console == null || player == null) return;
        validateAndRevertWeapon(player, boardSize);
        console.refresh(player, boardSize, turnAllows, onSelect);
    }

    /**
     * Computes the blast reticle area for a hovered cell, clipped safely to grid bounds.
     * CrossBomb: 5-cell '+' pattern.
     * NuclearWarhead: 9-cell 3x3 square block.
     * StandardShell: 1-cell.
     */
    public List<Coordinate> calculateTargetReticle(Weapon weapon, Coordinate anchor,
                                                   Orientation orientation, int boardSize) {
        if (weapon == null || anchor == null) return List.of();
        List<Coordinate> rawPattern = weapon.calculateBlastArea(anchor, orientation != null ? orientation : Orientation.HORIZONTAL);
        return rawPattern.stream()
                .filter(c -> c.isWithinBounds(boardSize))
                .toList();
    }

    /**
     * Formats attack log entries for a shot resolution result.
     * For multi-cell blasts, produces individual cell outcomes followed by a summary line.
     */
    public List<AttackLogEntry> formatAttackLog(LauncherFireResult result) {
        List<AttackLogEntry> entries = new ArrayList<>();
        if (result == null || result.results().isEmpty()) return entries;

        List<ShotResult> shots = result.results();
        boolean isMultiCell = shots.size() > 1;

        if (isMultiCell) {
            int hits = (int) shots.stream()
                    .filter(r -> r.outcome() == CellStatus.HIT || r.outcome() == CellStatus.SUNK)
                    .count();
            int misses = (int) shots.stream()
                    .filter(r -> r.outcome() == CellStatus.MISS)
                    .count();

            // Report individual cell outcomes
            for (ShotResult r : shots) {
                String outcomeStr = (r.outcome() == CellStatus.HIT || r.outcome() == CellStatus.SUNK) ? "HIT" : "MISS";
                entries.add(new AttackLogEntry("  " + r.coordinate().toString() + ": " + outcomeStr, outcomeStr.toLowerCase()));
            }

            // Report blast summary line
            String weaponLabel = shots.size() >= 6 ? "Nuclear strike" : "Cross Bomb";
            String summary = weaponLabel + " detonated: " + hits + " hit" + (hits == 1 ? "" : "s") + ", " + misses + " miss" + (misses == 1 ? "" : "es") + ".";
            entries.add(new AttackLogEntry(summary, hits > 0 ? "hit" : "miss"));
        } else {
            ShotResult single = shots.get(0);
            boolean hit = single.outcome() == CellStatus.HIT || single.outcome() == CellStatus.SUNK;
            if (hit) {
                entries.add(new AttackLogEntry(single.coordinate().toString() + ": Direct hit!", "hit"));
            } else {
                entries.add(new AttackLogEntry(single.coordinate().toString() + ": Nothing but spray \u2014 miss.", "miss"));
            }
        }

        // Report sunk ships
        for (ShipSnapshot sunkShip : result.sunkShips()) {
            entries.add(new AttackLogEntry(sunkShip.type().name().replace('_', ' ') + " SUNK!", "sunk"));
        }

        return entries;
    }

    /**
     * Checks if a weapon type is available on the given board size.
     */
    public boolean isWeaponAvailable(WeaponType type, int boardSize) {
        return type != null && type.availableFor(boardSize);
    }

    /**
     * Checks if a weapon is available on the given board size.
     */
    public boolean isWeaponAvailable(Weapon weapon, int boardSize) {
        return weapon != null && weapon.availableFor(boardSize);
    }

    /**
     * Returns starting ammo for the given weapon type on the specified board size.
     */
    public int getStartingAmmo(WeaponType type, int boardSize) {
        return type != null ? type.startingAmmo(boardSize) : 0;
    }

    /**
     * Returns starting ammo for the given weapon on the specified board size.
     */
    public int getStartingAmmo(Weapon weapon, int boardSize) {
        return weapon != null ? weapon.startingAmmo(boardSize) : 0;
    }
}
