package com.battleship.view;

/**
 * entry-point shim that makes the shaded fat jar runnable via a plain
 * {@code java -jar target/naval-command-1.0.0.jar} command.
 *
 * <p>the jdk launcher refuses to start a main class that extends
 * {@link javafx.application.application} unless the javafx runtime is present
 * as <em>modules</em> (on the module path). in a shaded fat jar the javafx
 * classes live on the classpath (unnamed module), so pointing {@code main-class}
 * at {@link mainapp} aborts with
 * "error: javafx runtime components are missing, and are required to run this
 * application" — before {@code main} is ever invoked. because this class does
 * <em>not</em> extend {@code application}, the launcher skips that module
 * check, and {@link mainapp}'s internal {@code launch()} call starts the
 * javafx toolkit from the classpath instead.</p>
 */
public final class Launcher {

    private Launcher() {
        // entry-point shim only — never instantiated.
    }

    /**
     * delegates straight to {@link mainapp#main(string[])}, so the packaged
     * jar behaves exactly like the {@code mvn javafx:run} launch.
     *
     * @param args command-line arguments, forwarded to the application
     */
    public static void main(String[] args) {
        MainApp.main(args);
    }
}
