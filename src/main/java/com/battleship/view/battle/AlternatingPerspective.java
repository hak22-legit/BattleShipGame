package com.battleship.view.battle;

import com.battleship.controller.GameController;
import com.battleship.model.FleetReadout;
import com.battleship.model.Player;
import com.battleship.model.fog.TrackingGrid;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * strategy implementation for hotseat (pass-and-play) combat: the viewport dynamically
 * alternates to whichever admiral currently holds the turn.
 */
public class AlternatingPerspective implements BattlePerspective {

    private final Supplier<Player> currentSupplier;
    private final Supplier<Player> opponentSupplier;

    public AlternatingPerspective(GameController controller) {
        Objects.requireNonNull(controller, "GameController is required.");
        this.currentSupplier = controller::getCurrentPlayer;
        this.opponentSupplier = controller::getOpponent;
    }

    public AlternatingPerspective(Supplier<Player> currentSupplier, Supplier<Player> opponentSupplier) {
        this.currentSupplier = Objects.requireNonNull(currentSupplier, "Current player supplier is required.");
        this.opponentSupplier = Objects.requireNonNull(opponentSupplier, "Opponent supplier is required.");
    }

    @Override
    public String perspectiveName() {
        return currentSupplier.get().name();
    }

    @Override
    public FleetReadout perspectiveFleet() {
        return currentSupplier.get();
    }

    @Override
    public String opponentName() {
        return opponentSupplier.get().name();
    }

    @Override
    public TrackingGrid opponentKnowledge() {
        return currentSupplier.get().trackingGrid();
    }
}
