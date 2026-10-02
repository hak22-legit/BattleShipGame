package com.battleship.view.battle;

import com.battleship.model.FleetReadout;
import com.battleship.model.fog.TrackingGrid;

/**
 * strategy interface defining the viewport perspective for local combat rendering.
 *
 * <p>satisfies the <strong>open/closed principle (ocp)</strong> by decoupling the
 * viewpoint determination (fixed vs. alternating hotseat vs. spectator) from the
 * battle screen drawing routines.</p>
 */
public interface BattlePerspective {

    /** the display name of the admiral owning this viewport perspective. */
    String perspectiveName();

    /** read-only view of the fleet defending this viewport's waters. */
    FleetReadout perspectiveFleet();

    /** the display name of the opposing commander. */
    String opponentName();

    /** the knowledge tracking grid containing observed enemy waters from this perspective. */
    TrackingGrid opponentKnowledge();
}
