package com.battleship.view;

import com.battleship.controller.GameController;
import com.battleship.controller.NetworkFireService;
import com.battleship.model.CellStatus;
import com.battleship.model.Coordinate;
import com.battleship.model.Orientation;
import com.battleship.model.Player;
import com.battleship.model.ShotResult;
import com.battleship.model.fog.MarkerStatus;
import com.battleship.model.fog.TrackingGrid;
import com.battleship.model.projection.ShipSnapshot;
import com.battleship.model.weapon.NuclearWarhead;
import com.battleship.model.weapon.Weapon;
import com.battleship.model.weapon.WeaponCatalog;
import java.util.List;
import com.battleship.net.NetMessage;
import com.battleship.net.NetworkBattleMediator;
import com.battleship.net.NetworkGameSession;
import com.battleship.view.quiz.NuclearResupplyDialog;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * battle screen for a network ("play with a friend") match. extends
 * {@link abstractbattleview} (shared weapon bar, ghost preview, fire pipeline)
 * and only contributes the network shot resolution: firing sends a fire
 * message and the defender resolves it locally and replies with fire_result.
 * no ship layout is ever transmitted.
 */
public class NetworkBattleView extends AbstractBattleView {

    private final NetworkGameSession netSession;
    private final Player me;
    private final NetworkBattleMediator mediator;

    private Label turnLabel;
    private Label logLabel;
    private Label orientationLabel;
    private Label fleetStatusLabel;
    private Label ownFleetLabel;
    private Label enemyFleetLabel;
    private boolean waitingForResult;

    public NetworkBattleView(ViewNavigator nav, GameController controller, NetworkGameSession netSession) {
        super(nav, controller);
        this.netSession = netSession;
        this.me = netSession.getMe();
        this.mediator = new NetworkBattleMediator(netSession);
    }

    // ---------- abstractbattleview hooks ----------

    @Override
    protected Player firingPlayer() { return me; }

    @Override
    protected boolean canFireNow() { return !gameOver && !viewClosed && !waitingForResult && netSession.isMyTurn(); }

    @Override
    protected boolean extraWeaponGate() { return netSession.isMyTurn(); }

    @Override
    protected int targetBoardSize() { return netSession.getEnemyKnowledge().size(); }

    @Override
    protected boolean isCellAlreadyResolved(Coordinate c) {
        return netSession.getEnemyKnowledge().isAlreadyShelled(c);
    }

    @Override
    protected String ghostStyleClass() {
        return BoardGridPane.GHOST_TARGET;
    }

    @Override
    protected void repaintGhostCell(int row, int col) {
        Coordinate c = new Coordinate(row, col);
        MarkerStatus status = netSession.getEnemyKnowledge().observedStatus(c);
        if (status == MarkerStatus.HIT) {
            enemyGrid.renderShot(c, CellStatus.HIT, false);
        } else if (status == MarkerStatus.MISS) {
            enemyGrid.renderShot(c, CellStatus.MISS, false);
        } else if (status == MarkerStatus.SUNK) {
            TrackingGrid.DiscoveredWreck wreck = netSession.getEnemyKnowledge().confirmedSunk().stream()
                    .filter(w -> w.cells().contains(c))
                    .findFirst()
                    .orElse(null);
            if (wreck != null) {
                enemyGrid.renderSunkShip(wreck.type(), wreck.orientation(), wreck.cells(), false);
            } else {
                enemyGrid.renderSunkShip(List.of(c), false);
            }
        } else {
            enemyGrid.resetCellStyle(row, col);
        }
    }

    @Override
    protected void selectWeapon(Weapon weapon) {
        controller.selectWeapon(me, weapon);
    }

    @Override
    protected void reportBlockedShot() {
        logLabel.setText("That area is already fully shelled, Admiral.");
    }

    @Override
    protected void onNuclearRejected() {
        controller.selectWeapon(me, WeaponCatalog.defaultWeapon());
        logLabel.setText("Launch codes rejected. Nuclear strike aborted \u2014 Default weapon re-armed.");
        refreshLauncherBar();
    }

    @Override
    protected String exitPrompt() {
        return "Leave this match and return to the main menu? This will disconnect your opponent.";
    }

    @Override
    protected void onExitConfirmed() {
        netSession.getSession().close();
    }

    @Override
    protected BoardGridPane createOwnGrid() {
        BoardGridPane grid = new BoardGridPane(me.size());
        for (ShipSnapshot s : me.fleet()) {
            if (!s.isSunk()) grid.renderShip(s);
        }
        return grid;
    }

    @Override
    protected BoardGridPane createEnemyGrid() {
        return new BoardGridPane(netSession.getEnemyKnowledge().size());
    }

    @Override
    protected Pane assembleLayout() {
        turnLabel = LanLobbyLayout.text(netSession.isMyTurn() ? "YOUR TURN" : "OPPONENT'S TURN", "battle-turn-title");
        Label title = LanLobbyLayout.text("NAVAL COMMAND  /  LAN BATTLE", "lan-eyebrow");
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        HBox header = new HBox(24, new VBox(7, title, turnLabel), spacer, turnClock.node(), buildExitButton());
        header.setAlignment(Pos.CENTER_LEFT);

        orientationLabel = LanLobbyLayout.text("", "lan-description");
        updateOrientationLabel();
        VBox weaponsBox = LanLobbyLayout.card(launcherBar, orientationLabel);
        weaponsBox.getStyleClass().add("battle-weapons");
        Label help = LanLobbyLayout.text("Select a weapon, then click enemy waters. 60 seconds per turn.", "lan-description");
        ownFleetLabel = LanLobbyLayout.text("", "lan-status");
        enemyFleetLabel = LanLobbyLayout.text("", "lan-status");
        VBox ownBox = buildBoardCard("YOUR FLEET", ownGrid, ownFleetLabel);
        VBox enemyBox = buildBoardCard("ENEMY WATERS", enemyGrid, enemyFleetLabel);
        HBox boards = new HBox(18, ownBox, enemyBox);
        boards.setAlignment(Pos.TOP_CENTER);

        fleetStatusLabel = LanLobbyLayout.text("", "lan-description");
        logLabel = LanLobbyLayout.text("Waiting for the first shot.", "lan-status");
        com.battleship.view.battle.BattleLog history = new com.battleship.view.battle.BattleLog();
        logLabel.textProperty().addListener((obs, old, value) -> history.add(value, "info"));
        VBox side = LanLobbyLayout.card(
                LanLobbyLayout.text("MATCH OVERVIEW", "lan-eyebrow"), fleetStatusLabel,
                new javafx.scene.control.Separator(),
                LanLobbyLayout.text("NUCLEAR STRIKES", "lan-field-label"),
                LanLobbyLayout.text("3 per player, per match. No refills. Launch authorization uses your turn time.", "lan-description"),
                new javafx.scene.control.Separator(),
                LanLobbyLayout.text("LATEST ACTION", "lan-eyebrow"), logLabel, history.node(),
                LanLobbyLayout.text("• Miss    ◆ Hit    × Sunk", "lan-description"));
        side.setPrefWidth(230);
        side.setMinWidth(230);
        side.setMaxWidth(230);
        javafx.scene.control.ScrollPane sideScroll = new javafx.scene.control.ScrollPane(side);
        sideScroll.setFitToWidth(true);
        sideScroll.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        sideScroll.getStyleClass().add("lan-scroll");
        sideScroll.setMinWidth(250);
        sideScroll.setPrefWidth(250);
        sideScroll.setMaxWidth(250);
        sideScroll.prefHeightProperty().bind(ownBox.heightProperty());
        sideScroll.maxHeightProperty().bind(ownBox.heightProperty());
        HBox content = new HBox(20, boards, sideScroll);
        content.setAlignment(Pos.TOP_CENTER);
        VBox layout = new VBox(18, header, help, weaponsBox, content);
        layout.setPadding(new Insets(24));
        layout.setMaxWidth(1500);
        layout.setAlignment(Pos.TOP_CENTER);
        refreshFleetStatus();
        return layout;
    }

    private VBox buildBoardCard(String title, BoardGridPane grid, Label fleet) {
        HBox heading = new HBox(12, LanLobbyLayout.text(title, "lan-card-title"), fleet);
        heading.setAlignment(Pos.CENTER_LEFT);
        VBox card = new VBox(14, heading, grid.withCoordinates());
        card.getStyleClass().add("battle-board-card");
        card.setMaxHeight(Region.USE_PREF_SIZE);
        return card;
    }
    @Override
    protected StackPane decorateRoot(Pane layout) {
        StackPane root = new StackPane();
        root.getStyleClass().add("battle-screen");
        root.getChildren().add(layout);

        root.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.widthProperty().addListener((o, oldW, newW) -> adjustGridSizes(newW.doubleValue(), newScene.getHeight()));
                newScene.heightProperty().addListener((o, oldH, newH) -> adjustGridSizes(newScene.getWidth(), newH.doubleValue()));
                adjustGridSizes(newScene.getWidth(), newScene.getHeight());
            }
        });

        return root;
    }

    private void adjustGridSizes(double width, double height) {
        if (width <= 0 || height <= 0 || ownGrid == null || enemyGrid == null) return;
        int size = ownGrid.getSize();

        // vertical budget: window minus title (~55), weaponsbox (~65), statusbox (~65),
        // card chrome (title ~30, padding ~32, spacing ~12), vbox gaps (10*3),
        // layout padding (28) ≈ 320px total overhead.
        double availH = height - 360;
        // horizontal budget per board: window minus padding (40), gap (28),
        // card padding (32 each = 64) ≈ 132px total overhead.
        double availW = (Math.min(width, 1500) - 460) / 2.0;

        double maxGridPx = Math.min(availW, availH);
        maxGridPx = Math.max(200.0, Math.min(maxGridPx, 520.0));

        double newCellPx = Math.floor(maxGridPx / size);
        // allow cells to grow up to 120px so 5x5 boards on fullscreen are prominent and fill space
        newCellPx = Math.min(newCellPx, 86.0);
        ownGrid.setCellSize(newCellPx);
        enemyGrid.setCellSize(newCellPx);
    }

    @Override
    protected void onViewShown() {
        startTurnClock();
        if (nav.getStage() != null && nav.getStage().getScene() != null) {
            adjustGridSizes(nav.getStage().getScene().getWidth(), nav.getStage().getScene().getHeight());
        }
        netSession.getSession().setOnMessage(this::handleMessage);
        netSession.getSession().setOnDisconnected(this::handleDisconnect);
        enemyGrid.setDisable(!netSession.isMyTurn());
    }

    // ---------- networking ----------

    private void handleMessage(NetMessage msg) {
        if (msg == null) return;
        switch (msg) {
            case NetMessage.Fire f        -> handleIncomingFire(f);
            case NetMessage.FireResult fr -> handleFireResult(fr);
            case NetMessage.FleetReveal reveal -> netSession.setRevealedEnemyFleet(reveal.ships());
            case NetMessage.TurnExpired expired -> {
                if (!gameOver && !waitingForResult && netSession.acceptRemoteTimeout(expired.turnNumber())) {
                    logLabel.setText("Opponent ran out of time. Your turn.");
                    activateMyTurn();
                }
            }
            default -> { /* lobby-phase messages ignored during battle */ }
        }
    }

    private boolean gameOver = false;

    private void handleDisconnect() {
        if (gameOver) return;
        stopBattle();
        netSession.getSession().close();
        AlertUtil.showWarning(nav.window(), "Disconnected", "Your opponent disconnected.");
        nav.showMainMenu();
    }

    /** i am the defender: delegate incoming fire resolution and response to mediator, then render. */
    private void handleIncomingFire(NetMessage.Fire fire) {
        if (gameOver || netSession.isMyTurn() || waitingForResult) return;
        turnClock.stop();
        NetworkBattleMediator.IncomingFireOutcome outcome = mediator.resolveAndReply(fire);

        for (ShotResult r : outcome.resolution().results()) {
            if (r.outcome() != CellStatus.SUNK) ownGrid.renderShot(r.coordinate(), r.outcome());
        }
        for (ShipSnapshot s : outcome.resolution().sunkShips()) ownGrid.renderSunkShip(s);

        refreshFleetStatus();

        if (outcome.lost()) {
            if (netSession.getSession() != null) {
                netSession.getSession().send(new NetMessage.FleetReveal(netSession.getMe().fleet()));
            }
            goToGameOver(false);
            return;
        }

        playResultAudio(outcome.anyHit(), outcome.anySunk());
        logLabel.setText(outcome.anyHit() ? "Incoming fire \u2014 you took damage!" : "Incoming fire \u2014 they missed.");
        netSession.beginMyTurn();
        netSession.advanceTurn();
        activateMyTurn();
    }

    private void activateMyTurn() {
        startTurnClock();
        audio.playTurnStart();
        turnLabel.setText("YOUR TURN");
        enemyGrid.setDisable(false);
        // Re-enable weapon selection for the player who was waiting, including quiz-gated nuclear fire.
        refreshLauncherBar();
    }

    /** i am the attacker: apply the result the defender reported for my shot. */
    private void handleFireResult(NetMessage.FireResult result) {
        if (gameOver || !waitingForResult) return;
        waitingForResult = false;
        mediator.recordObservedResult(result);
        boolean anyHit = applyCellResults(result.results());
        String sunkLog = applySunkShips(result.sunkShips());

        logLabel.setText(sunkLog.isEmpty()
                ? (anyHit ? "Direct hit!" : "Nothing but spray \u2014 miss.")
                : sunkLog.trim());
        playResultAudio(anyHit, !sunkLog.isEmpty());
        refreshFleetStatus();

        if (result.defenderLost()) {
            if (netSession.getSession() != null) {
                netSession.getSession().send(new NetMessage.FleetReveal(netSession.getMe().fleet()));
            }
            goToGameOver(true);
            return;
        }
        handTurnToOpponent();
    }

    /**
     * records and renders every cell the defender reported.
     * @return {@code true} if any reported cell was a hit or part of a sunk ship
     */
    private boolean applyCellResults(List<NetMessage.CellResult> results) {
        boolean anyHit = false;
        for (NetMessage.CellResult cr : results) {
            Coordinate c = cr.coordinate();
            CellStatus status = cr.outcome();
            if (status == CellStatus.HIT) {
                enemyGrid.renderShot(c, CellStatus.HIT);
                anyHit = true;
            } else if (status == CellStatus.MISS) {
                enemyGrid.renderShot(c, CellStatus.MISS);
            } else if (status == CellStatus.SUNK) {
                anyHit = true; // cell rendering handled via the sunkships list below
            }
        }
        return anyHit;
    }

    /**
     * records and renders every ship reported sunk.
     * @return the attack-log fragment for the sunk ships, or an empty string if none
     */
    private String applySunkShips(List<NetMessage.SunkShipInfo> sunkShips) {
        if (sunkShips == null || sunkShips.isEmpty()) return "";
        StringBuilder log = new StringBuilder();
        for (NetMessage.SunkShipInfo si : sunkShips) {
            Orientation orientation = (si.cells().size() >= 2 && si.cells().get(0).getRow() != si.cells().get(1).getRow())
                    ? Orientation.VERTICAL : Orientation.HORIZONTAL;
            enemyGrid.renderSunkShip(si.shipType(), orientation, si.cells(), true);
            log.append(si.shipType().name().replace('_', ' ')).append(" has been sent to the bottom! ");
        }
        return log.toString();
    }

    /** my shot is resolved — hand the turn back to the opponent. */
    private void handTurnToOpponent() {
        netSession.advanceTurn();
        netSession.beginOpponentTurn();
        startTurnClock();
        turnLabel.setText("OPPONENT'S TURN");
        enemyGrid.setDisable(true);
        refreshLauncherBar();
    }

    private void goToGameOver(boolean won) {
        gameOver = true;
        stopBattle();
        nav.showNetworkGameOver(netSession, won);
    }

    // ---------- shot resolution (network) ----------

    @Override
    protected void resolveShot(Coordinate anchor) {
        if (!canFireNow()) return;
        Weapon weapon = me.selectedWeapon();

        // all domain mutations (ammo consumption, launcher reset) live in
        // the controller-owned networkfireservice — the view only does ui + network i/o.
        NetworkFireService.NetworkShotOrder order =
                controller.fireNetworkShot(me, weapon, anchor, firingOrientation());

        waitingForResult = true;
        turnClock.waiting();
        netSession.getSession().send(new NetMessage.Fire(order.weapon().id(), order.anchor(), order.orientation()));
        audio.playFire();

        netSession.beginOpponentTurn();
        turnLabel.setText("AWAITING RESPONSE\u2026");
        enemyGrid.setDisable(true);
        refreshLauncherBar();
    }

    private void refreshFleetStatus() {
        int myTotal = me.fleet().size();
        long myLost = me.fleet().stream().filter(ShipSnapshot::isSunk).count();
        int enemySunkKnown = netSession.getEnemyKnowledge().confirmedSunk().size();
        int enemyTotal = controller.getSelectedTheater().getTotalShipCount();
        fleetStatusLabel.setText("Your ships lost: " + myLost + " / " + myTotal +
                "     Enemy ships confirmed sunk: " + enemySunkKnown + " / " + enemyTotal);
        if (ownFleetLabel != null) ownFleetLabel.setText((myTotal - myLost) + " / " + myTotal + " AFLOAT");
        if (enemyFleetLabel != null) enemyFleetLabel.setText(enemySunkKnown + " / " + enemyTotal + " SUNK");
    }

    private void updateOrientationLabel() {
        orientationLabel.setText(orientationLabelText());
    }

    @Override
    protected void onOrientationChanged() {
        updateOrientationLabel();
    }

    @Override protected void onTurnExpired() {
        // Only the active player advances a timed-out turn. The other clock is display-only.
        if (!canFireNow()) return;
        com.battleship.view.quiz.NuclearLaunchDialog.dismissActive();
        long expiredTurn = netSession.getTurnNumber();
        me.resetWeaponAfterShot();
        netSession.getSession().send(new NetMessage.TurnExpired(expiredTurn));
        logLabel.setText("Time expired. Your turn was skipped.");
        handTurnToOpponent();
    }
}
