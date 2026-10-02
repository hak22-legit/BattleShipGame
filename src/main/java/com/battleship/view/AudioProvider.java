package com.battleship.view;

/**
 * supplies the game's audio facade to a screen.
 *
 * <p>extracted from {@link viewnavigator} (srp / isp audit): a navigator should
 * navigate and nothing else. views that need sound ask for this tiny capability
 * (a lambda or a stub in tests), and screens that have no sound simply do not
 * request it.</p>
 */
@FunctionalInterface
public interface AudioProvider {

    GameAudio audio();
}
