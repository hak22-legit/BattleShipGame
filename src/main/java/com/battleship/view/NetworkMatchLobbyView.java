package com.battleship.view;

import com.battleship.net.NetMessage;
import com.battleship.net.NetworkGameSession;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Shared lobby between joining the host and deploying the two fleets. */
public final class NetworkMatchLobbyView {
    private final ViewNavigator nav;
    private final NetworkGameSession match;
    private final Label hostStatus = LanLobbyLayout.text("Connected · Not ready", "lan-description");
    private final Label guestStatus = LanLobbyLayout.text("Connected · Not ready", "lan-description");
    private final Label count = LanLobbyLayout.text("0 / 2 players ready", "lan-card-title");
    private final Label status = LanLobbyLayout.text("Both players are connected. Press Ready when you want to start.", "lan-status");
    private final Button ready = new Button("Ready");
    private boolean localReady;
    private boolean remoteReady;
    private boolean finished;

    public NetworkMatchLobbyView(ViewNavigator nav, NetworkGameSession match) {
        this.nav = nav;
        this.match = match;
    }

    public StackPane build() {
        hostStatus.setId("host-ready-status");
        guestStatus.setId("guest-ready-status");
        status.setId("lobby-status");
        count.setId("lobby-ready-count");
        VBox host = playerCard("01", "Host" + (match.isHost() ? " (You)" : ""), hostStatus);
        VBox guest = playerCard("02", "Guest" + (!match.isHost() ? " (You)" : ""), guestStatus);
        HBox players = new HBox(18, host, guest);
        HBox.setHgrow(host, Priority.ALWAYS);
        HBox.setHgrow(guest, Priority.ALWAYS);
        host.setPrefWidth(400);
        guest.setPrefWidth(400);
        int size = match.getTheater().getBoardSize();
        String theater = match.getTheater().name().replace('_', ' ');
        Label settings = LanLobbyLayout.text(theater + "  ·  " + size + " × " + size
                + " board  ·  " + match.getTheater().getTotalShipCount() + " ships per player", "lan-description");
        ready.setId("lobby-ready-button");
        ready.getStyleClass().add("lan-primary");
        ready.setMaxWidth(Double.MAX_VALUE);
        ready.setOnAction(e -> confirmReady());
        VBox actions = LanLobbyLayout.card(count, status, ready,
                LanLobbyLayout.text("Next: deploy your ships. The battle begins automatically once both fleets are ready.", "lan-description"));
        Button leave = new Button("Leave lobby");
        leave.setId("leave-lobby-button");
        leave.getStyleClass().add("lan-secondary");
        leave.setOnAction(e -> {
            finished = true;
            match.getSession().close();
            nav.showMultiplayerLobby();
        });
        StackPane root = LanLobbyLayout.page("Your game lobby", "Ready up together before deploying your fleets.", new VBox(18, settings, players, actions), leave);
        match.getSession().setOnMessage(this::handleMessage);
        match.getSession().setOnDisconnected(this::handleDisconnect);
        return root;
    }

    private VBox playerCard(String number, String name, Label readiness) {
        VBox card = LanLobbyLayout.card(LanLobbyLayout.text("PLAYER " + number, "lan-eyebrow"),
                LanLobbyLayout.text(name, "lan-card-title"), readiness);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMinWidth(0);
        return card;
    }

    private void confirmReady() {
        if (finished || localReady) return;
        localReady = true;
        ready.setDisable(true);
        ready.setText("Ready · Waiting for the other player");
        match.getSession().send(new NetMessage.LobbyReady());
        refreshStatus();
        startAsHostIfReady();
    }

    private void handleMessage(NetMessage message) {
        if (finished) return;
        if (message instanceof NetMessage.LobbyReady) {
            remoteReady = true;
            refreshStatus();
            startAsHostIfReady();
        } else if (message instanceof NetMessage.BeginDeployment && !match.isHost() && localReady && remoteReady) {
            enterDeployment();
        }
    }

    private void refreshStatus() {
        updatePlayer(hostStatus, match.isHost() ? localReady : remoteReady);
        updatePlayer(guestStatus, match.isHost() ? remoteReady : localReady);
        count.setText(((localReady ? 1 : 0) + (remoteReady ? 1 : 0)) + " / 2 players ready");
        status.setText(localReady && remoteReady ? "Both players ready. Opening fleet deployment…"
                : localReady ? "You're ready. Waiting for your friend…"
                : remoteReady ? "Your friend is ready. Press Ready to start together."
                : "Both players are connected. Press Ready when you want to start.");
    }

    private void updatePlayer(Label label, boolean isReady) {
        label.setText(isReady ? "✓ Ready" : "Connected · Not ready");
        label.getStyleClass().remove("lan-status");
        if (isReady) label.getStyleClass().add("lan-status");
    }

    private void startAsHostIfReady() {
        if (match.isHost() && localReady && remoteReady && !finished) {
            match.getSession().send(new NetMessage.BeginDeployment());
            enterDeployment();
        }
    }

    private void enterDeployment() {
        if (finished) return;
        finished = true;
        // Navigation installs placement handlers synchronously on the same UI queue.
        nav.showNetworkShipPlacement(match);
    }

    private void handleDisconnect() {
        if (finished) return;
        finished = true;
        match.getSession().close();
        Label peer = match.isHost() ? guestStatus : hostStatus;
        peer.setText("Disconnected");
        peer.getStyleClass().remove("lan-status");
        peer.getStyleClass().add("lan-error");
        count.setText("1 / 2 players connected");
        status.setText("Your friend left or the connection was lost. Leave this lobby to reconnect.");
        status.getStyleClass().add("lan-error");
        ready.setDisable(true);
        ready.setText("Player disconnected");
    }
}
