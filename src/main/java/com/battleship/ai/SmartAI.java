package com.battleship.ai;

import com.battleship.model.AmmoReadout;
import com.battleship.model.CellStatus;
import com.battleship.model.Coordinate;
import com.battleship.model.Orientation;
import com.battleship.model.ShipType;
import com.battleship.model.ShotOrder;
import com.battleship.model.ShotResult;
import com.battleship.model.fog.MarkerStatus;
import com.battleship.model.fog.TrackingGrid;
import com.battleship.model.weapon.BlastPattern;
import com.battleship.model.weapon.Weapon;
import com.battleship.model.weapon.WeaponCatalog;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * admiral (hard) difficulty: builds a probability density map over unshot
 * cells (how many valid remaining-ship placements would cover each cell)
 * and fires at the highest-probability cell. falls back to target-following
 * behavior right after a hit, since that's more precise than pure probability
 * once a ship has been found.
 *
 * <p>fixes v1.3: the density map is built from the {@link trackinggrid} — the
 * roster minus announced wrecks — instead of peeking at the defender's live
 * fleet. uses composition (targetingqueue) instead of inheriting from hunttargetai.</p>
 */
public class SmartAI implements AIStrategy {

    private final TargetingQueue targetQueue = new TargetingQueue();
    private final SecureRandom random = new SecureRandom();
    private int lastBoardSize = -1;

    @Override
    public Coordinate chooseTarget(TrackingGrid knowledge) {
        lastBoardSize = knowledge.size();

        // if we're actively finishing off a located ship, defer to the queue.
        Coordinate queued = targetQueue.nextTarget(knowledge);
        if (queued != null) return queued;

        int size = knowledge.size();
        int[][] density = new int[size][size];
        List<ShipType> remaining = knowledge.remainingShipPool();
        if (remaining.isEmpty()) {
            return ParityHunter.pick(knowledge, random);
        }

        for (ShipType type : remaining) {
            int len = type.getSize();
            for (int r = 0; r < size; r++) {
                for (int c = 0; c <= size - len; c++) {
                    if (fits(knowledge, r, c, len, Orientation.HORIZONTAL)) {
                        for (int i = 0; i < len; i++) density[r][c + i]++;
                    }
                }
            }
            for (int c = 0; c < size; c++) {
                for (int r = 0; r <= size - len; r++) {
                    if (fits(knowledge, r, c, len, Orientation.VERTICAL)) {
                        for (int i = 0; i < len; i++) density[r + i][c]++;
                    }
                }
            }
        }

        int best = -1;
        List<Coordinate> bestCells = new ArrayList<>();
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                Coordinate coord = new Coordinate(r, c);
                if (knowledge.isAlreadyShelled(coord)) continue; // can't fire twice
                if (density[r][c] > best) {
                    best = density[r][c];
                    bestCells.clear();
                    bestCells.add(coord);
                } else if (density[r][c] == best) {
                    bestCells.add(coord);
                }
            }
        }

        if (bestCells.isEmpty()) return ParityHunter.pick(knowledge, random);
        return bestCells.get(random.nextInt(bestCells.size()));
    }

    @Override
    public void notifyResult(ShotResult result) {
        if (!result.isHit()) return;
        if (result.outcome() == CellStatus.SUNK) {
            targetQueue.clear();
            return;
        }
        targetQueue.enqueueNeighbors(result.coordinate(), lastBoardSize);
    }

    /** a cell that is known empty (miss) or confirmed wreckage cannot host a living hull. */
    private boolean fits(TrackingGrid knowledge, int row, int col, int len, Orientation orientation) {
        for (int i = 0; i < len; i++) {
            int r = orientation.isHorizontal() ? row : row + i;
            int c = orientation.isHorizontal() ? col + i : col;
            MarkerStatus marker = knowledge.observedStatus(new Coordinate(r, c));
            if (marker == MarkerStatus.MISS || marker == MarkerStatus.SUNK) return false;
        }
        return true;
    }

    /**
     * admiral ai spends its limited-ammo weapons deliberately: it looks for the
     * blast block containing the most still-unshot cells and only fires it if that
     * block is mostly "fresh" — otherwise it saves the ammo and falls back to a
     * precise single standard shot.
     */
    @Override
    public ShotOrder chooseShotPlan(TrackingGrid knowledge, AmmoReadout ammo) {
        if (targetQueue.hasTargets()) {
            return new ShotOrder(WeaponCatalog.standard(), chooseTarget(knowledge), Orientation.HORIZONTAL);
        }

        int size = knowledge.size();
        Weapon nuclear = WeaponCatalog.nuclear();
        if (ammo.hasAmmo(nuclear) && !ammo.isAmmoInfinite(nuclear) && size >= 10) {
            ShotOrder plan = bestBlock(knowledge, nuclear);
            if (plan != null) return plan;
        }
        Weapon salvo = WeaponCatalog.salvo();
        if (ammo.hasAmmo(salvo) && !ammo.isAmmoInfinite(salvo) && size >= 8) {
            ShotOrder plan = bestBlock(knowledge, salvo);
            if (plan != null) return plan;
        }
        return new ShotOrder(WeaponCatalog.standard(), chooseTarget(knowledge), Orientation.HORIZONTAL);
    }

    /** finds the best-scoring placement for an area weapon; null if not worth the ammo. */
    private ShotOrder bestBlock(TrackingGrid knowledge, Weapon weapon) {
        int size = knowledge.size();
        // ask the weapon for its own blast geometry (ocp-safe): no hardcoded dims here.
        BlastPattern pattern = weapon.blastPattern();

        int bestScore = -1;
        Coordinate bestAnchor = null;
        Orientation bestOrientation = Orientation.HORIZONTAL;

        for (Orientation orientation : Orientation.values()) {
            BlastPattern laid = pattern.rotatedTo(orientation);
            for (int r = 0; r <= size - laid.rows(); r++) {
                for (int c = 0; c <= size - laid.cols(); c++) {
                    int score = 0;
                    for (Coordinate cell : laid.coverage(new Coordinate(r, c))) {
                        if (!knowledge.isAlreadyShelled(cell)) score++;
                    }

                    if (score > bestScore) {
                        bestScore = score;
                        bestAnchor = new Coordinate(r, c);
                        bestOrientation = orientation;
                    }
                }
            }
        }

        int totalCells = pattern.cellCount();
        // only worth the ammo if at least half the covered cells are still unshot.
        if (bestAnchor == null || bestScore < (totalCells / 2 + 1)) return null;
        return new ShotOrder(weapon, bestAnchor, bestOrientation);
    }
}
