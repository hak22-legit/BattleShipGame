package com.battleship.view;

import com.battleship.model.Player;
import com.battleship.net.NetworkGameSession;

/**
 * abstraction for screen navigation (fixes v6 / c1). views depend on this
 * interface — never on the concrete {@code mainapp} — so they can be
 * unit-tested without a javafx runtime, and the application class no longer
 * has to be known to every screen.
 *
 * <p>{@link mainapp} is the production implementation; tests may supply a stub.
 * by extending {@link audioprovider} and {@link windowprovider}, the navigator
 * also satisfies segregated clients that only need sound playback or a dialog owner window.</p>
 */
public interface ViewNavigator extends ScreenNavigator, AudioProvider, WindowProvider {

    /** the window, needed only by modal dialogs owned by deep screens. */
    javafx.stage.Stage getStage();

    /** game audio facade, so views never touch the concrete sound manager. */
    GameAudio getAudio();

    @Override
    default GameAudio audio() {
        return getAudio();
    }

    @Override
    default javafx.stage.Window window() {
        return getStage();
    }
}
