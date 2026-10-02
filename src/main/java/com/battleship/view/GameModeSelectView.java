package com.battleship.view;

import com.battleship.controller.GameController;
import com.battleship.model.GameMode;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

/**
 * shown right after clicking play. three command cards — vs ai, hotseat,
 * online — matching the "select mode" mockup. stores the selection in
 * gamecontroller before moving to board select.
 */
public class GameModeSelectView {

    private final ViewNavigator nav;
    private final GameController controller;

    public GameModeSelectView(ViewNavigator nav, GameController controller) {
        this.nav = nav;
        this.controller = controller;
    }

    public StackPane build() {
        Label title = new Label("SELECT GAME MODE");
        title.setFont(Font.font("Arial Black", FontWeight.BOLD, 40));
        title.getStyleClass().add("app-title");

        Label subtitle = new Label("\u2693  CHOOSE YOUR BATTLE  \u2693");
        subtitle.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 14));
        subtitle.getStyleClass().add("app-subtitle");

        HBox cards = new HBox(30, aiCard(), hotseatCard(), onlineCard());
        cards.setAlignment(Pos.CENTER);

        Button back = new Button("\u2190  BACK");
        back.getStyleClass().add("ghost-button");
        back.setPrefWidth(140);
        back.setPrefHeight(42);
        back.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        back.setOnAction(e -> { nav.getAudio().playClick(); nav.showMainMenu(); });

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

        // same gentle entrance used on the main menu, so navigating between
        // screens feels continuous rather than an abrupt cut.
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

    private void selectMode(GameMode mode) {
        nav.getAudio().playClick();
        controller.setMode(mode);
        nav.showBoardSelect();
    }

    // ---------- vs ai card ----------

    private VBox aiCard() {
        Label heading = new Label("VS AI");
        heading.getStyleClass().add("mode-card-title");

        Label badge = new Label("SINGLE PLAYER");
        badge.getStyleClass().addAll("mode-card-badge", "mode-card-badge-ai");

        StackPane icon = iconRing(DecorUtil.animatedRadarSweep(76), "mode-card-icon-ring-lg");

        Button easy = subButton("EASY");
        easy.setOnAction(e -> selectMode(GameMode.AI_EASY));
        Button normal = subButton("NORMAL");
        normal.setOnAction(e -> selectMode(GameMode.AI_NORMAL));
        Button hard = subButton("HARD");
        hard.setOnAction(e -> selectMode(GameMode.AI_HARD));

        VBox difficulties = new VBox(9, easy, normal, hard);
        difficulties.setAlignment(Pos.CENTER);

        return card(heading, badge, icon, difficulties, "mode-card-ai");
    }

    // ---------- hotseat card ----------

    private VBox hotseatCard() {
        Label heading = new Label("HOTSEAT");
        heading.getStyleClass().add("mode-card-title");

        Label badge = new Label("SAME DEVICE");
        badge.getStyleClass().addAll("mode-card-badge", "mode-card-badge-hotseat");

        Image anchorArt = ImageResources.ui("icon-anchor");
        javafx.scene.Node iconInner;
        if (anchorArt != null) {
            ImageView iv = new ImageView(anchorArt);
            iv.setFitWidth(48);
            iv.setFitHeight(48);
            iv.setPreserveRatio(true);
            iconInner = iv;
        } else {
            Label handshake = new Label("\u2694");
            handshake.setFont(Font.font(36));
            iconInner = handshake;
        }
        StackPane icon = iconRing(iconInner, "mode-card-icon-ring-hotseat");

        Label sub = new Label("PLAYER 1  vs  PLAYER 2");
        sub.getStyleClass().add("mode-card-sub");
        sub.setWrapText(true);
        sub.setAlignment(Pos.CENTER);
        sub.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        Button select = new Button("SELECT");
        select.getStyleClass().addAll("primary-button");
        select.setPrefWidth(180);
        select.setPrefHeight(46);
        select.setAlignment(Pos.CENTER);
        select.setOnAction(e -> selectMode(GameMode.HOTSEAT));
        select.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_CLICKED, javafx.event.Event::consume);

        VBox body = new VBox(16, sub, select);
        body.setAlignment(Pos.CENTER);

        VBox card = card(heading, badge, icon, body, "mode-card-hotseat");
        card.setOnMouseClicked(e -> selectMode(GameMode.HOTSEAT));
        return card;
    }

    // ---------- online card (real lan + qr feature, styled as the third card) ----------

    private VBox onlineCard() {
        Label heading = new Label("ONLINE");
        heading.getStyleClass().add("mode-card-title");

        Label badge = new Label("LAN / QR MATCH");
        badge.getStyleClass().addAll("mode-card-badge", "mode-card-badge-online");

        Image compassArt = ImageResources.ui("compass-rose");
        javafx.scene.Node iconInner;
        if (compassArt != null) {
            ImageView iv = new ImageView(compassArt);
            iv.setFitWidth(52);
            iv.setFitHeight(52);
            iv.setPreserveRatio(true);
            iconInner = iv;
        } else {
            Label compassGlyph = new Label("\u2693");
            compassGlyph.setFont(Font.font(36));
            iconInner = compassGlyph;
        }
        StackPane icon = iconRing(iconInner, "mode-card-icon-ring-online");

        Label sub = new Label("LAN + QR MATCHMAKING");
        sub.getStyleClass().add("mode-card-sub");
        sub.setWrapText(true);
        sub.setMinWidth(210);
        sub.setPrefWidth(210);
        sub.setAlignment(Pos.CENTER);
        sub.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        Button select = new Button("PLAY");
        select.getStyleClass().addAll("primary-button", "featured-button");
        select.setPrefWidth(180);
        select.setPrefHeight(46);
        select.setAlignment(Pos.CENTER);
        select.setOnAction(e -> {
            nav.getAudio().playClick();
            controller.setMode(GameMode.ONLINE);
            nav.showMultiplayerLobby();
        });
        select.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_CLICKED, javafx.event.Event::consume);

        VBox body = new VBox(16, sub, select);
        body.setAlignment(Pos.CENTER);

        VBox card = card(heading, badge, icon, body, "mode-card-online");
        card.setOnMouseClicked(e -> {
            nav.getAudio().playClick();
            controller.setMode(GameMode.ONLINE);
            nav.showMultiplayerLobby();
        });
        return card;
    }

    // ---------- shared card chrome ----------

    private VBox card(Label heading, Label badge, StackPane icon, javafx.scene.Node body, String accentClass) {
        VBox box = new VBox(18, heading, badge, icon, body);
        box.setAlignment(Pos.TOP_CENTER);
        box.setPadding(new Insets(32, 26, 32, 26));
        box.setPrefWidth(268);
        box.setPrefHeight(360);
        box.getStyleClass().addAll("card-panel", "mode-card", accentClass);
        box.setCursor(javafx.scene.Cursor.HAND);

        // a small lift + scale on hover so the cards feel tactile, matching
        // the button hover treatment used across the rest of the app.
        box.setOnMouseEntered(e -> { box.setScaleX(1.03); box.setScaleY(1.03); box.setTranslateY(-4); });
        box.setOnMouseExited(e -> { box.setScaleX(1.0); box.setScaleY(1.0); box.setTranslateY(0); });

        return box;
    }

    private StackPane iconRing(javafx.scene.Node inner, String ringStyleClass) {
        StackPane ring = new StackPane(inner);
        ring.getStyleClass().add(ringStyleClass);
        ring.setPrefSize(100, 100);
        ring.setMaxSize(100, 100);
        return ring;
    }

    private Button subButton(String text) {
        Button b = new Button(text);
        b.setPrefWidth(170);
        b.setPrefHeight(38);
        b.getStyleClass().add("ghost-button");
        b.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        return b;
    }
}
