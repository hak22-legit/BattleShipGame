package com.battleship.view.decor;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * a small "fleet command"-style radar sweep: concentric rings, crosshairs, a
 * rotating cyan sweep wedge, and gold blips that flare as the sweep passes
 * over them. one visual effect, one class (srp — extracted from the former
 * decorutil god object).
 */
public final class RadarSweepRenderer {

    private RadarSweepRenderer() { }

    /**
     * builds the radar sweep as a non-interactive square stackpane of the
     * given pixel size, meant to sit inside a side-panel card.
     */
    public static StackPane animatedRadarSweep(double size) {
        Canvas canvas = new Canvas(size, size);
        canvas.setMouseTransparent(true);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        double cx = size / 2.0, cy = size / 2.0, r = size / 2.0 - 4;

        List<double[]> blips = new ArrayList<>();
        java.util.Random rnd = new java.util.Random(7);
        for (int i = 0; i < 6; i++) {
            double a = rnd.nextDouble() * Math.PI * 2;
            double d = rnd.nextDouble() * 0.4 + 0.15;
            blips.add(new double[]{a, d});
        }

        final double[] angle = {0};
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                angle[0] += 0.02;
                gc.clearRect(0, 0, size, size);

                gc.setStroke(Color.rgb(46, 93, 135, 0.55));
                gc.setLineWidth(1);
                for (int i = 1; i <= 3; i++) {
                    double rr = r * i / 3.0;
                    gc.strokeOval(cx - rr, cy - rr, rr * 2, rr * 2);
                }
                gc.strokeLine(cx, cy - r, cx, cy + r);
                gc.strokeLine(cx - r, cy, cx + r, cy);

                gc.save();
                gc.beginPath();
                gc.moveTo(cx, cy);
                gc.arc(cx, cy, r, r, Math.toDegrees(-angle[0]), 30);
                gc.closePath();
                gc.setFill(Color.rgb(68, 184, 255, 0.18));
                gc.fill();
                gc.restore();

                for (double[] b : blips) {
                    double bx = cx + Math.cos(b[0]) * b[1] * r;
                    double by = cy + Math.sin(b[0]) * b[1] * r;
                    double diff = Math.abs(((angle[0] - b[0] + Math.PI * 4) % (Math.PI * 2)));
                    double alpha = diff < 0.6 ? 1.0 : 0.15;
                    gc.setFill(Color.rgb(255, 209, 102, alpha));
                    gc.fillOval(bx - 3, by - 3, 6, 6);
                }
            }
        };
        timer.start();

        StackPane wrap = new StackPane(canvas);
        wrap.setMouseTransparent(true);
        wrap.setPrefSize(size, size);
        wrap.setMaxSize(size, size);
        return wrap;
    }
}
