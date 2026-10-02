package com.battleship.view;

import com.battleship.controller.GameController;
import com.battleship.model.*;
import com.battleship.net.*;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.util.Duration;
import java.security.SecureRandom;

/** Host lobby: select a LAN adapter, copy the invite, and wait for a peer. */
public class HostLobbyView {
    private final ViewNavigator nav;
    private final Theater theater;
    private final String code = String.format(java.util.Locale.ROOT, "%04d", new SecureRandom().nextInt(10000));
    private int port;
    private NetworkSession pendingSession;
    private boolean closed;
    private final ComboBox<NetUtil.LanAddress> addresses = new ComboBox<>();
    private final TextField invite = new TextField();
    private final Label status = LanLobbyLayout.text("Opening your lobby…", "lan-status");
    private final Label warning = LanLobbyLayout.text("", "lan-warning");
    private final Label ipValue = LanLobbyLayout.text("—", "lan-detail-value");
    private final Label portValue = LanLobbyLayout.text("—", "lan-detail-value");
    private final ImageView qr = new ImageView();
    private final Button copy = new Button("Copy invite");
    private final Button retry = new Button("Retry hosting");
    private final PauseTransition handshakeTimeout = new PauseTransition(Duration.seconds(12));

    public HostLobbyView(ViewNavigator nav, GameController controller, Theater theater) {
        this.nav = nav;
        this.theater = theater;
        controller.setMode(GameMode.ONLINE);
        controller.setTheater(theater);
    }

    public StackPane build() {
        addresses.getItems().setAll(NetUtil.getLocalAddresses());
        addresses.getSelectionModel().selectFirst();
        addresses.setMaxWidth(Double.MAX_VALUE);
        addresses.getStyleClass().add("lan-input");
        addresses.valueProperty().addListener((obs, old, value) -> updateInvite());
        invite.setEditable(false);
        invite.getStyleClass().add("lan-input");
        invite.setPromptText("Your invite will appear when the lobby is ready");
        copy.getStyleClass().add("lan-primary");
        copy.setMaxWidth(Double.MAX_VALUE);
        copy.setDisable(true);
        copy.setOnAction(e -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(invite.getText());
            copy.setText(Clipboard.getSystemClipboard().setContent(content) ? "Invite copied ✓" : "Select and copy the invite above");
        });
        HBox details = new HBox(28, LanLobbyLayout.field("Host IP", ipValue),
                LanLobbyLayout.field("Port", portValue),
                LanLobbyLayout.field("Join code", LanLobbyLayout.text(code, "lan-detail-value")));
        VBox form = LanLobbyLayout.card(
                LanLobbyLayout.text("1. Choose your network", "lan-card-title"),
                LanLobbyLayout.field("Wi-Fi / Ethernet adapter", addresses), warning,
                new Separator(), LanLobbyLayout.text("2. Invite your friend", "lan-card-title"),
                LanLobbyLayout.text("Send this invite to your friend. On their computer, open Join a game and paste it.", "lan-description"),
                invite, copy, details, new Separator(), status, retry);
        form.setPrefWidth(540);
        qr.setFitWidth(172);
        qr.setFitHeight(172);
        qr.setPreserveRatio(true);
        StackPane qrFrame = new StackPane(qr);
        qrFrame.getStyleClass().add("lan-qr");
        qrFrame.setMaxSize(196, 196);
        VBox aside = LanLobbyLayout.card(
                LanLobbyLayout.text("Also share by QR", "lan-card-title"), qrFrame,
                LanLobbyLayout.text("Scan with a phone to read the invite. Enter it in the game on the other computer.", "lan-description"),
                new Separator(), LanLobbyLayout.text("Same network, two fleets", "lan-field-label"),
                LanLobbyLayout.text("Keep this screen open. When your friend joins, you both confirm Ready in the shared lobby.", "lan-description"),
                LanLobbyLayout.text("Use the same Wi-Fi or wired LAN. If blocked, allow Java on your private network in the host's firewall.", "lan-description"));
        aside.setPrefWidth(270);
        aside.setAlignment(Pos.TOP_LEFT);
        HBox columns = new HBox(20, form, aside);
        HBox.setHgrow(form, Priority.ALWAYS);
        Button back = new Button("← Close lobby and go back");
        back.getStyleClass().add("lan-secondary");
        back.setOnAction(e -> {
            closed = true;
            handshakeTimeout.stop();
            if (pendingSession != null) pendingSession.close();
            nav.showMultiplayerLobby();
        });
        retry.getStyleClass().add("lan-secondary");
        retry.setVisible(false);
        retry.setManaged(false);
        retry.setOnAction(e -> { port = 0; startHosting(); });
        updateInvite();
        startHosting();
        return LanLobbyLayout.page("Host a game", "Open a lobby. Invite a friend. Command your fleet.", columns, back);
    }

    private void updateInvite() {
        NetUtil.LanAddress selected = addresses.getValue();
        if (selected == null) return;
        String message = selected.ip().startsWith("127.") ? "No LAN address found. Connect to Wi-Fi or Ethernet and reopen this lobby."
                : selected.virtual() ? "This is a virtual / VPN adapter. Choose your Wi-Fi or Ethernet adapter for local play."
                : "Use the adapter connected to the same network as your friend.";
        warning.setText(message);
        ipValue.setText(selected.ip());
        if (port == 0) return;
        String text = new LanInvite(selected.ip(), port, code).encode();
        invite.setText(text);
        portValue.setText(Integer.toString(port));
        qr.setImage(QrCodeUtil.generate(text, 172));
        copy.setText("Copy invite");
    }

    private void startHosting() {
        if (closed) return;
        handshakeTimeout.stop();
        if (pendingSession != null) pendingSession.close();
        copy.setDisable(true);
        retry.setVisible(false);
        retry.setManaged(false);
        status.getStyleClass().remove("lan-error");
        status.setText("Opening your lobby…");
        pendingSession = NetworkSession.host(port, boundPort -> {
            if (closed) return;
            port = boundPort;
            updateInvite();
            copy.setDisable(false);
            status.setText("● Lobby ready · Waiting for your friend to join");
        }, session -> {
            if (closed) { session.close(); return; }
            status.setText("Friend found · Checking the join code…");
            session.setOnMessage(msg -> handleHandshake(session, msg));
            session.setOnDisconnected(() -> { if (!closed && session == pendingSession) startHosting(); });
            handshakeTimeout.setOnFinished(e -> { if (!closed) startHosting(); });
            handshakeTimeout.playFromStart();
        }, error -> {
            if (closed) return;
            status.setText("Couldn't open the lobby. Select Retry hosting to use a new port.");
            status.getStyleClass().add("lan-error");
            copy.setDisable(true);
            retry.setVisible(true);
            retry.setManaged(true);
        }, Platform::runLater);
    }

    private void handleHandshake(NetworkSession session, NetMessage msg) {
        if (closed || !(msg instanceof NetMessage.Hello hello)) return;
        handshakeTimeout.stop();
        if (!code.equals(hello.code())) {
            session.send(new NetMessage.Reject("Wrong join code"));
            startHosting();
            return;
        }
        closed = true;
        session.send(new NetMessage.Welcome(theater.name()));
        NetworkGameSession match = new NetworkGameSession(session, theater, Role.HOST,
                new HumanPlayer("You (Host)", theater));
        nav.setScreen(new NetworkMatchLobbyView(nav, match).build());
    }
}
