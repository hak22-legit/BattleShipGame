package com.battleship.view;

import com.battleship.controller.GameController;
import com.battleship.model.Coordinate;
import com.battleship.model.Orientation;
import com.battleship.model.Player;
import com.battleship.model.weapon.Weapon;

import com.battleship.view.battle.WeaponConsole;
import com.battleship.view.quiz.NuclearLaunchDialog;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * composite view base for both battle screens (local + network).
 * composes autonomous components (such as {@link weaponconsole}) and manages
 * the shared targeting ghost preview, fire-click pipeline, and navigation exit.
 */
public abstract class AbstractBattleView {

    protected final ViewNavigator nav;
    protected final GameController controller;
    /** audio facade injected from the navigator — never the static singleton. */
    protected final GameAudio audio;

    protected BoardGridPane ownGrid;
    protected BoardGridPane enemyGrid;
    protected final WeaponConsole weaponConsole = new WeaponConsole();
    protected HBox launcherBar;
    protected final com.battleship.view.battle.TurnClock turnClock =
            new com.battleship.view.battle.TurnClock(this::onTurnExpired);
    private long turnRevision;
    protected boolean viewClosed;
    protected void onTurnExpired() { }
    protected void onBattleClosed() { }
    protected final void startTurnClock() { turnRevision++; turnClock.start(); }
    protected final void stopBattle() {
        viewClosed = true;
        turnRevision++;
        turnClock.stop();
        NuclearLaunchDialog.dismissActive();
        onBattleClosed();
    }

    private final List<Coordinate> ghostCells = new ArrayList<>();

    protected AbstractBattleView(ViewNavigator nav, GameController controller) {
        this.nav = nav;
        this.controller = controller;
        this.audio = nav.getAudio();
    }

    // ================= template method =================

    /** assembles the shared battle skeleton. subclasses customize via hooks only. */
    public final StackPane build() {
        launcherBar = weaponConsole.node();
        refreshLauncherBar();

        ownGrid = createOwnGrid();
        enemyGrid = createEnemyGrid();
        attachFireHandlers();

        Pane layout = assembleLayout();
        StackPane root = decorateRoot(layout);
        root.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (oldScene != null && newScene == null) stopBattle();
        });

        root.setFocusTraversable(true);
        root.setOnKeyPressed(e -> { if (e.getCode().toString().equals("R")) toggleOrientation(); });
        root.setOnMouseClicked(e -> { if (e.getButton() == MouseButton.SECONDARY) toggleOrientation(); });
        root.requestFocus();

        audio.playBattleMusic();
        onViewShown();
        return root;
    }

    // ================= hooks (subclass responsibilities) =================

    /** builds the local player's grid, including any pre-rendered fleet/shots. */
    protected abstract BoardGridPane createOwnGrid();

    /** builds the enemy grid (no ship layout is ever rendered here). */
    protected abstract BoardGridPane createEnemyGrid();

    /** assembles the screen-specific chrome around the shared widgets. */
    protected abstract Pane assembleLayout();

    /** optional root decoration (e.g. animated ocean background). */
    protected abstract StackPane decorateRoot(Pane layout);

    /** called after the screen is visible: turn kick-off, network handlers, etc. */
    protected abstract void onViewShown();

    /** resolves a validated shot — locally via the controller, or over the network. */
    protected abstract void resolveShot(Coordinate anchor);

    /** true when the local player may act right now (gates ghost + fire clicks). */
    protected abstract boolean canFireNow();

    /** extra gating for weapon buttons beyond availability/ammo (network turn). */
    protected boolean extraWeaponGate() { return true; }

    /** the player whose launcher is currently aimed. */
    protected abstract Player firingPlayer();

    /** board size the shot will land on — used for live-cell checks. */
    protected abstract int targetBoardSize();

    /** true when the cell has already been hit/miss/sunk and cannot be re-shot. */
    protected abstract boolean isCellAlreadyResolved(Coordinate c);

    /** ghost highlight style class, e.g. {@link boardgridpane#ghost_target}. */
    protected abstract String ghostStyleClass();

    /** restores one cell after the ghost leaves it. */
    protected abstract void repaintGhostCell(int row, int col);

    /** applies a weapon selection for the firing player. */
    protected abstract void selectWeapon(Weapon weapon);

    /** ui reaction to a click on an already-shelled area. */
    protected abstract void reportBlockedShot();

    /** ui reaction to rejected nuclear launch codes (re-arm default + repaint). */
    protected abstract void onNuclearRejected();

    /** Requests launch authorization; overridable so fire-flow tests need no modal dialog. */
    protected boolean requestNuclearAuthorization() {
        return NuclearLaunchDialog.askAndAwaitAuthorization(enemyGrid.getScene().getWindow());
    }

    /** default orientation label text shared across battle views. */
    protected final String orientationLabelText() {
        Orientation o = firingPlayer().weaponOrientation();
        String mode = o.isHorizontal() ? "\u2194 HORIZONTAL" : "\u2195 VERTICAL";
        return "ORIENTATION: " + mode + "   \u2022   [R / Right-Click] to Rotate   \u2022   Affects Salvo / Nuclear";
    }

    /** shared shot outcome audio playback. */
    protected final void playResultAudio(boolean anyHit, boolean anySunk) {
        if (anySunk) {
            audio.playSunk();
        } else if (anyHit) {
            audio.playHit();
        } else {
            audio.playMiss();
        }
    }

    /** confirmation text shown by the shared exit dialog. */
    protected abstract String exitPrompt();

    /** extra teardown when the player confirms exit (e.g. close socket). */
    protected void onExitConfirmed() { }

    /** notified after the shared orientation toggle; subclasses refresh labels. */
    protected abstract void onOrientationChanged();

    // ================= shared behavior =================

    /** shared fire-click pipeline used by both battle screens. */
    private void handleFireClick(Coordinate anchor) {
        turnClock.poll();
        if (viewClosed || !canFireNow()) return;
        long revision = turnRevision;

        Weapon weapon = firingPlayer().selectedWeapon();
        List<Coordinate> pattern = weapon.calculateBlastArea(anchor, firingOrientation());

        boolean anyLiveCell = pattern.stream().anyMatch(c ->
                c.isWithinBounds(targetBoardSize()) && !isCellAlreadyResolved(c));
        if (!anyLiveCell) {
            reportBlockedShot();
            return;
        }

        if (weapon.requiresAuthorization()) {
            boolean authorized = requestNuclearAuthorization();
            turnClock.poll();
            if (viewClosed || revision != turnRevision || !canFireNow()) return;
            if (!authorized) {
                onNuclearRejected();
                return;
            }
        }

        weapon.playFiringSound(audio);
        clearGhost();
        resolveShot(anchor);
    }


    protected final Orientation firingOrientation() {
        return firingPlayer().weaponOrientation();
    }

    private void attachFireHandlers() {
        int size = enemyGrid.getSize();
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                final int row = r, col = c;
                enemyGrid.getCell(r, c).setOnMouseClicked(e -> {
                    if (e == null || e.getButton() == MouseButton.PRIMARY) handleFireClick(new Coordinate(row, col));
                });
                enemyGrid.getCell(r, c).setOnMouseEntered(e -> showGhost(row, col));
                enemyGrid.getCell(r, c).setOnMouseExited(e -> clearGhost());
            }
        }
    }

    private void showGhost(int row, int col) {
        if (!canFireNow()) return;
        clearGhost();
        Weapon weapon = firingPlayer().selectedWeapon();
        List<Coordinate> cells = weapon.calculateBlastArea(new Coordinate(row, col), firingOrientation());
        int size = enemyGrid.getSize();
        for (Coordinate c : cells) {
            if (!c.isWithinBounds(size)) continue;
            enemyGrid.setCellState(c, ghostStyleClass());
            ghostCells.add(c);
        }
    }

    private void clearGhost() {
        for (Coordinate gc : ghostCells) {
            repaintGhostCell(gc.getRow(), gc.getCol());
        }
        ghostCells.clear();
    }

    protected final void refreshLauncherBar() {
        weaponConsole.refresh(
                firingPlayer(),
                controller.getSelectedTheater().getBoardSize(),
                canFireNow() && extraWeaponGate(),
                weapon -> {
                    selectWeapon(weapon);
                    refreshLauncherBar();
                });
    }

    protected final void toggleOrientation() {
        if (viewClosed || !canFireNow()) return;
        firingPlayer().toggleWeaponOrientation();
        onOrientationChanged();
    }

    private void confirmExit() {
        if (AlertUtil.showConfirmation(nav.window(), "Exit Game", exitPrompt())) {
            stopBattle();
            com.battleship.view.quiz.NuclearResupplyDialog.dismissActive();
            onExitConfirmed();
            audio.stopBgm();
            audio.playMenuMusic();
            nav.showMainMenu();
        }
    }

    /** shared exit button wired to the common confirm dialog. */
    protected final Button buildExitButton() {
        Button exit = new Button("EXIT");
        exit.getStyleClass().add("danger-button");
        exit.setOnAction(e -> { audio.playClick(); confirmExit(); });
        return exit;
    }
}
