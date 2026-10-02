package com.battleship.view;

import javafx.stage.Window;

/**
 * supplies the owning window to screens that need to anchor modal dialogs.
 *
 * <p>extracted from {@link viewnavigator} (srp / isp audit): leaking
 * {@code javafx.stage.stage} out of the navigator meant every screen could reach
 * the whole application window, whether or not it had any business doing so.</p>
 */
@FunctionalInterface
public interface WindowProvider {

    Window window();
}
