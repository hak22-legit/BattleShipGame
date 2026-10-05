package com.battleship.model;

import com.battleship.model.fog.TrackingGrid;
import com.battleship.model.projection.ShipSnapshot;
import com.battleship.model.weapon.Weapon;
import com.battleship.model.weapon.WeaponCatalog;
import com.battleship.model.weapon.WeaponType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * area-of-effect and fog-of-war attack resolution engine.
 * strictly pure domain service with zero JavaFX imports.
 */
public class AttackService {

    /**
     * resolves an attack against defender grid using the specified weapon and target coordinate.
     * handles blast calculation, out-of-bounds clipping, multi-hit ship damage,
     * trivia gate authorization, ammunition deduction, and fog-of-war updating.
     */
    public AttackResult executeAttack(
            PlayerArsenal attackerArsenal,
            ShotTarget defenderTarget,
            TrackingGrid attackerKnowledge,
            WeaponType weaponType,
            Coordinate target,
            Orientation orientation,
            boolean quizAuthorized,
            MatchConfig matchConfig
    ) {
        Objects.requireNonNull(attackerArsenal, "attackerArsenal must not be null");
        Objects.requireNonNull(defenderTarget, "defenderTarget must not be null");
        Objects.requireNonNull(weaponType, "weaponType must not be null");
        Objects.requireNonNull(target, "target coordinate must not be null");

        int boardSize = defenderTarget.size();
        if (matchConfig == null) {
            matchConfig = MatchConfig.standard(boardSize);
        }

        // Nuclear Quiz Gate:
        // If matchConfig.quizEnabled() is true: Nuclear requires trivia authorization.
        // If trivia fails: strike is cancelled, ammo remains untouched, weapon selection reverts to SINGLE.
        if (weaponType == WeaponType.NUCLEAR && matchConfig.quizEnabled() && !quizAuthorized) {
            attackerArsenal.select(WeaponType.SINGLE);
            return AttackResult.cancelled(target, WeaponType.SINGLE);
        }

        // Validate availability and ammunition
        if (!attackerArsenal.canFire(weaponType)) {
            throw new IllegalStateException("Cannot fire " + weaponType + ": ammo depleted or weapon unavailable for board size " + boardSize);
        }

        // Calculate all valid in-bounds cells in the blast zone
        List<Coordinate> candidates = calculateBlastZone(weaponType, target, orientation);
        List<Coordinate> inBoundsCells = new ArrayList<>(candidates.size());
        for (Coordinate c : candidates) {
            if (c.isWithinBounds(boardSize)) {
                inBoundsCells.add(c);
            }
        }

        List<ShotResult> rawResults = new ArrayList<>();
        List<ShipSnapshot> sunkShips = new ArrayList<>();

        // Every unattacked cell inside the zone must update state:
        // Ship present -> HIT, decrement health, check if sunk
        // Empty water -> MISS
        // Multi-hit ship rule: Each caught segment sustains damage simultaneously
        for (Coordinate c : inBoundsCells) {
            if (defenderTarget.isCellResolved(c)) {
                continue; // skip already attacked cells
            }
            ShotResult result = defenderTarget.receiveShot(c);
            rawResults.add(result);
            if (result.outcome() == CellStatus.SUNK && result.shipSunk() != null) {
                boolean alreadyRecorded = sunkShips.stream()
                        .anyMatch(s -> s.cells().equals(result.shipSunk().cells()));
                if (!alreadyRecorded) {
                    sunkShips.add(result.shipSunk());
                }
            }
        }

        // If any ship was sunk during this attack, synchronize all hit segments of that ship in this attack to SUNK
        List<ShotResult> finalResults = new ArrayList<>(rawResults.size());
        for (ShotResult sr : rawResults) {
            ShipSnapshot matchingSunk = null;
            for (ShipSnapshot sunk : sunkShips) {
                if (sunk.cells().contains(sr.coordinate())) {
                    matchingSunk = sunk;
                    break;
                }
            }
            if (matchingSunk != null && sr.outcome() != CellStatus.SUNK) {
                finalResults.add(new ShotResult(sr.coordinate(), CellStatus.SUNK, matchingSunk));
            } else {
                finalResults.add(sr);
            }
        }

        // Deduct 1 ammo for the selected weapon
        attackerArsenal.consumeAmmo(weaponType);

        // Update attacker's Fog-of-War knowledge grid
        if (attackerKnowledge != null) {
            for (ShotResult sr : finalResults) {
                attackerKnowledge.recordShotOutcome(sr.coordinate(), sr.outcome());
            }
            for (ShipSnapshot sunk : sunkShips) {
                attackerKnowledge.recordWreck(sunk.type(), sunk.cells(), sunk.orientation());
            }
        }

        // Reset weapon selection after shot
        attackerArsenal.resetAfterShot();

        int hits = (int) finalResults.stream().filter(ShotResult::isHit).count();
        int misses = (int) finalResults.stream().filter(sr -> sr.outcome() == CellStatus.MISS).count();

        return new AttackResult(target, weaponType, finalResults, sunkShips, hits, misses, true, false);
    }

    /** convenience overload for Player objects. */
    public AttackResult executeAttack(
            Player attacker,
            Player defender,
            WeaponType weaponType,
            Coordinate target,
            Orientation orientation,
            boolean quizAuthorized,
            MatchConfig matchConfig
    ) {
        return executeAttack(
                attacker.arsenal(),
                defender,
                attacker.trackingGrid(),
                weaponType,
                target,
                orientation,
                quizAuthorized,
                matchConfig
        );
    }

    public AttackResult executeAttack(Player attacker, Player defender, Coordinate target, boolean quizAuthorized, MatchConfig matchConfig) {
        Weapon selected = attacker.selectedWeapon();
        WeaponType type = WeaponType.fromId(selected.id());
        return executeAttack(attacker, defender, type, target, attacker.weaponOrientation(), quizAuthorized, matchConfig);
    }

    public AttackResult executeAttack(Player attacker, Player defender, Coordinate target) {
        return executeAttack(attacker, defender, target, true, MatchConfig.standard(defender.size()));
    }

    /**
     * calculates raw blast zone coordinates for a weapon type centered at the target anchor.
     * coordinates out of bounds are preserved here for callers to clip safely.
     */
    public List<Coordinate> calculateBlastZone(WeaponType type, Coordinate anchor, Orientation orientation) {
        if (type == null) {
            return List.of(anchor);
        }
        return switch (type) {
            case SINGLE -> List.of(anchor);
            case CROSS -> {
                List<Coordinate> cells = new ArrayList<>(5);
                cells.add(anchor); // center
                cells.add(new Coordinate(anchor.getRow() - 1, anchor.getCol())); // north
                cells.add(new Coordinate(anchor.getRow() + 1, anchor.getCol())); // south
                cells.add(new Coordinate(anchor.getRow(), anchor.getCol() - 1)); // west
                cells.add(new Coordinate(anchor.getRow(), anchor.getCol() + 1)); // east
                yield cells;
            }
            case NUCLEAR -> {
                List<Coordinate> cells = new ArrayList<>(9);
                for (int dr = -1; dr <= 1; dr++) {
                    for (int dc = -1; dc <= 1; dc++) {
                        cells.add(new Coordinate(anchor.getRow() + dr, anchor.getCol() + dc));
                    }
                }
                yield cells;
            }
        };
    }
}
