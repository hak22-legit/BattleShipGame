package com.battleship.view.battle;

import com.battleship.model.TurnDeadline;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/** Shared countdown component for local and network battles. */
public final class TurnClock {
    private final TurnDeadline deadline = new TurnDeadline();
    private final Label label = new Label("TURN TIME  01:00");
    private final ProgressBar progress = new ProgressBar(1);
    private final VBox node = new VBox(5, label, progress);
    private final Timeline ticker;
    private final Runnable expired;
    public TurnClock(Runnable expired) {
        this.expired = expired;
        node.getStyleClass().add("turn-clock");
        label.getStyleClass().add("clock-label");
        progress.setMaxWidth(Double.MAX_VALUE);
        node.setPrefWidth(180);
        ticker = new Timeline(new KeyFrame(Duration.millis(100), e -> poll()));
        ticker.setCycleCount(Timeline.INDEFINITE);
    }
    public VBox node() { return node; }
    public void start() { deadline.start(); poll(); ticker.playFromStart(); }
    public void stop() { ticker.stop(); deadline.stop(); }
    public void waiting() { stop(); label.setText("RESOLVING SHOT…"); }
    public void poll() {
        int seconds = deadline.secondsLeft();
        label.setText(String.format("TURN TIME  %02d:%02d", seconds / 60, seconds % 60));
        progress.setProgress(seconds / (double) TurnDeadline.TURN_SECONDS);
        node.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("urgent"), seconds <= 10);
        if (deadline.expireIfDue()) { ticker.stop(); expired.run(); }
    }
}
