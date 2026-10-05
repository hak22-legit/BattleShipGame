package com.battleship.model;

/**
 * backwards-compatible alias forwarding to {@link PlayerArsenal}.
 * strictly pure domain class with zero JavaFX imports.
 */
final class Arsenal extends PlayerArsenal {

    Arsenal(int boardSize) {
        super(boardSize);
    }
}
