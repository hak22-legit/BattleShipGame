package com.battleship.ai;

import com.battleship.model.Coordinate;
import com.battleship.model.HumanPlayer;
import com.battleship.model.Player;
import com.battleship.model.ShotOrder;
import com.battleship.model.Theater;
import com.battleship.model.fog.MarkerStatus;
import com.battleship.model.fog.TrackingGrid;
import com.battleship.model.weapon.WeaponCatalog;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** verifies the shared hunt heuristic: parity preference, unshot-only picks, fallback. */
class ParityHunterTest {

    @Test
    void alwaysPicksAnUnshotEvenParityCellOnAFreshBoard() {
        TrackingGrid board = TrackingGrid.blind(10);
        for (int i = 0; i < 200; i++) {
            Coordinate pick = ParityHunter.pick(board, new java.security.SecureRandom());
            assertTrue(pick.isWithinBounds(10));
            assertEquals(MarkerStatus.UNKNOWN, board.observedStatus(pick));
            assertEquals(0, (pick.getRow() + pick.getCol()) % 2,
                    "checkerboard parity must be preferred while even cells remain");
        }
    }

    @Test
    void fallsBackToAnyUnshotCellWhenNoEvenParityCellRemains() {
        TrackingGrid board = TrackingGrid.blind(3);
        // shell every even-parity cell; only odd-parity cells stay unshot.
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                if ((r + c) % 2 == 0) board.recordShotOutcome(new Coordinate(r, c), MarkerStatus.MISS);
            }
        }
        assertEquals(4, board.unshotCells().size());

        Coordinate pick = ParityHunter.pick(board, new java.security.SecureRandom());

        assertTrue(board.unshotCells().contains(pick), "pick must be an unshot cell");
        assertEquals(1, (pick.getRow() + pick.getCol()) % 2,
                "fallback must kick in once even-parity cells are exhausted");
    }

    @Test
    void isDeterministicForTheSameSeed() {
        Coordinate a = ParityHunter.pick(TrackingGrid.blind(10), new Random(7));
        Coordinate b = ParityHunter.pick(TrackingGrid.blind(10), new Random(7));
        assertEquals(a, b);
    }

    @Test
    void respectsAmmoReadOnlyContractWhenUsedThroughHuntTargetAI() {
        // integration sanity: the refactored hunttargetai still plans legal shots.
        Player p = new HumanPlayer("AI", Theater.FLEET_ACTION);
        HuntTargetAI ai = new HuntTargetAI();

        ShotOrder plan = ai.chooseShotPlan(TrackingGrid.blind(10), p);

        assertTrue(plan.anchor().isWithinBounds(10));
        assertEquals(3, p.ammoCount(WeaponCatalog.crossBomb()));
        assertEquals(1, p.ammoCount(WeaponCatalog.nuclear()));
    }
}
