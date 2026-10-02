package com.battleship.model;

import java.util.Map;

/**
 * a person at the keyboard (or two, in hotseat). supplies shots through the ui
 * and learns nothing automatically — see {@link player#decideautonomousshot()}
 * and {@link player#observeownshot(shotresult)}, whose default implementations
 * are exactly this class's behaviour.
 */
public final class HumanPlayer extends Player {

    public HumanPlayer(String name, int boardSize, Map<ShipType, Integer> enemyFleetComposition) {
        super(name, boardSize, enemyFleetComposition);
    }

    /** convenience for the standard match flow: board size and roster come from the theater. */
    public HumanPlayer(String name, Theater theater) {
        this(name, theater.getBoardSize(), theater.getFleetComposition());
    }

    @Override
    public boolean isAutonomous() {
        return false;
    }
}
