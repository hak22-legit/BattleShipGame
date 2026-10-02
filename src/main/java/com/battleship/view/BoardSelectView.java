package com.battleship.view;

import com.battleship.controller.GameController;
import com.battleship.model.ShipType;
import com.battleship.model.Theater;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

/**
 * issue 3: shown after mode selection. three battlefield cards with a mini
 * grid preview, ship list, and select button. stores selection via
 * controller.settheater(...), which also initializes the boards/players.
 */
public class BoardSelectView {

    private final ViewNavigator nav;
    private final GameController controller;

    public BoardSelectView(ViewNavigator nav, GameController controller) {
        this.nav = nav;
        this.controller = controller;
    }

    public StackPane build() {
        Label title = new Label("CHOOSE YOUR BATTLEFIELD");
        title.setFont(Font.font("Arial Black", FontWeight.BOLD, 38));
        title.getStyleClass().add("app-title");

        Label subtitle = new Label("\u2693  SELECT A GRID SIZE  \u2693");
        subtitle.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 14));
        subtitle.getStyleClass().add("app-subtitle");

        HBox cards = new HBox(30,
                card(Theater.SKIRMISH, "3 ships, 7 hits", "mode-card-ai", "board-preview-frame-ai", "#44b8ff"),
                card(Theater.ENGAGEMENT, "5 ships, 14 hits", "mode-card-hotseat", "board-preview-frame-hotseat", "#6be89b"),
                card(Theater.FLEET_ACTION, "7 ships, 19 hits", "mode-card-online", "board-preview-frame-online", "#ffd166"));
        cards.setAlignment(Pos.CENTER);

        Button back = new Button("\u2190  BACK");
        back.getStyleClass().add("ghost-button");
        back.setPrefWidth(140);
        back.setPrefHeight(42);
        back.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        back.setOnAction(e -> { nav.getAudio().playClick(); nav.showModeSelect(); });

        VBox layout = new VBox(16, title, subtitle, cards, back);
        layout.setAlignment(Pos.CENTER);
        VBox.setMargin(cards, new Insets(24, 0, 0, 0));
        VBox.setMargin(back, new Insets(14, 0, 0, 0));

        StackPane root = new StackPane();
        javafx.scene.canvas.Canvas ocean = DecorUtil.animatedOceanScene(root);
        root.getChildren().add(ocean);
        javafx.scene.image.ImageView compass = DecorUtil.compassWatermark(420);
        if (compass != null) {
            compass.setOpacity(0.06);
            root.getChildren().add(compass);
        }
        root.getChildren().add(layout);

        layout.setOpacity(0.0);
        layout.setTranslateY(16);
        FadeTransition fade = new FadeTransition(Duration.millis(380), layout);
        fade.setToValue(1.0);
        TranslateTransition rise = new TranslateTransition(Duration.millis(380), layout);
        rise.setToY(0);
        fade.play();
        rise.play();

        return root;
    }

    private VBox card(Theater theater, String statsLine, String accentClass, String frameAccentClass, String accentHex) {
        Label name = new Label(theater.getDisplayName());
        name.setFont(Font.font("Arial Black", FontWeight.BOLD, 19));
        name.setStyle("-fx-text-fill:" + accentHex + "; -fx-letter-spacing: 0.6px;");

        Label sizeBadge = new Label(theater.getBoardSize() + "\u00D7" + theater.getBoardSize() + " GRID");
        sizeBadge.setStyle(
                "-fx-text-fill:" + accentHex + ";"
                + "-fx-font-size: 10px; -fx-font-weight: bold; -fx-letter-spacing: 1.4px;"
                + "-fx-background-color: derive(" + accentHex + ", -70%);"
                + "-fx-background-radius: 8; -fx-border-radius: 8;"
                + "-fx-border-color: " + accentHex + "; -fx-border-width: 1;"
                + "-fx-padding: 3 10;");

        StackPane gridFrame = new StackPane(miniGridPreview(theater.getBoardSize(), accentHex));
        gridFrame.getStyleClass().addAll("board-preview-frame", frameAccentClass);
        gridFrame.setMaxWidth(Region.USE_PREF_SIZE);

        Label stats = new Label(statsLine);
        stats.getStyleClass().add("info-text");
        stats.setWrapText(true);
        stats.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        Label time = new Label("\u23F1  " + theater.getSessionEstimate());
        time.getStyleClass().add("dim-text");

        VBox shipList = new VBox(3);
        shipList.setAlignment(Pos.CENTER_LEFT);
        for (var entry : theater.getFleetComposition().entrySet()) {
            ShipType type = entry.getKey();
            Label l = new Label("\u25B8 " + entry.getValue() + "x " + type.name().replace('_', ' ') + " (" + type.getSize() + ")");
            l.getStyleClass().add("theater-ship-row");
            shipList.getChildren().add(l);
        }
        VBox shipWrap = new VBox(shipList);
        shipWrap.setAlignment(Pos.CENTER);

        Button select = new Button("SELECT");
        select.setPrefWidth(190);
        select.setPrefHeight(44);
        select.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        select.getStyleClass().add("primary-button");
        select.setOnMouseEntered(e -> { select.setScaleX(1.04); select.setScaleY(1.04); });
        select.setOnMouseExited(e -> { select.setScaleX(1.0); select.setScaleY(1.0); });
        select.setOnAction(e -> {
            nav.getAudio().playClick();
            controller.setTheater(theater);
            nav.showShipPlacement();
        });
        select.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_CLICKED, javafx.event.Event::consume);

        VBox top = new VBox(6, name, sizeBadge);
        top.setAlignment(Pos.CENTER);

        VBox info = new VBox(4, stats, time);
        info.setAlignment(Pos.CENTER);

        VBox card = new VBox(16, top, gridFrame, info, shipWrap, select);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(28, 22, 28, 22));
        card.setPrefWidth(272);
        card.setPrefHeight(420);
        card.getStyleClass().addAll("card-panel", "mode-card", accentClass);
        card.setCursor(javafx.scene.Cursor.HAND);

        card.setOnMouseEntered(e -> { card.setScaleX(1.025); card.setScaleY(1.025); card.setTranslateY(-4); });
        card.setOnMouseExited(e -> { card.setScaleX(1.0); card.setScaleY(1.0); card.setTranslateY(0); });
        card.setOnMouseClicked(e -> {
            nav.getAudio().playClick();
            controller.setTheater(theater);
            nav.showShipPlacement();
        });

        return card;
    }

    private GridPane miniGridPreview(int size, String accentHex) {
        GridPane grid = new GridPane();
        grid.setHgap(1.5);
        grid.setVgap(1.5);
        double cellSize = Math.max(6, 92.0 / size);
        Color accent = Color.web(accentHex);
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                Rectangle rect = new Rectangle(cellSize, cellSize);
                boolean checker = (r + c) % 2 == 0;
                LinearGradient fill = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                        new Stop(0, checker ? Color.web("#123a5c") : Color.web("#0d2c47")),
                        new Stop(1, checker ? Color.web("#0d2c47") : Color.web("#0a2438")));
                rect.setFill(fill);
                rect.setStroke(accent.deriveColor(0, 1, 1, 0.35));
                rect.setStrokeWidth(0.6);
                rect.setArcWidth(1.5);
                rect.setArcHeight(1.5);
                grid.add(rect, c, r);
            }
        }
        return grid;
    }
}
