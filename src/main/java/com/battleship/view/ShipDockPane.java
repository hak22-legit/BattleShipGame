package com.battleship.view;

import com.battleship.controller.GameController;
import com.battleship.model.Orientation;
import com.battleship.model.Player;
import com.battleship.model.ShipType;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.Map;

/**
 * Compact ship inventory. The active ship rotates in the placement preview;
 * dock thumbnails stay horizontal so the full inventory remains reachable.
 */
public class ShipDockPane extends VBox {

    private static final int UNIT = 34;

    private final GameController controller;
    private final Player player;
    private ShipType selectedShipType = null;
    private java.util.function.Consumer<ShipType> onSelectionChanged;
    private java.util.function.BiConsumer<ShipType, MouseEvent> onDragStarted;
    private boolean dragging;

    public void setOnDragStarted(java.util.function.BiConsumer<ShipType, MouseEvent> handler) {
        onDragStarted = handler;
    }

    /** Keep the pressed node attached until its mouse gesture finishes. */
    public void setDragging(boolean dragging) { this.dragging = dragging; }

    public ShipDockPane(GameController controller, Player player) {
        this.controller = controller;
        this.player = player;
        setSpacing(16);
        setPadding(new Insets(8, 4, 8, 4));
        setAlignment(Pos.TOP_CENTER);
        refresh();
    }

    public void setOnSelectionChanged(java.util.function.Consumer<ShipType> onSelectionChanged) {
        this.onSelectionChanged = onSelectionChanged;
    }

    public ShipType getSelectedShip() {
        return selectedShipType;
    }

    public void selectShip(ShipType type) {
        this.selectedShipType = type;
        if (onSelectionChanged != null) {
            onSelectionChanged.accept(selectedShipType);
        }
        refresh();
    }

    public void clearSelection() {
        if (this.selectedShipType != null) {
            this.selectedShipType = null;
            if (onSelectionChanged != null) {
                onSelectionChanged.accept(null);
            }
            refresh();
        }
    }

    /** rebuilds the dock contents from the controller's remaining-ship counts. */
    public void refresh() {
        if (dragging) return;
        getChildren().clear();
        Label header = new Label("SHIP DOCK");
        header.getStyleClass().add("side-card-title");
        getChildren().add(header);

        Map<ShipType, Integer> remaining = controller.getRemainingShipCounts(player);
        if (selectedShipType != null && remaining.getOrDefault(selectedShipType, 0) == 0) {
            selectedShipType = null;
            if (onSelectionChanged != null) {
                onSelectionChanged.accept(null);
            }
        }
        for (Map.Entry<ShipType, Integer> entry : remaining.entrySet()) {
            for (int i = 0; i < entry.getValue(); i++) {
                getChildren().add(buildShipNode(entry.getKey()));
            }
        }
    }

    private Pane buildShipNode(ShipType type) {
        Image sprite = ImageResources.ship(type, Orientation.HORIZONTAL);
        int len = type.getSize();
        double w = UNIT * len + (len - 1);
        double h = UNIT;

        StackPane block = new StackPane();
        block.setPrefSize(w, h);
        block.setMaxSize(w, h);

        boolean isSelected = type == selectedShipType;

        Rectangle frame = new Rectangle(w, h);
        frame.setArcWidth(10);
        frame.setArcHeight(10);
        frame.setFill(sprite == null ? Color.web("#1c4468") : Color.TRANSPARENT);
        frame.setStroke(isSelected ? Color.web("#ffd166") : Color.web("#63c4ff", 0.85));
        frame.setStrokeWidth(isSelected ? 2.2 : 1.3);

        if (sprite != null) {
            // the whole hull rendered as one uncut image, so the ship reads as
            // a single vessel in the dock rather than a row of bordered tiles.
            ImageView iv = new ImageView(sprite);
            iv.setFitWidth(w);
            iv.setFitHeight(h);
            iv.setPreserveRatio(false);
            iv.setMouseTransparent(true);
            javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(w, h);
            clip.setArcWidth(10);
            clip.setArcHeight(10);
            iv.setClip(clip);
            block.getChildren().add(iv);
        }
        block.getChildren().add(frame);
        frame.setMouseTransparent(true);

        if (isSelected) {
            block.setEffect(new javafx.scene.effect.DropShadow(12, Color.web("#ffd166", 0.85)));
            block.getStyleClass().addAll("ship-block", "ship-block-selected");
        } else {
            block.setEffect(new javafx.scene.effect.DropShadow(8, Color.web("#44b8ff", 0.4)));
            block.getStyleClass().add("ship-block");
        }

        block.setUserData(type);

        block.setOnMouseClicked(event -> {
            if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                if (selectedShipType == type) {
                    clearSelection();
                } else {
                    selectShip(type);
                }
                event.consume();
            }
        });

        block.setOnDragDetected(event -> {
            if (event.isPrimaryButtonDown() && onDragStarted != null) onDragStarted.accept(type, event);
            event.consume();
        });

        return block;
    }
}
