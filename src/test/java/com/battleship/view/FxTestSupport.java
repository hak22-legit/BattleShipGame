package com.battleship.view;

import javafx.application.Platform;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class FxTestSupport {
    private FxTestSupport() { }
    public static void startToolkit() throws Exception {
        CompletableFuture<Void> started = new CompletableFuture<>();
        Runnable ready = () -> { Platform.setImplicitExit(false); started.complete(null); };
        try { Platform.startup(ready); }
        catch (IllegalStateException alreadyStarted) { Platform.runLater(ready); }
        started.get(10, TimeUnit.SECONDS);
    }
    public static void onFxThread(Runnable check) throws Exception {
        CompletableFuture<Void> checked = new CompletableFuture<>();
        Platform.runLater(() -> {
            try { check.run(); checked.complete(null); }
            catch (Throwable error) { checked.completeExceptionally(error); }
        });
        checked.get(10, TimeUnit.SECONDS);
    }
}
