package com.battleship.view.quiz;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * a themed "mysterious box" pop-up that must be answered correctly before a
 * nuclear launcher shot is allowed to fire. blocks (via showandwait) until the
 * player picks an option, then reports whether the answer was correct.
 */
public final class NuclearLaunchDialog {

    private NuclearLaunchDialog() { }
    private static Stage active;
    public static void dismissActive() { if (active != null) active.close(); }

    /** shows the dialog modally and returns true only if the player answered correctly. */
    public static boolean askAndAwaitAuthorization(Window owner) {
        QuizQuestion question = QuizBank.random();
        boolean[] correct = {false};

        Stage stage = new Stage(StageStyle.TRANSPARENT);
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);

        Label lock = new Label("\u2622");
        lock.setStyle("-fx-font-size:40px; -fx-text-fill:#ffd166;");

        Label title = new Label("MYSTERIOUS BOX");
        title.setFont(Font.font("Arial Black", FontWeight.BOLD, 22));
        title.setTextFill(Color.web("#ffd166"));

        Label subtitle = new Label("AUTHORIZE LAUNCH · YOUR TURN TIMER IS STILL RUNNING");
        subtitle.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 11));
        subtitle.setTextFill(Color.web("#ff5c5c"));

        Label questionLabel = new Label(question.prompt());
        questionLabel.setWrapText(true);
        questionLabel.setMaxWidth(340);
        questionLabel.setStyle("-fx-text-fill:#f5f7fa; -fx-font-size:14px;");
        questionLabel.setAlignment(Pos.CENTER);
        questionLabel.setTextAlignment(TextAlignment.CENTER);

        GridPane options = new GridPane();
        options.setHgap(10);
        options.setVgap(10);
        options.setAlignment(Pos.CENTER);

        String[] opts = question.options();
        for (int i = 0; i < opts.length; i++) {
            int idx = i;
            Button b = new Button(opts[i]);
            b.setPrefWidth(160);
            b.setPrefHeight(44);
            b.setWrapText(true);
            b.setFont(Font.font("Arial", FontWeight.BOLD, 12));
            b.setStyle("-fx-background-color:#2e5d87; -fx-text-fill:#f5f7fa; -fx-background-radius:8; " +
                       "-fx-border-color:#44b8ff; -fx-border-radius:8; -fx-border-width:1.5;");
            b.setOnMouseEntered(e -> { b.setScaleX(1.05); b.setScaleY(1.05); });
            b.setOnMouseExited(e -> { b.setScaleX(1.0); b.setScaleY(1.0); });
            b.setOnAction(e -> {
                correct[0] = idx == question.correctIndex();
                stage.close();
            });
            options.add(b, i % 2, i / 2);
        }

        VBox layout = new VBox(12, lock, title, subtitle, questionLabel, options);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(28));
        layout.setMaxWidth(400);
        layout.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #102e4a, #081a2d);" +
                "-fx-background-radius:14; -fx-border-radius:14; -fx-border-width:2; -fx-border-color:#ff5c5c;");

        DropShadow glow = new DropShadow();
        glow.setColor(Color.web("#ff5c5c"));
        glow.setRadius(35);
        glow.setSpread(0.3);
        layout.setEffect(glow);

        Scene scene = new Scene(layout);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);

        layout.setScaleX(0.85);
        layout.setScaleY(0.85);
        layout.setOpacity(0.0);
        stage.setOnShown(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(180), layout);
            scale.setToX(1.0);
            scale.setToY(1.0);
            FadeTransition fade = new FadeTransition(Duration.millis(180), layout);
            fade.setToValue(1.0);
            scale.play();
            fade.play();
        });

        active = stage;
        try { stage.showAndWait(); } finally { if (active == stage) active = null; }
        return correct[0];
    }
}
