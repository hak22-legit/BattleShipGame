package com.battleship.view;

import com.battleship.model.HumanPlayer;
import com.battleship.model.Theater;
import com.battleship.net.*;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.lang.reflect.Proxy;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import static org.junit.jupiter.api.Assertions.*;

class NetworkMatchLobbyViewTest {
    @BeforeAll static void startToolkit() throws Exception { FxTestSupport.startToolkit(); }

    @ParameterizedTest @EnumSource(Role.class)
    void bothMustBeReadyAndEitherPlayerMayConfirmFirst(Role first) throws Exception {
        try (Pair pair = new Pair()) {
            Peer firstPeer = first == Role.HOST ? pair.host : pair.guest;
            Peer secondPeer = first == Role.HOST ? pair.guest : pair.host;
            FxTestSupport.onFxThread(() -> {
                assertEquals("0 / 2 players ready", pair.host.text("lobby-ready-count"));
                assertEquals("0 / 2 players ready", pair.guest.text("lobby-ready-count"));
                firstPeer.ready().fire();
                assertTrue(firstPeer.ready().isDisabled());
            });
            awaitFx(() -> "1 / 2 players ready".equals(secondPeer.text("lobby-ready-count")));
            FxTestSupport.onFxThread(() -> {
                assertEquals(0, pair.host.transitions.get());
                assertEquals(0, pair.guest.transitions.get());
                assertFalse(secondPeer.ready().isDisabled());
                String readyId = first == Role.HOST ? "host-ready-status" : "guest-ready-status";
                assertEquals("✓ Ready", pair.host.text(readyId));
                assertEquals("✓ Ready", pair.guest.text(readyId));
                secondPeer.ready().fire();
            });
            pair.host.started.get(5, TimeUnit.SECONDS);
            pair.guest.started.get(5, TimeUnit.SECONDS);
            FxTestSupport.onFxThread(() -> {
                assertEquals(1, pair.host.transitions.get());
                assertEquals(1, pair.guest.transitions.get());
            });
        }
    }

    @Test void disconnectWhileWaitingPreventsStart() throws Exception {
        try (Pair pair = new Pair()) {
            FxTestSupport.onFxThread(() -> pair.host.ready().fire());
            pair.guest.transport.close();
            awaitFx(() -> "Disconnected".equals(pair.host.text("guest-ready-status")));
            FxTestSupport.onFxThread(() -> {
                assertTrue(pair.host.ready().isDisabled());
                assertTrue(pair.host.text("lobby-status").contains("connection was lost"));
                assertEquals(0, pair.host.transitions.get());
            });
        }
    }

    @Test void deploymentCannotStartBeforeReadiness() throws Exception {
        try (Pair pair = new Pair()) {
            pair.host.transport.send(new NetMessage.BeginDeployment());
            pair.host.transport.send(new NetMessage.LobbyReady());
            // Seeing the later message proves the early start message was already handled.
            awaitFx(() -> "1 / 2 players ready".equals(pair.guest.text("lobby-ready-count")));
            assertEquals(0, pair.guest.transitions.get());
        }
    }

    private static void awaitFx(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            boolean[] ready = {false};
            FxTestSupport.onFxThread(() -> ready[0] = condition.getAsBoolean());
            if (ready[0]) return;
            Thread.sleep(10);
        }
        fail("Timed out waiting for lobby synchronization");
    }

    private static class Peer {
        final NetworkSession transport;
        final StackPane root;
        final AtomicInteger transitions = new AtomicInteger();
        final CompletableFuture<Void> started = new CompletableFuture<>();
        Peer(NetworkSession transport, Role role) {
            this.transport = transport;
            ViewNavigator nav = (ViewNavigator) Proxy.newProxyInstance(ViewNavigator.class.getClassLoader(),
                    new Class<?>[]{ViewNavigator.class}, (proxy, method, args) -> {
                        if (method.getName().equals("showNetworkShipPlacement")) {
                            transitions.incrementAndGet();
                            started.complete(null);
                        }
                        return method.getName().equals("getAudio") ? SilentAudio.INSTANCE : null;
                    });
            NetworkGameSession match = new NetworkGameSession(transport, Theater.SKIRMISH, role,
                    new HumanPlayer("Player", Theater.SKIRMISH));
            root = new NetworkMatchLobbyView(nav, match).build();
            new Scene(root, 1160, 740);
            root.applyCss(); root.layout();
        }
        Button ready() { return (Button) root.lookup("#lobby-ready-button"); }
        String text(String id) { return ((Label) root.lookup("#" + id)).getText(); }
    }

    private static class Pair implements AutoCloseable {
        final Peer host;
        final Peer guest;
        Pair() throws Exception {
            CompletableFuture<Integer> port = new CompletableFuture<>();
            CompletableFuture<Peer> hostReady = new CompletableFuture<>();
            CompletableFuture<Peer> guestReady = new CompletableFuture<>();
            NetworkSession hosting = NetworkSession.host(0, port::complete,
                    session -> hostReady.complete(new Peer(session, Role.HOST)),
                    error -> { port.completeExceptionally(error); hostReady.completeExceptionally(error); }, Platform::runLater);
            NetworkSession joining = null;
            try {
                joining = NetworkSession.connect("127.0.0.1", port.get(5, TimeUnit.SECONDS),
                        session -> guestReady.complete(new Peer(session, Role.CLIENT)),
                        guestReady::completeExceptionally, Platform::runLater);
                host = hostReady.get(5, TimeUnit.SECONDS);
                guest = guestReady.get(5, TimeUnit.SECONDS);
            } catch (Exception error) {
                hosting.close();
                if (joining != null) joining.close();
                throw error;
            }
        }
        @Override public void close() { host.transport.close(); guest.transport.close(); }
    }
}
