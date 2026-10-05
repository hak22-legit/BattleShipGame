package com.battleship.ai;

import com.battleship.model.AmmoReadout;
import com.battleship.model.Coordinate;
import com.battleship.model.Orientation;
import com.battleship.model.ShotOrder;
import com.battleship.model.ShotResult;
import com.battleship.model.fog.TrackingGrid;
import com.battleship.model.weapon.WeaponCatalog;

import java.security.SecureRandom;

/**
 * lieutenant (normal) difficulty: hunt/target state machine.
 * hunt  -> checkerboard-parity random search (exploits min ship size = 2).
 * target -> on hit, queue orthogonal neighbors and follow the line.
 */
public class HuntTargetAI implements AIStrategy {

    private final TargetingQueue targetQueue = new TargetingQueue();
    private final SecureRandom random = new SecureRandom();
    private int lastBoardSize = -1;

    @Override
    public Coordinate chooseTarget(TrackingGrid knowledge) {
        lastBoardSize = knowledge.size();

        // target mode: drain queue, skip any coordinate already shot at.
        Coordinate queued = targetQueue.nextTarget(knowledge);
        if (queued != null) return queued;

        // hunt mode: checkerboard parity over unshot cells (shared heuristic, dry).
        return ParityHunter.pick(knowledge, random);
    }

    @Override
    public void notifyResult(ShotResult result) {
        if (!result.isHit()) return;
        if (result.outcome() == com.battleship.model.CellStatus.SUNK) {
            targetQueue.clear();
            return;
        }
        targetQueue.enqueueNeighbors(result.coordinate(), lastBoardSize);
    }

    /**
     * normal ai stays precise once it has a lead (target mode -> standard shots),
     * but while blind-searching (hunt mode) it occasionally spends salvo ammo to
     * cover 3 cells at once instead of 1.
     */
    @Override
    public ShotOrder chooseShotPlan(TrackingGrid knowledge, AmmoReadout ammo) {
        var cross = WeaponCatalog.crossBomb();
        boolean hunting = !targetQueue.hasTargets();
        if (hunting && ammo.hasAmmo(cross) && !ammo.isAmmoInfinite(cross) && random.nextInt(4) == 0) {
            // same shared hunt heuristic as choosetarget — no duplicated parity block.
            Coordinate anchor = ParityHunter.pick(knowledge, random);
            return new ShotOrder(cross, anchor, Orientation.random(random));
        }
        return AIStrategy.super.chooseShotPlan(knowledge, ammo);
    }
}
