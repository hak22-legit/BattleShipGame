package com.battleship.model.mode;

import com.battleship.model.CellStatus;
import com.battleship.model.Coordinate;
import com.battleship.model.HumanPlayer;
import com.battleship.model.Orientation;
import com.battleship.model.Player;
import com.battleship.model.PrimaryGrid;
import com.battleship.model.ShipType;
import com.battleship.model.ShotResult;
import com.battleship.model.Theater;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameModeStrategyTest {

    @Test
    void classicModeEnforcesStandardFleetAnnihilation() {
        ClassicModeStrategy classic = new ClassicModeStrategy();
        assertTrue(classic.isBoardSizeAllowed(5));
        assertTrue(classic.isBoardSizeAllowed(10));

        Player p1 = new HumanPlayer("P1", Theater.SKIRMISH);
        Player p2 = new HumanPlayer("P2", Theater.SKIRMISH);
        p2.deploy(ShipType.PATROL_BOAT, new Coordinate(0, 0), Orientation.HORIZONTAL);

        VictoryResult initial = classic.evaluateVictory(p1, p2, 1);
        assertFalse(initial.isGameOver());

        // Sink patrol boat
        p2.receiveShot(new Coordinate(0, 0));
        p2.receiveShot(new Coordinate(0, 1));
        assertTrue(p2.isFleetDestroyed());

        VictoryResult finalVictory = classic.evaluateVictory(p1, p2, 2);
        assertTrue(finalVictory.isGameOver());
        assertEquals(p1, finalVictory.winner());
        assertTrue(finalVictory.victoryReason().contains("annihilated"));
    }

    @Test
    void convoyModeRestrictedTo8x8And10x10Only() {
        ConvoyModeStrategy convoy = new ConvoyModeStrategy();
        assertFalse(convoy.isBoardSizeAllowed(5), "5x5 boards must be disallowed for Convoy mode");
        assertTrue(convoy.isBoardSizeAllowed(8));
        assertTrue(convoy.isBoardSizeAllowed(10));

        Player p1 = new HumanPlayer("P1", Theater.SKIRMISH);
        Player p2 = new HumanPlayer("P2", Theater.SKIRMISH);
        assertThrows(IllegalArgumentException.class, () -> convoy.initializeMatch(p1, p2, 5));
    }

    @Test
    void convoyModeAttackerWinsImmediatelyIfFreighterSinks() {
        Player attacker = new HumanPlayer("Attacker", Theater.ENGAGEMENT);
        Player defender = new HumanPlayer("Defender", Theater.ENGAGEMENT);

        ConvoyModeStrategy convoy = new ConvoyModeStrategy(defender);
        convoy.initializeMatch(attacker, defender, 8);

        // Verify 3-cell Freighter is deployed on defender's board
        assertTrue(defender.primaryGrid().fleet().stream().anyMatch(s -> s.type() == ShipType.FREIGHTER));
        var freighter = defender.primaryGrid().fleet().stream()
                .filter(s -> s.type() == ShipType.FREIGHTER)
                .findFirst()
                .orElseThrow();
        assertEquals(3, freighter.cells().size());

        // Shoot and sink the freighter
        for (Coordinate c : freighter.cells()) {
            defender.receiveShot(c);
        }

        VictoryResult victory = convoy.evaluateVictory(attacker, defender, 3);
        assertTrue(victory.isGameOver());
        assertEquals(attacker, victory.winner(), "Attacker must win immediately when freighter sinks");
        assertTrue(victory.victoryReason().contains("Freighter sunk"));
    }

    @Test
    void convoyModeDefenderWinsIfFreighterSurvives15Rounds() {
        Player attacker = new HumanPlayer("Attacker", Theater.ENGAGEMENT);
        Player defender = new HumanPlayer("Defender", Theater.ENGAGEMENT);

        ConvoyModeStrategy convoy = new ConvoyModeStrategy(defender);
        convoy.initializeMatch(attacker, defender, 8);

        VictoryResult round14 = convoy.evaluateVictory(attacker, defender, 14);
        assertFalse(round14.isGameOver());

        VictoryResult round15 = convoy.evaluateVictory(attacker, defender, 15);
        assertTrue(round15.isGameOver());
        assertEquals(defender, round15.winner(), "Defender wins if freighter survives 15 rounds");
        assertTrue(round15.victoryReason().contains("survived 15 rounds"));
    }

    @Test
    void minefieldModeSeedsNeutralMinesAndDetonatesSecondaryCrossOnImpact() {
        Player attacker = new HumanPlayer("Attacker", Theater.ENGAGEMENT);
        Player defender = new HumanPlayer("Defender", Theater.ENGAGEMENT);

        // Deploy a ship near the mine for damage observation
        defender.deploy(ShipType.PATROL_BOAT, new Coordinate(3, 4), Orientation.HORIZONTAL);

        MinefieldModeStrategy minefield = new MinefieldModeStrategy(3);
        minefield.initializeMatch(attacker, defender, 8);

        // Add explicit mine at (3, 3)
        Coordinate mineCoord = new Coordinate(3, 3);
        minefield.addSeaMine(mineCoord);

        // Hit mine at (3, 3)
        ShotResult directShot = defender.receiveShot(mineCoord);
        assertEquals(CellStatus.MISS, directShot.outcome());

        // Trigger post-shot hook
        List<ShotResult> secondaries = minefield.onShotResolved(attacker, defender, mineCoord, List.of(directShot));

        // Secondary cross (+) blast centered at (3, 3) covers:
        // Center: (3, 3) - already shot
        // N: (2, 3), S: (4, 3), W: (3, 2), E: (3, 4) - which contains the Patrol Boat segment!
        assertFalse(secondaries.isEmpty(), "Secondary explosion must detonate");
        assertTrue(secondaries.stream().anyMatch(sr -> sr.coordinate().equals(new Coordinate(3, 4))),
                "Secondary Cross blast must hit Patrol Boat at (3, 4)");
        assertEquals(CellStatus.HIT, defender.cellStatus(new Coordinate(3, 4)));
    }

    @Test
    void blitzModeTracks90sCountdownWithBonusAndTriggersInstantLossOnFlagFall() {
        Player p1 = new HumanPlayer("P1", Theater.FLEET_ACTION);
        Player p2 = new HumanPlayer("P2", Theater.FLEET_ACTION);

        BlitzModeStrategy blitz = new BlitzModeStrategy();
        blitz.initializeMatch(p1, p2, 10);

        assertEquals(90.0, blitz.getRemainingTimeSeconds(p1));
        assertEquals(90.0, blitz.getRemainingTimeSeconds(p2));

        // p1 takes a shot after 10 seconds: remaining = 90 - 10 + 2 = 82s
        blitz.recordTimeDeductionAndBonus(p1, 10_000L);
        assertEquals(82.0, blitz.getRemainingTimeSeconds(p1));

        VictoryResult ongoing = blitz.evaluateVictory(p1, p2, 1);
        assertFalse(ongoing.isGameOver());

        // Flag fall: player 1 exceeds remaining time
        blitz.recordTimeDeductionAndBonus(p1, 85_000L); // 82 - 85 = -3s -> 0:00 flag fall
        assertEquals(0.0, blitz.getRemainingTimeSeconds(p1));
        assertTrue(blitz.isFlagged());
        assertEquals(p1, blitz.getFlaggedPlayer());

        VictoryResult flagVictory = blitz.evaluateVictory(p1, p2, 1);
        assertTrue(flagVictory.isGameOver());
        assertEquals(p2, flagVictory.winner(), "Opponent must win instantly on flag-fall");
        assertTrue(flagVictory.victoryReason().contains("Flag-fall"));
    }
}
