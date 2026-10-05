package com.battleship.view.battle;

import com.battleship.controller.BattleService;
import com.battleship.controller.GameController;
import com.battleship.controller.LauncherFireResult;
import com.battleship.model.CellStatus;
import com.battleship.model.Coordinate;
import com.battleship.model.HumanPlayer;
import com.battleship.model.Orientation;
import com.battleship.model.Player;
import com.battleship.model.ShipType;
import com.battleship.model.ShotResult;
import com.battleship.model.mode.BlitzModeStrategy;
import com.battleship.model.mode.ClassicModeStrategy;
import com.battleship.model.mode.ConvoyModeStrategy;
import com.battleship.model.mode.MinefieldModeStrategy;
import com.battleship.model.projection.ShipSnapshot;
import com.battleship.model.weapon.CrossBomb;
import com.battleship.model.weapon.NuclearWarhead;
import com.battleship.model.weapon.StandardShell;
import com.battleship.model.weapon.Weapon;
import com.battleship.model.weapon.WeaponCatalog;
import com.battleship.model.weapon.WeaponType;
import com.battleship.view.FxTestSupport;
import javafx.application.Platform;
import javafx.scene.control.Button;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class BattleViewControllerTest {

    @BeforeAll
    static void initFx() throws Exception {
        FxTestSupport.startToolkit();
    }

    @Test
    void boardSizeRulesForWeapons() {
        BattleService battleService = new BattleService();
        GameController gameController = new GameController(null, battleService);
        BattleViewController controller = new BattleViewController(gameController);

        // 5x5 board
        assertFalse(controller.isWeaponAvailable(WeaponType.CROSS, 5));
        assertFalse(controller.isWeaponAvailable(WeaponType.NUCLEAR, 5));
        assertTrue(controller.isWeaponAvailable(WeaponType.SINGLE, 5));
        assertEquals(0, controller.getStartingAmmo(WeaponType.CROSS, 5));
        assertEquals(0, controller.getStartingAmmo(WeaponType.NUCLEAR, 5));

        // 8x8 board
        assertTrue(controller.isWeaponAvailable(WeaponType.CROSS, 8));
        assertFalse(controller.isWeaponAvailable(WeaponType.NUCLEAR, 8));
        assertEquals(2, controller.getStartingAmmo(WeaponType.CROSS, 8));
        assertEquals(0, controller.getStartingAmmo(WeaponType.NUCLEAR, 8));

        // 10x10 board
        assertTrue(controller.isWeaponAvailable(WeaponType.CROSS, 10));
        assertTrue(controller.isWeaponAvailable(WeaponType.NUCLEAR, 10));
        assertEquals(3, controller.getStartingAmmo(WeaponType.CROSS, 10));
        assertEquals(1, controller.getStartingAmmo(WeaponType.NUCLEAR, 10));
    }

    @Test
    void autoRevertsToDefaultWhenAmmoDepletedOrUnavailable() {
        Player player = new HumanPlayer("Admiral", com.battleship.model.Theater.FLEET_ACTION);
        BattleViewController controller = new BattleViewController(null);

        // Select CROSS on 10x10 and verify it's selected
        player.selectWeapon(WeaponCatalog.crossBomb());
        assertEquals(CrossBomb.ID, player.selectedWeapon().id());

        // Deplete CROSS ammo to 0
        player.arsenal().setAmmo(WeaponType.CROSS, 0);

        // validateAndRevertWeapon should revert to SINGLE
        Weapon reverted = controller.validateAndRevertWeapon(player, 10);
        assertEquals(StandardShell.ID, reverted.id());
        assertEquals(StandardShell.ID, player.selectedWeapon().id());

        // When weapon is unavailable on small board (e.g. Nuclear on 8x8)
        player.armWeapon(WeaponCatalog.nuclear(), Orientation.HORIZONTAL);
        reverted = controller.validateAndRevertWeapon(player, 8);
        assertEquals(StandardShell.ID, reverted.id());
    }

    @Test
    void targetReticleComputesAndClipsSafely() {
        BattleViewController controller = new BattleViewController(null);

        // Standard shell: 1 cell
        List<Coordinate> single = controller.calculateTargetReticle(
                WeaponCatalog.defaultWeapon(), new Coordinate(3, 3), Orientation.HORIZONTAL, 10);
        assertEquals(1, single.size());
        assertEquals(new Coordinate(3, 3), single.get(0));

        // CrossBomb in open waters: 5 cells in '+' pattern
        List<Coordinate> crossCenter = controller.calculateTargetReticle(
                WeaponCatalog.crossBomb(), new Coordinate(4, 4), Orientation.HORIZONTAL, 10);
        assertEquals(5, crossCenter.size());
        assertTrue(crossCenter.contains(new Coordinate(4, 4))); // Center
        assertTrue(crossCenter.contains(new Coordinate(3, 4))); // North
        assertTrue(crossCenter.contains(new Coordinate(5, 4))); // South
        assertTrue(crossCenter.contains(new Coordinate(4, 3))); // West
        assertTrue(crossCenter.contains(new Coordinate(4, 5))); // East

        // CrossBomb at corner (0, 0): North and West clipped out
        List<Coordinate> crossCorner = controller.calculateTargetReticle(
                WeaponCatalog.crossBomb(), new Coordinate(0, 0), Orientation.HORIZONTAL, 10);
        assertEquals(3, crossCorner.size());
        assertTrue(crossCorner.contains(new Coordinate(0, 0)));
        assertTrue(crossCorner.contains(new Coordinate(1, 0)));
        assertTrue(crossCorner.contains(new Coordinate(0, 1)));

        // Nuclear in open waters: 9 cells (3x3 block)
        List<Coordinate> nukeCenter = controller.calculateTargetReticle(
                WeaponCatalog.nuclear(), new Coordinate(5, 5), Orientation.HORIZONTAL, 10);
        assertEquals(9, nukeCenter.size());

        // Nuclear at corner (0, 0): 4 cells in-bounds
        List<Coordinate> nukeCorner = controller.calculateTargetReticle(
                WeaponCatalog.nuclear(), new Coordinate(0, 0), Orientation.HORIZONTAL, 10);
        assertEquals(4, nukeCorner.size());
        assertTrue(nukeCorner.contains(new Coordinate(0, 0)));
        assertTrue(nukeCorner.contains(new Coordinate(0, 1)));
        assertTrue(nukeCorner.contains(new Coordinate(1, 0)));
        assertTrue(nukeCorner.contains(new Coordinate(1, 1)));

        // Nuclear at corner (9, 9): 4 cells in-bounds
        List<Coordinate> nukeBottomRight = controller.calculateTargetReticle(
                WeaponCatalog.nuclear(), new Coordinate(9, 9), Orientation.HORIZONTAL, 10);
        assertEquals(4, nukeBottomRight.size());
        assertTrue(nukeBottomRight.contains(new Coordinate(8, 8)));
        assertTrue(nukeBottomRight.contains(new Coordinate(8, 9)));
        assertTrue(nukeBottomRight.contains(new Coordinate(9, 8)));
        assertTrue(nukeBottomRight.contains(new Coordinate(9, 9)));
    }

    @Test
    void attackLogFormattingForMultiCellAndSingleShots() {
        BattleViewController controller = new BattleViewController(null);

        // Multi-cell Cross bomb: 1 hit, 4 misses
        LauncherFireResult crossResult = new LauncherFireResult(
                List.of(
                        new ShotResult(new Coordinate(2, 2), CellStatus.HIT),
                        new ShotResult(new Coordinate(1, 2), CellStatus.MISS),
                        new ShotResult(new Coordinate(3, 2), CellStatus.MISS),
                        new ShotResult(new Coordinate(2, 1), CellStatus.MISS),
                        new ShotResult(new Coordinate(2, 3), CellStatus.MISS)
                ),
                List.of()
        );

        List<BattleViewController.AttackLogEntry> crossLogs = controller.formatAttackLog(crossResult);
        assertEquals(6, crossLogs.size()); // 5 cells + 1 summary
        assertEquals("  C3: HIT", crossLogs.get(0).message());
        assertEquals("  C2: MISS", crossLogs.get(1).message());
        assertEquals("Cross Bomb detonated: 1 hit, 4 misses.", crossLogs.get(5).message());
        assertEquals("hit", crossLogs.get(5).type());

        // Multi-cell Nuclear strike sinking a ship: 2 hits, 7 misses
        ShipSnapshot sunkDestroyer = new ShipSnapshot(
                ShipType.DESTROYER, List.of(new Coordinate(4, 4), new Coordinate(4, 5)),
                Orientation.HORIZONTAL, 2, true);
        LauncherFireResult nukeResult = new LauncherFireResult(
                List.of(
                        new ShotResult(new Coordinate(4, 4), CellStatus.HIT),
                        new ShotResult(new Coordinate(4, 5), CellStatus.HIT),
                        new ShotResult(new Coordinate(3, 3), CellStatus.MISS),
                        new ShotResult(new Coordinate(3, 4), CellStatus.MISS),
                        new ShotResult(new Coordinate(3, 5), CellStatus.MISS),
                        new ShotResult(new Coordinate(4, 3), CellStatus.MISS),
                        new ShotResult(new Coordinate(5, 3), CellStatus.MISS),
                        new ShotResult(new Coordinate(5, 4), CellStatus.MISS),
                        new ShotResult(new Coordinate(5, 5), CellStatus.MISS)
                ),
                List.of(sunkDestroyer)
        );

        List<BattleViewController.AttackLogEntry> nukeLogs = controller.formatAttackLog(nukeResult);
        assertEquals(11, nukeLogs.size()); // 9 cells + 1 summary + 1 sunk announcement
        assertEquals("Nuclear strike detonated: 2 hits, 7 misses.", nukeLogs.get(9).message());
        assertEquals("DESTROYER SUNK!", nukeLogs.get(10).message());
        assertEquals("sunk", nukeLogs.get(10).type());

        // Single cell direct hit
        LauncherFireResult singleHit = new LauncherFireResult(
                List.of(new ShotResult(new Coordinate(0, 0), CellStatus.HIT)),
                List.of()
        );
        List<BattleViewController.AttackLogEntry> hitLogs = controller.formatAttackLog(singleHit);
        assertEquals(1, hitLogs.size());
        assertEquals("A1: Direct hit!", hitLogs.get(0).message());
        assertEquals("hit", hitLogs.get(0).type());

        // Single cell miss
        LauncherFireResult singleMiss = new LauncherFireResult(
                List.of(new ShotResult(new Coordinate(0, 1), CellStatus.MISS)),
                List.of()
        );
        List<BattleViewController.AttackLogEntry> missLogs = controller.formatAttackLog(singleMiss);
        assertEquals(1, missLogs.size());
        assertEquals("B1: Nothing but spray \u2014 miss.", missLogs.get(0).message());
        assertEquals("miss", missLogs.get(0).type());
    }

    @Test
    void weaponConsoleUiBindingsOnFxThread() throws Exception {
        CompletableFuture<Void> future = new CompletableFuture<>();

        Platform.runLater(() -> {
            try {
                WeaponConsole console = new WeaponConsole();
                BattleViewController controller = new BattleViewController(null);

                // --- Test 5x5 board ---
                Player player5 = new HumanPlayer("P5", com.battleship.model.Theater.SKIRMISH);
                controller.synchronizeWeaponConsole(console, player5, 5, true, w -> {});

                Button singleBtn5 = console.getButton(WeaponCatalog.defaultWeapon());
                Button crossBtn5 = console.getButton(WeaponCatalog.crossBomb());
                Button nukeBtn5 = console.getButton(WeaponCatalog.nuclear());

                assertNotNull(singleBtn5);
                assertTrue(singleBtn5.isVisible());
                assertTrue(singleBtn5.isManaged());

                assertNotNull(crossBtn5);
                assertFalse(crossBtn5.isVisible());
                assertFalse(crossBtn5.isManaged());

                assertNotNull(nukeBtn5);
                assertFalse(nukeBtn5.isVisible());
                assertFalse(nukeBtn5.isManaged());

                // --- Test 8x8 board ---
                Player player8 = new HumanPlayer("P8", com.battleship.model.Theater.ENGAGEMENT);
                controller.synchronizeWeaponConsole(console, player8, 8, true, w -> {});

                Button crossBtn8 = console.getButton(WeaponCatalog.crossBomb());
                Button nukeBtn8 = console.getButton(WeaponCatalog.nuclear());

                assertNotNull(crossBtn8);
                assertTrue(crossBtn8.isVisible());
                assertTrue(crossBtn8.isManaged());
                assertTrue(crossBtn8.getText().contains("(2)"));

                assertNotNull(nukeBtn8);
                assertFalse(nukeBtn8.isVisible());
                assertFalse(nukeBtn8.isManaged());

                // --- Test 10x10 board ---
                Player player10 = new HumanPlayer("P10", com.battleship.model.Theater.FLEET_ACTION);
                controller.synchronizeWeaponConsole(console, player10, 10, true, w -> {});

                Button crossBtn10 = console.getButton(WeaponCatalog.crossBomb());
                Button nukeBtn10 = console.getButton(WeaponCatalog.nuclear());

                assertNotNull(crossBtn10);
                assertTrue(crossBtn10.isVisible());
                assertTrue(crossBtn10.isManaged());
                assertTrue(crossBtn10.getText().contains("(3)"));

                assertNotNull(nukeBtn10);
                assertTrue(nukeBtn10.isVisible());
                assertTrue(nukeBtn10.isManaged());
                assertTrue(nukeBtn10.getText().contains("(1)"));

                // --- Test Ammo Depletion on 10x10 ---
                player10.selectWeapon(WeaponCatalog.nuclear());
                assertEquals(NuclearWarhead.ID, player10.selectedWeapon().id());

                // Deplete nuclear ammo to 0
                player10.arsenal().setAmmo(WeaponType.NUCLEAR, 0);

                // Refresh console
                controller.synchronizeWeaponConsole(console, player10, 10, true, w -> {});

                Button depletedNukeBtn = console.getButton(WeaponCatalog.nuclear());
                assertNotNull(depletedNukeBtn);
                assertTrue(depletedNukeBtn.isDisable());
                assertTrue(depletedNukeBtn.getText().contains("(0)"));

                // Selected weapon should have auto-reverted to SINGLE
                assertEquals(StandardShell.ID, player10.selectedWeapon().id());

                future.complete(null);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });

        future.get(5, TimeUnit.SECONDS);
    }

    @Test
    void gameModeStrategyIntegration() {
        BattleService battleService = new BattleService();
        GameController gameController = new GameController(null, battleService);
        BattleViewController controller = new BattleViewController(gameController);

        Player p1 = new HumanPlayer("Admiral 1", com.battleship.model.Theater.FLEET_ACTION);
        Player p2 = new HumanPlayer("Admiral 2", com.battleship.model.Theater.FLEET_ACTION);

        // Classic mode
        battleService.init(p1, p2, new ClassicModeStrategy());
        assertInstanceOf(ClassicModeStrategy.class, controller.getGameModeStrategy());

        // Convoy mode
        battleService.init(p1, p2, new ConvoyModeStrategy());
        assertInstanceOf(ConvoyModeStrategy.class, controller.getGameModeStrategy());

        // Minefield mode
        battleService.init(p1, p2, new MinefieldModeStrategy());
        assertInstanceOf(MinefieldModeStrategy.class, controller.getGameModeStrategy());

        // Blitz mode
        battleService.init(p1, p2, new BlitzModeStrategy());
        assertInstanceOf(BlitzModeStrategy.class, controller.getGameModeStrategy());

        // Arsenal access
        assertNotNull(controller.getArsenal(p1));
        assertEquals(p1.arsenal(), controller.getArsenal(p1));
    }
}
