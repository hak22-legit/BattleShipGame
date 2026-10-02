package com.battleship.view;

import com.battleship.controller.GameController;
import com.battleship.model.GameMode;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

/**
 * issue 4 + 5: local (vs ai / hotseat) ship-placement screen.
 * left = dock, center = board card, right = status panel,
 * bottom = rotate / auto place / reset.
 *
 * <p>extends {@link abstractshipplaceview}, so the dock, drag-and-drop,
 * ghost preview, orientation handling, counter and ready gating are all
 * inherited. this class contributes only the local chrome (ocean backdrop,
 * entrance animation, rotate hint) and the local ready/exit behaviour.</p>
 */
public class ShipPlaceView extends AbstractShipPlaceView {

    public ShipPlaceView(ViewNavigator nav, GameController controller) {
        super(nav, controller, controller.getPlacingPlayer());
    }

    @Override
    protected Pane assembleLayout() {
        Label title = new Label("DEPLOY YOUR FLEET");
        title.setFont(Font.font("Arial Black", FontWeight.BOLD, 34));
        title.getStyleClass().add("app-title");

        Label subtitle = new Label("\u2693  " + player.getName().toUpperCase() + " \u2014 POSITION YOUR SHIPS  \u2693");
        subtitle.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 13));
        subtitle.getStyleClass().add("app-subtitle");

        dockPane.getStyleClass().add("side-card");
        dockPane.setPrefWidth(190);

        Label statusHeader = new Label("STATUS");
        statusHeader.getStyleClass().add("side-card-title");

        countLabel.getStyleClass().add("placement-count-strong");

        Label removeHint = new Label("Click a placed ship or drag it back to the dock.\nHold and drag to move. Press R to rotate while dragging. Esc cancels.");
        removeHint.setWrapText(true);
        removeHint.setMaxWidth(190);
        removeHint.setTextAlignment(TextAlignment.CENTER);
        removeHint.getStyleClass().add("dim-text");

        readyButton.setPrefWidth(170);
        readyButton.setPrefHeight(46);

        VBox statusPanel = new VBox(16, statusHeader, countLabel, orientationLabel, removeHint, readyButton);
        statusPanel.setPadding(new Insets(24, 22, 24, 22));
        statusPanel.setAlignment(Pos.TOP_CENTER);
        statusPanel.getStyleClass().add("side-card");
        statusPanel.setPrefWidth(240);

        VBox boardCard = buildBoardCard();

        Button rotate = new Button("\u21bb  ROTATE SHIP");
        rotate.getStyleClass().add("ghost-button");
        rotate.setPrefHeight(40);
        rotate.setOnAction(e -> {
            audio.playClick();
            toggleOrientation();
        });

        Button autoPlace = new Button("AUTO PLACE");
        autoPlace.getStyleClass().add("ghost-button");
        autoPlace.setPrefHeight(40);
        autoPlace.setOnAction(e -> {
            audio.playClick();
            audio.playPlaceShip();
            controller.autoPlaceRemaining(player);
            refreshAll();
        });

        Button reset = new Button("RESET");
        reset.getStyleClass().add("ghost-button");
        reset.setPrefHeight(40);
        reset.setOnAction(e -> {
            audio.playClick();
            controller.resetPlacement(player);
            refreshAll();
        });

        HBox bottomBar = new HBox(16, rotate, autoPlace, reset);
        bottomBar.setAlignment(Pos.CENTER);

        Button exit = buildExitButton();

        VBox titleBlock = new VBox(6, title, subtitle);
        titleBlock.setAlignment(Pos.CENTER);
        HBox center = new HBox(26, dockPane, boardCard, statusPanel);
        center.setAlignment(Pos.CENTER);
        center.setMaxWidth(Region.USE_PREF_SIZE);

        StackPane titleRow = new StackPane(titleBlock, exit);
        StackPane.setAlignment(exit, Pos.CENTER_RIGHT);
        titleRow.setMaxWidth(Region.USE_PREF_SIZE);
        center.widthProperty().addListener((obs, oldW, newW) -> {
            if (newW.doubleValue() > 0) {
                titleRow.setPrefWidth(newW.doubleValue());
                titleRow.setMaxWidth(newW.doubleValue());
            }
        });

        VBox layout = new VBox(24, titleRow, center, bottomBar);
        layout.setAlignment(Pos.CENTER);
        layout.setFillWidth(false);
        layout.setPadding(new Insets(28, 24, 24, 24));
        return layout;
    }

    @Override
    protected StackPane decorateRoot(Pane layout) {
        StackPane root = new StackPane();
        Canvas ocean = DecorUtil.lightSeaScene(root);
        root.getChildren().add(ocean);
        root.getChildren().add(layout);

        // same gentle entrance used on the mode-select screen, so navigating
        // into placement feels continuous rather than an abrupt cut.
        layout.setOpacity(0.0);
        layout.setTranslateY(16);
        FadeTransition fade = new FadeTransition(Duration.millis(380), layout);
        fade.setToValue(1.0);
        TranslateTransition rise = new TranslateTransition(Duration.millis(380), layout);
        rise.setToY(0);
        fade.play();
        rise.play();

        showRotateHint(root);
        return root;
    }

    // ---------- local flow: ready + exit ----------

    @Override
    protected void onReadyPressed() {
        controller.confirmReady();
        routeAfterReady();
    }

    @Override
    protected String exitPrompt() {
        return "Leave this game and return to the main menu? Your fleet deployment will be lost.";
    }

    @Override
    protected void onExitConfirmed() {
        // nothing extra for a local match; the base silences bgm and returns to the menu.
    }

    // ---------- local chrome ----------

    /** wraps the board grid in the same "board-card" chrome used on the battle screen. */
    private VBox buildBoardCard() {
        Label heading = new Label("YOUR WATERS");
        heading.getStyleClass().addAll("board-card-title", "board-card-title-lg");

        VBox card = new VBox(16, heading, boardGridPane);
        card.getStyleClass().add("board-card");
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(22));
        return card;
    }

    private void showRotateHint(StackPane root) {
        Label hint = new Label("Drag a ship • R to rotate • Drop in the dock to return it");
        hint.setMouseTransparent(true);
        hint.getStyleClass().addAll("status-panel", "rotate-hint");
        StackPane.setAlignment(hint, Pos.TOP_CENTER);
        StackPane.setMargin(hint, new Insets(16, 0, 0, 0));
        root.getChildren().add(hint);

        PauseTransition wait = new PauseTransition(Duration.seconds(3));
        FadeTransition fade = new FadeTransition(Duration.millis(500), hint);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        SequentialTransition seq = new SequentialTransition(wait, fade);
        seq.setOnFinished(e -> root.getChildren().remove(hint));
        seq.play();
    }

    private void routeAfterReady() {
        switch (controller.getState()) {
            case PASS_SCREEN -> nav.showPassScreen(() -> {
                controller.resumePlacementAfterPass();
                nav.showShipPlacement();
            });
            case BATTLE -> {
                if (controller.getSelectedMode() == GameMode.HOTSEAT) {
                    nav.showPassScreen(() -> nav.showBattle());
                } else {
                    nav.showBattle();
                }
            }
            default -> nav.showShipPlacement();
        }
    }
}
