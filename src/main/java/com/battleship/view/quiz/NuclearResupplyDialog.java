package com.battleship.view.quiz;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * "resupply inbound" pop-up shown the moment a player's nuclear ammo hits
 * zero. unlike {@link nuclearlaunchdialog} this is non-blocking (no
 * showandwait) — it counts down from 30 seconds in the corner while the game
 * keeps going, then auto-closes and grants +1 nuclear shot with no action
 * required from the player.
 */
public final class NuclearResupplyDialog {

    private static final int SECONDS = 30;
    private static Stage activeStage = null;

    private NuclearResupplyDialog() { }

    /** dismisses any currently running resupply countdown cleanly without resupplying. */
    public static void dismissActive() {
        if (activeStage != null) {
            try {
                activeStage.close();
            } catch (Exception ignored) { }
            activeStage = null;
        }
    }

    /** shows the countdown and invokes {@code onresupplied} once it reaches zero. */
    public static void show(Window owner, Runnable onResupplied) {
        dismissActive();
        Stage stage = new Stage(StageStyle.TRANSPARENT);
        activeStage = stage;
        if (owner != null) stage.initOwner(owner);

        Label icon = new Label("\u2622");
        icon.setStyle("-fx-font-size:30px; -fx-text-fill:#ffd166;");

        Label title = new Label("NUCLEAR RESUPPLY INBOUND");
        title.setFont(Font.font("Arial Black", FontWeight.BOLD, 14));
        title.setTextFill(Color.web("#ffd166"));

        Label countdown = new Label(SECONDS + "s");
        countdown.setFont(Font.font("Arial", FontWeight.BOLD, 26));
        countdown.setTextFill(Color.web("#f5f7fa"));

        VBox layout = new VBox(8, icon, title, countdown);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(18, 26, 18, 26));
        layout.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #102e4a, #081a2d);" +
                "-fx-background-radius:14; -fx-border-radius:14; -fx-border-width:2; -fx-border-color:#ffd166;");

        DropShadow glow = new DropShadow();
        glow.setColor(Color.web("#ffd166"));
        glow.setRadius(22);
        glow.setSpread(0.25);
        layout.setEffect(glow);

        Scene scene = new Scene(layout);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);

        if (owner != null) {
            stage.setX(owner.getX() + owner.getWidth() - 260);
            stage.setY(owner.getY() + 60);
        }

        int[] remaining = {SECONDS};
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            remaining[0]--;
            countdown.setText(Math.max(remaining[0], 0) + "s");
            if (remaining[0] <= 0) stage.close();
        }));
        timeline.setCycleCount(SECONDS);

        stage.setOnHidden(e -> {
            timeline.stop();
            if (activeStage == stage) {
                activeStage = null;
            }
            if (remaining[0] <= 0 && onResupplied != null) {
                onResupplied.run();
            }
        });

        layout.setOpacity(0.0);
        FadeTransition fade = new FadeTransition(Duration.millis(200), layout);
        fade.setToValue(1.0);
        stage.setOnShown(e -> fade.play());

        stage.show();
        timeline.play();
    }
}
