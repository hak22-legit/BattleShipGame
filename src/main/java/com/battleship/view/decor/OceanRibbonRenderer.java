package com.battleship.view.decor;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * a wide, very faint ribbon of drifting sine-wave "ocean" lines meant to sit
 * behind menu content (add as the first child of a stackpane). one visual
 * effect, one class (srp — extracted from the former decorutil god object).
 */
public final class OceanRibbonRenderer {

    private OceanRibbonRenderer() { }

    /** builds the ribbon canvas; purely decorative and mouse-transparent. */
    public static Canvas animatedOceanRibbon(double width, double height) {
        Canvas canvas = new Canvas(width, height);
        canvas.setMouseTransparent(true);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        Color[] shades = {
                Color.rgb(68, 184, 255, 0.10),
                Color.rgb(68, 184, 255, 0.06),
                Color.rgb(255, 209, 102, 0.05)
        };

        final double[] t = {0};
        AnimationTimer timer = new AnimationTimer() {
            private long last = -1;

            @Override
            public void handle(long now) {
                if (last < 0) last = now;
                double dt = (now - last) / 1_000_000_000.0;
                last = now;
                t[0] += dt * 0.35;

                gc.clearRect(0, 0, width, height);
                double midY = height / 2.0;
                for (int line = 0; line < shades.length; line++) {
                    gc.setStroke(shades[line]);
                    gc.setLineWidth(2);
                    gc.beginPath();
                    double amp = 10 + line * 6;
                    double freq = 0.015 - line * 0.002;
                    double phase = t[0] * (1.2 + line * 0.4) + line * 1.7;
                    double yOffset = line * 14 - 14;
                    for (double x = 0; x <= width; x += 4) {
                        double y = midY + yOffset + Math.sin(x * freq + phase) * amp;
                        if (x == 0) gc.moveTo(x, y); else gc.lineTo(x, y);
                    }
                    gc.stroke();
                }
            }
        };
        timer.start();

        return canvas;
    }
}
