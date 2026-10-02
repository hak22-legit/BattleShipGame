package com.battleship.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * thin tcp transport for a single host&lt;-&gt;client connection: one json
 * {@link netmessage} per line. connection setup and the blocking read loop run
 * on a background daemon thread; every callback is marshalled through the
 * caller-supplied {@link executor} (fixes f6 + dip).
 *
 * <p>fixes the dip audit finding: this class used to
 * {@code import javafx.application.platform} and default to
 * {@code platform::runlater}, which welded the transport layer to the javafx
 * toolkit — the class would not even link on a headless server, a cli client or
 * a test without javafx on the classpath. the dispatcher is now a plain
 * {@link java.util.concurrent.executor} injected by the caller: the javafx views
 * pass {@code platform::runlater}, tests pass {@code runnable::run}.</p>
 */
public class NetworkSession {

    private final NetMessageCodec codec = new NetMessageCodec();
    private final Executor dispatcher;

    private Socket socket;
    private ServerSocket serverSocket;
    private BufferedReader in;
    private PrintWriter out;
    private volatile boolean running = true;
    private volatile boolean locallyClosed;

    private Consumer<NetMessage> onMessage;
    private Runnable onDisconnected;

    private NetworkSession(Executor dispatcher) {
        this.dispatcher = Objects.requireNonNull(dispatcher, "A thread dispatcher is required.");
    }

    private void dispatch(Runnable action) {
        dispatcher.execute(action);
    }

    /** opens a listening socket on {@code port} and waits for exactly one peer to connect. */
    public static NetworkSession host(int port, Consumer<NetworkSession> onClientConnected,
                                      Consumer<Exception> onError, Executor dispatcher) {
        return host(port, boundPort -> { }, onClientConnected, onError, dispatcher);
    }

    /** Port zero lets the OS reserve a port; announce it only after binding succeeds. */
    public static NetworkSession host(int port, Consumer<Integer> onListening,
                                      Consumer<NetworkSession> onClientConnected,
                                      Consumer<Exception> onError, Executor dispatcher) {
        NetworkSession session = new NetworkSession(dispatcher);
        Thread t = new Thread(() -> {
            try {
                synchronized (session) {
                    if (!session.running) return;
                    session.serverSocket = new ServerSocket(port);
                }
                int boundPort = session.serverSocket.getLocalPort();
                session.dispatch(() -> { if (session.running) onListening.accept(boundPort); });
                Socket client = session.serverSocket.accept();
                session.attach(client);
                session.dispatch(() -> { if (!session.locallyClosed) onClientConnected.accept(session); });
                session.listenLoop();
            } catch (Exception ex) {
                boolean report = session.running;
                session.close();
                if (report) session.dispatch(() -> onError.accept(ex));
            }
        }, "battleship-net-host");
        t.setDaemon(true);
        t.start();
        return session;
    }

    /** connects out to a host's ip/port, dispatching callbacks through the supplied executor. */
    public static NetworkSession connect(String host, int port, Consumer<NetworkSession> onConnected,
                               Consumer<Exception> onError, Executor dispatcher) {
        NetworkSession session = new NetworkSession(dispatcher);
        Thread t = new Thread(() -> {
            try {
                Socket socket = new Socket();
                synchronized (session) {
                    if (!session.running) { socket.close(); return; }
                    session.socket = socket;
                }
                socket.connect(new InetSocketAddress(host, port), 8000);
                session.attach(socket);
                session.dispatch(() -> { if (!session.locallyClosed) onConnected.accept(session); });
                session.listenLoop();
            } catch (Exception ex) {
                boolean report = session.running;
                session.close();
                if (report) session.dispatch(() -> onError.accept(ex));
            }
        }, "battleship-net-client");
        t.setDaemon(true);
        t.start();
        return session;
    }

    private synchronized void attach(Socket socket) throws IOException {
        if (!running) { socket.close(); throw new IOException("Connection cancelled"); }
        this.socket = socket;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
    }

    private void listenLoop() {
        try {
            String line;
            while (running && (line = in.readLine()) != null) {
                NetMessage msg = codec.decode(line);
                // Resolve the handler on the UI queue, after the connection callback
                // has installed it (and after any preceding screen transition).
                dispatch(() -> {
                    if (!locallyClosed && onMessage != null) onMessage.accept(msg);
                });
            }
        } catch (IOException ignored) {
            // socket closed locally, or connection dropped by the peer
        } finally {
            boolean peerDrop = running;
            running = false;
            if (peerDrop) dispatch(() -> {
                if (!locallyClosed && onDisconnected != null) onDisconnected.run();
            });
        }
    }

    public void send(NetMessage msg) {
        if (out != null) out.println(codec.encode(msg));
    }

    public void setOnMessage(Consumer<NetMessage> onMessage) { this.onMessage = onMessage; }
    public void setOnDisconnected(Runnable onDisconnected) { this.onDisconnected = onDisconnected; }

    public synchronized void close() {
        locallyClosed = true;
        running = false;
        try { if (socket != null) socket.close(); } catch (IOException ignored) { }
        try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignored) { }
    }
}
