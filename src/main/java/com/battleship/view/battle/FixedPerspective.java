package com.battleship.view.battle;

import com.battleship.controller.GameController;
import com.battleship.model.FleetReadout;
import com.battleship.model.Player;
import com.battleship.model.fog.TrackingGrid;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * strategy implementation for single-player (vs ai) combat: the viewport is permanently
 * pinned to player 1 (the human user) on the left, observing player 2 (the ai) on the right.
 */
public class FixedPerspective implements BattlePerspective {

    private final Supplier<String> nameSupplier;
    private final Supplier<FleetReadout> fleetSupplier;
    private final Supplier<String> opponentNameSupplier;
    private final Supplier<TrackingGrid> trackingGridSupplier;

    public FixedPerspective(GameController controller) {
        Objects.requireNonNull(controller, "GameController is required.");
        this.nameSupplier = () -> controller.getPlayerName(1);
        this.fleetSupplier = () -> controller.getPlayerFleet(1);
        this.opponentNameSupplier = () -> controller.getPlayerName(2);
        this.trackingGridSupplier = () -> controller.getTrackingGrid(1);
    }

    public FixedPerspective(Player human, Player opponent) {
        Objects.requireNonNull(human, "Human player is required.");
        Objects.requireNonNull(opponent, "Opponent player is required.");
        this.nameSupplier = human::name;
        this.fleetSupplier = () -> human;
        this.opponentNameSupplier = opponent::name;
        this.trackingGridSupplier = human::trackingGrid;
    }

    @Override
    public String perspectiveName() {
        return nameSupplier.get();
    }

    @Override
    public FleetReadout perspectiveFleet() {
        return fleetSupplier.get();
    }

    @Override
    public String opponentName() {
        return opponentNameSupplier.get();
    }

    @Override
    public TrackingGrid opponentKnowledge() {
        return trackingGridSupplier.get();
    }
}
