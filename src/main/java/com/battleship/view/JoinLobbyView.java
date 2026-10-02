package com.battleship.view;

import com.battleship.controller.GameController;
import com.battleship.model.*;
import com.battleship.net.*;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

/** Guided LAN form with inline validation and cancellable connection attempts. */
public class JoinLobbyView {
    private final ViewNavigator nav;
    private final GameController controller;
    private final TextField address = input("e.g. 192.168.1.23");
    private final TextField port = input("e.g. 55123");
    private final TextField code = input("4 digits");
    private final TextField invite = input("Paste the invite your friend copied");
    private final Label status = LanLobbyLayout.text("Enter the details shown on your friend's host screen.", "lan-status");
    private final Button connect = new Button("Connect to game");
    private final ProgressIndicator progress = new ProgressIndicator();
    private final PauseTransition timeout = new PauseTransition(Duration.seconds(12));
    private boolean connecting;
    private boolean closed;
    private long attemptId;
    private NetworkSession pendingSession;

    public JoinLobbyView(ViewNavigator nav, GameController controller) {
        this.nav = nav;
        this.controller = controller;
    }

    private static TextField input(String hint) {
        TextField field = new TextField();
        field.setPromptText(hint);
        field.getStyleClass().add("lan-input");
        return field;
    }

    public StackPane build() {
        controller.setMode(GameMode.ONLINE);
        Button useInvite = new Button("Fill details");
        useInvite.getStyleClass().add("lan-secondary");
        useInvite.setOnAction(e -> fillInvite());
        invite.setOnAction(e -> fillInvite());
        invite.textProperty().addListener((obs, old, value) -> {
            try { fillFields(LanInvite.parse(value)); } catch (IllegalArgumentException ignored) { }
        });
        HBox pasteRow = new HBox(10, invite, useInvite);
        HBox.setHgrow(invite, Priority.ALWAYS);
        useInvite.setMinWidth(110);
        VBox portField = LanLobbyLayout.field("Port", port);
        VBox codeField = LanLobbyLayout.field("Join code", code);
        HBox shortFields = new HBox(16, portField, codeField);
        HBox.setHgrow(portField, Priority.ALWAYS);
        HBox.setHgrow(codeField, Priority.ALWAYS);
        portField.setMaxWidth(Double.MAX_VALUE);
        codeField.setMaxWidth(Double.MAX_VALUE);
        for (TextField field : new TextField[]{address, port, code}) {
            field.setOnAction(e -> attemptConnect());
            field.textProperty().addListener((obs, old, value) ->
                    field.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("invalid"), false));
        }
        progress.setMaxSize(20, 20);
        progress.setVisible(false);
        progress.setManaged(false);
        connect.getStyleClass().add("lan-primary");
        connect.setMaxWidth(Double.MAX_VALUE);
        connect.setDefaultButton(true);
        connect.setOnAction(e -> attemptConnect());
        HBox connectionStatus = new HBox(10, progress, status);
        HBox.setHgrow(status, Priority.ALWAYS);
        VBox form = LanLobbyLayout.card(
                LanLobbyLayout.text("Join your friend's fleet", "lan-card-title"),
                LanLobbyLayout.field("Quick join · paste an invite", pasteRow), new Separator(),
                LanLobbyLayout.text("Or enter the details manually", "lan-description"),
                LanLobbyLayout.field("Host IP address", address), shortFields, connect, connectionStatus);
        form.setPrefWidth(510);
        VBox help = LanLobbyLayout.card(
                LanLobbyLayout.text("Before you connect", "lan-card-title"),
                LanLobbyLayout.text("01  Join the same network", "lan-field-label"),
                LanLobbyLayout.text("Both computers need the same Wi-Fi or wired LAN. An internet connection alone isn't enough.", "lan-description"),
                LanLobbyLayout.text("02  Ask your friend to host", "lan-field-label"),
                LanLobbyLayout.text("Keep their host screen open. Use its IP address, port and 4-digit code.", "lan-description"),
                LanLobbyLayout.text("03  Ready up together", "lan-field-label"),
                LanLobbyLayout.text("Meet in the shared lobby. Both players press Ready, then deploy their fleets.", "lan-description"), new Separator(),
                LanLobbyLayout.text("Can't connect?", "lan-field-label"),
                LanLobbyLayout.text("Check the host's Wi-Fi / Ethernet address. Allow Java through the host's private-network firewall. Guest Wi-Fi can block other devices.", "lan-description"));
        help.setPrefWidth(280);
        HBox columns = new HBox(20, form, help);
        HBox.setHgrow(form, Priority.ALWAYS);
        Button back = new Button("← Back to multiplayer");
        back.getStyleClass().add("lan-secondary");
        back.setOnAction(e -> { closed = true; cancelAttempt(); nav.showMultiplayerLobby(); });
        useInvite.disableProperty().bind(connect.disableProperty());
        return LanLobbyLayout.page("Join a game", "Your friend hosts. You bring the fleet.", columns, back);
    }

    private void fillInvite() {
        try { fillFields(LanInvite.parse(invite.getText())); }
        catch (IllegalArgumentException error) { showError(error.getMessage()); invite.requestFocus(); }
    }

    private void fillFields(LanInvite details) {
        if (connecting || closed) return;
        address.setText(details.address());
        port.setText(Integer.toString(details.port()));
        code.setText(details.code());
        status.getStyleClass().remove("lan-error");
        status.setText("Invite filled in. Select Connect to game when you're ready.");
    }

    private void attemptConnect() {
        if (connecting || closed) return;
        LanInvite details;
        try { details = LanInvite.fromFields(address.getText(), port.getText(), code.getText()); }
        catch (IllegalArgumentException error) {
            showError(error.getMessage());
            TextField field = error.getMessage().contains("port") ? port
                    : error.getMessage().contains("code") ? code : address;
            field.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("invalid"), true);
            field.requestFocus();
            return;
        }
        setBusy(true);
        status.getStyleClass().remove("lan-error");
        status.setText("Connecting to " + details.address() + ":" + details.port() + "…");
        long id = ++attemptId;
        timeout.setOnFinished(e -> {
            if (closed || id != attemptId) return;
            cancelAttempt();
            showError("The host didn't respond. Check the address and network, then try again.");
        });
        timeout.playFromStart();
        pendingSession = NetworkSession.connect(details.address(), details.port(), session -> {
            if (closed || id != attemptId) { session.close(); return; }
            status.setText("Host found. Checking your join code…");
            session.setOnMessage(msg -> {
                if (closed || id != attemptId) return;
                if (msg instanceof NetMessage.Reject) {
                    cancelAttempt();
                    showError("That code didn't match. Ask your friend for the current 4-digit code.");
                    code.requestFocus();
                } else if (msg instanceof NetMessage.Welcome welcome) {
                    try {
                        Theater theater = Theater.valueOf(welcome.theater());
                        timeout.stop();
                        closed = true;
                        controller.setTheater(theater);
                        NetworkGameSession match = new NetworkGameSession(session, theater, Role.CLIENT,
                                new HumanPlayer("You", theater));
                        nav.setScreen(new NetworkMatchLobbyView(nav, match).build());
                    } catch (IllegalArgumentException error) {
                        cancelAttempt();
                        showError("This host uses an incompatible game version.");
                    }
                }
            });
            session.setOnDisconnected(() -> {
                if (closed || id != attemptId) return;
                cancelAttempt();
                showError("The host closed the connection. Ask your friend to host again.");
            });
            session.send(new NetMessage.Hello(details.code()));
        }, error -> {
            if (closed || id != attemptId) return;
            cancelAttempt();
            showError("Couldn't reach the host. Check the IP and port, keep the host lobby open, and check the network tips.");
        }, Platform::runLater);
    }

    private void cancelAttempt() {
        ++attemptId;
        timeout.stop();
        if (pendingSession != null) pendingSession.close();
        pendingSession = null;
        setBusy(false);
    }

    private void setBusy(boolean busy) {
        connecting = busy;
        connect.setDisable(busy);
        connect.setText(busy ? "Connecting…" : "Connect to game");
        for (TextField field : new TextField[]{address, port, code, invite}) field.setDisable(busy);
        progress.setVisible(busy);
        progress.setManaged(busy);
    }

    private void showError(String message) {
        status.setText(message);
        if (!status.getStyleClass().contains("lan-error")) status.getStyleClass().add("lan-error");
    }
}
