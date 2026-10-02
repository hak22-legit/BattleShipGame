package com.battleship.view;

import com.battleship.controller.GameController;
import com.battleship.model.Theater;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * "play with a friend" entry point: choose to host a lan game (shows a qr
 * invite code) or join one a friend is hosting.
 */
public class MultiplayerLobbyView {

    private final ViewNavigator nav;
    private final GameController controller;
    private VBox contentArea;

    public MultiplayerLobbyView(ViewNavigator nav, GameController controller) {
        this.nav = nav;
        this.controller = controller;
    }

    private boolean inTheaterStep = false;

    public StackPane build() {
        controller.setMode(com.battleship.model.GameMode.ONLINE);
        Label title = new Label("PLAY WITH A FRIEND");
        title.setFont(Font.font("Arial Black", FontWeight.BOLD, 30));
        title.getStyleClass().add("app-title");

        Label subtitle = new Label("Same WiFi/network — no internet or account needed.");
        subtitle.getStyleClass().add("dim-text");

        contentArea = new VBox(16);
        contentArea.setAlignment(Pos.CENTER);
        showChoiceStep();

        Button back = new Button("BACK");
        back.getStyleClass().add("ghost-button");
        back.setPrefWidth(120);
        back.setOnAction(e -> {
            nav.getAudio().playClick();
            if (inTheaterStep) {
                showChoiceStep();
            } else {
                nav.showModeSelect();
            }
        });

        VBox layout = new VBox(20, title, subtitle, contentArea, back);
        layout.setAlignment(Pos.CENTER);
        VBox.setMargin(back, new Insets(10, 0, 0, 0));

        return new StackPane(layout);
    }

    private void showChoiceStep() {
        inTheaterStep = false;
        contentArea.getChildren().clear();

        Button host = navButton("HOST A GAME");
        host.getStyleClass().add("featured-button");
        host.setOnAction(e -> {
            nav.getAudio().playClick();
            showTheaterStep();
        });

        Button join = navButton("JOIN A GAME");
        join.setOnAction(e -> {
            nav.getAudio().playClick();
            nav.setScreen(new JoinLobbyView(nav, controller).build());
        });

        contentArea.getChildren().addAll(host, join);
    }

    private void showTheaterStep() {
        inTheaterStep = true;
        contentArea.getChildren().clear();

        Label pick = new Label("Choose the battlefield size:");
        pick.getStyleClass().add("info-text");

        HBox choices = new HBox(12,
                theaterButton(Theater.SKIRMISH),
                theaterButton(Theater.ENGAGEMENT),
                theaterButton(Theater.FLEET_ACTION));
        choices.setAlignment(Pos.CENTER);

        contentArea.getChildren().addAll(pick, choices);
    }

    private Button theaterButton(Theater theater) {
        Button b = navButton(theater.getDisplayName() + "  (" + theater.getBoardSize() + "\u00D7" + theater.getBoardSize() + ")");
        b.setOnAction(e -> {
            nav.getAudio().playClick();
            nav.setScreen(new HostLobbyView(nav, controller, theater).build());
        });
        return b;
    }

    private Button navButton(String text) {
        Button b = new Button(text);
        b.setPrefWidth(300);
        b.setPrefHeight(52);
        b.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        b.getStyleClass().add("primary-button");
        b.setOnMouseEntered(e -> { b.setScaleX(1.04); b.setScaleY(1.04); });
        b.setOnMouseExited(e -> { b.setScaleX(1.0); b.setScaleY(1.0); });
        return b;
    }
}
