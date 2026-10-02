package com.battleship.view;

import com.battleship.controller.GameController;
import com.battleship.net.NetMessage;
import com.battleship.net.NetworkGameSession;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.security.SecureRandom;

/**
 * ship placement for a network match. drives a ready/ready -&gt; (host decides)
 * start handshake over the socket instead of the local pass_screen flow.
 *
 * <p>extends {@link abstractshipplaceview}, so the dock, drag-and-drop, ghost
 * preview, orientation handling, counter and ready gating are all inherited.
 * this class contributes only the network chrome and the socket handshake.</p>
 */
public class NetworkShipPlaceView extends AbstractShipPlaceView {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final NetworkGameSession netSession;

    private Label statusLabel;
    private boolean localReady = false;
    private boolean opponentReady = false;

    public NetworkShipPlaceView(ViewNavigator nav, GameController controller, NetworkGameSession netSession) {
        super(nav, controller, netSession.getMe());
        this.netSession = netSession;
    }

    @Override
    protected Pane assembleLayout() {
        Label title = new Label("DEPLOY YOUR FLEET \u2014 " + player.getName());
        title.getStyleClass().addAll("app-title", "screen-title-md");

        dockPane.getStyleClass().add("card-panel");

        countLabel.getStyleClass().add("placement-count");

        statusLabel = new Label("Deploy your fleet, then hit READY.");
        statusLabel.getStyleClass().add("dim-text");
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(180);

        readyButton.setPrefWidth(160);
        readyButton.setPrefHeight(44);

        Label removeHint = new Label("Click a placed ship or drag it back to the dock.\nPress R while dragging to rotate. Esc cancels.");
        removeHint.setWrapText(true);
        removeHint.setMaxWidth(180);
        removeHint.getStyleClass().add("dim-text");

        VBox statusPanel = new VBox(14, countLabel, orientationLabel, removeHint, readyButton, statusLabel);
        statusPanel.setPadding(new Insets(14));
        statusPanel.setAlignment(Pos.TOP_CENTER);
        statusPanel.getStyleClass().add("card-panel");
        statusPanel.setPrefWidth(200);

        Button rotate = new Button("ROTATE SHIP");
        rotate.getStyleClass().add("ghost-button");
        rotate.setOnAction(e -> {
            audio.playClick();
            toggleOrientation();
        });

        Button autoPlace = new Button("AUTO PLACE");
        autoPlace.getStyleClass().add("ghost-button");
        autoPlace.setOnAction(e -> {
            if (isReadyLocked()) return;
            audio.playClick();
            audio.playPlaceShip();
            controller.autoPlaceRemaining(player);
            refreshAll();
        });

        Button reset = new Button("RESET");
        reset.getStyleClass().add("ghost-button");
        reset.setOnAction(e -> {
            if (isReadyLocked()) return;
            audio.playClick();
            controller.resetPlacement(player);
            refreshAll();
        });

        HBox bottomBar = new HBox(16, rotate, autoPlace, reset);
        bottomBar.setAlignment(Pos.CENTER);

        Button exit = buildExitButton();

        HBox center = new HBox(24, dockPane, boardGridPane, statusPanel);
        center.setAlignment(Pos.CENTER);
        center.setMaxWidth(Region.USE_PREF_SIZE);

        HBox topBar = new HBox(title);
        topBar.setAlignment(Pos.CENTER);
        StackPane titleRow = new StackPane(topBar, exit);
        StackPane.setAlignment(exit, Pos.CENTER_RIGHT);
        titleRow.setMaxWidth(Region.USE_PREF_SIZE);
        center.widthProperty().addListener((obs, oldW, newW) -> {
            if (newW.doubleValue() > 0) {
                titleRow.setPrefWidth(newW.doubleValue());
                titleRow.setMaxWidth(newW.doubleValue());
            }
        });

        VBox layout = new VBox(20, titleRow, center, bottomBar);
        layout.setAlignment(Pos.CENTER);
        layout.setFillWidth(false);
        layout.setPadding(new Insets(24));
        return layout;
    }

    @Override
    protected StackPane decorateRoot(Pane layout) {
        return new StackPane(layout); // no ocean backdrop on the network screen
    }

    /** once ready has been sent the button must stay disabled even if the fleet changes. */
    @Override
    protected boolean isReadyLocked() {
        return localReady;
    }

    @Override
    protected void onViewShown() {
        netSession.getSession().setOnMessage(this::handleMessage);
        netSession.getSession().setOnDisconnected(this::handleDisconnect);
    }

    // ---------- network flow: ready handshake + exit ----------

    @Override
    protected void onReadyPressed() {
        onReadyClicked();
    }

    @Override
    protected String exitPrompt() {
        return "Leave this match and return to the main menu? This will disconnect your opponent.";
    }

    @Override
    protected void onExitConfirmed() {
        netSession.getSession().close();
    }

    // ---------- networking ----------

    private void handleMessage(NetMessage msg) {
        if (msg == null) return;
        if (msg instanceof NetMessage.Ready) {
            opponentReady = true;
            statusLabel.setText(localReady
                    ? "Both fleets deployed \u2014 starting battle\u2026"
                    : "Opponent is ready. Deploy your fleet!");
            maybeStartAsHost();
        } else if (msg instanceof NetMessage.Start start) {
            // only the client ever receives this (the host sets its own turn locally
            // in maybestartashost right before sending start).
            netSession.beginMatch("HOST".equals(start.firstPlayer()));
            goToBattle();
        }
    }

    private boolean transitionedToBattle = false;

    private void handleDisconnect() {
        if (transitionedToBattle) return;
        statusLabel.setText("Connection lost.");
        AlertUtil.showWarning(nav.window(), "Disconnected", "Your opponent disconnected.");
        nav.showMainMenu();
    }

    private void onReadyClicked() {
        if (localReady || !controller.isPlacementComplete(player)) return;
        localReady = true;
        boardGridPane.setDisable(true);
        dockPane.setDisable(true);
        readyButton.setDisable(true);
        statusLabel.setText(opponentReady
                ? "Both fleets deployed \u2014 starting battle\u2026"
                : "Waiting for opponent to finish deploying\u2026");
        netSession.getSession().send(new NetMessage.Ready());
        maybeStartAsHost();
    }

    private void maybeStartAsHost() {
        if (netSession.isHost() && localReady && opponentReady) {
            boolean hostFirst = RANDOM.nextBoolean();
            netSession.beginMatch(hostFirst);
            netSession.getSession().send(new NetMessage.Start(hostFirst ? "HOST" : "CLIENT"));
            goToBattle();
        }
    }

    private void goToBattle() {
        transitionedToBattle = true;
        if (netSession.getSession() != null) {
            netSession.getSession().setOnDisconnected(null);
            netSession.getSession().setOnMessage(null);
        }
        nav.showNetworkBattle(netSession);
    }
}
