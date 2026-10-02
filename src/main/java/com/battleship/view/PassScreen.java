package com.battleship.view;

import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

/** full-screen opaque overlay shown between hotseat turns (spec 10). */
public class PassScreen {

    private final String nextPlayerName;
    private final Runnable onContinue;

    public PassScreen(String nextPlayerName, Runnable onContinue) {
        this.nextPlayerName = nextPlayerName;
        this.onContinue = onContinue;
    }

    public StackPane build() {
        Image anchorArt = ImageResources.ui("icon-anchor");
        javafx.scene.Node icon;
        if (anchorArt != null) {
            ImageView iv = new ImageView(anchorArt);
            iv.setFitWidth(56);
            iv.setPreserveRatio(true);
            TranslateTransition bob = new TranslateTransition(Duration.millis(900), iv);
            bob.setFromY(-6);
            bob.setToY(6);
            bob.setAutoReverse(true);
            bob.setCycleCount(TranslateTransition.INDEFINITE);
            bob.play();
            icon = iv;
        } else {
            Label fallback = new Label("\uD83D\uDC41");
            fallback.setStyle("-fx-font-size:40px;");
            icon = fallback;
        }

        Label text = new Label("PASS COMMAND TO " + nextPlayerName.toUpperCase());
        text.setFont(Font.font("Arial Black", FontWeight.BOLD, 30));
        text.getStyleClass().add("app-title");

        Label subtext = new Label("Press CONTINUE when ready. The other Admiral should look away.");
        subtext.getStyleClass().add("info-text");

        Button continueBtn = new Button("CONTINUE");
        continueBtn.setPrefWidth(200);
        continueBtn.setPrefHeight(48);
        continueBtn.getStyleClass().add("primary-button");
        continueBtn.setOnAction(e -> onContinue.run());

        VBox layout = new VBox(20, icon, text, subtext, continueBtn);
        layout.setAlignment(Pos.CENTER);

        StackPane root = new StackPane(layout);
        root.getStyleClass().add("pass-screen-root");
        return root;
    }
}
