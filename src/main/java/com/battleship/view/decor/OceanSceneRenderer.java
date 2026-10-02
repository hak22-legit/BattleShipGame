package com.battleship.view.decor;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

/**
 * full-bleed animated ocean backdrops (night scene and daytime "light sea").
 * one visual family, one class (srp — extracted from the former decorutil
 * god object). both backdrops bind their size to a region so they always
 * cover the full window, and are purely decorative / mouse-transparent.
 */
public final class OceanSceneRenderer {

    private OceanSceneRenderer() { }

    /** night ocean with the default horizon (40% sky). */
    public static Canvas animatedOceanScene(Region sizeSource) {
        return animatedOceanScene(sizeSource, 0.40);
    }

    /**
     * a full painted night-ocean backdrop: gradient sky, a glowing moon,
     * scattered stars, a horizon line, and several layers of filled,
     * parallaxing swells with moonlit shimmer on the water.
     */
    public static Canvas animatedOceanScene(Region sizeSource, double horizonFrac) {
        Canvas canvas = new Canvas();
        canvas.widthProperty().bind(sizeSource.widthProperty());
        canvas.heightProperty().bind(sizeSource.heightProperty());
        canvas.setMouseTransparent(true);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // star field stored as fractional coordinates (0..1) so it rescales
        // cleanly with the canvas instead of being tied to one fixed size.
        java.util.Random starRng = new java.util.Random(42);
        double[][] starsFrac = new double[46][3];
        for (int i = 0; i < starsFrac.length; i++) {
            starsFrac[i][0] = starRng.nextDouble();
            starsFrac[i][1] = starRng.nextDouble() * 0.92;
            starsFrac[i][2] = starRng.nextDouble();
        }

        // swell layers, back to front: [speed, amplitude, wavelength, y-position(0..1 of sea band), base opacity, warm-tint]
        double[][] layers = {
                {0.12, 8, 220, 0.10, 0.55, 0},
                {0.20, 11, 170, 0.30, 0.65, 0},
                {0.30, 15, 140, 0.52, 0.78, 0.15},
                {0.42, 19, 110, 0.76, 0.92, 0.25},
        };
        Color deepSea = Color.rgb(8, 26, 44);
        Color midSea = Color.rgb(13, 42, 66);

        final double[] t = {0};
        AnimationTimer timer = new AnimationTimer() {
            private long last = -1;

            @Override
            public void handle(long now) {
                if (last < 0) last = now;
                double dt = (now - last) / 1_000_000_000.0;
                last = now;
                t[0] += dt;

                double width = canvas.getWidth();
                double height = canvas.getHeight();
                if (width <= 0 || height <= 0) return;

                double horizonY = height * horizonFrac;
                boolean showSky = horizonY > 4;
                double moonX = width * 0.78;
                double moonY = horizonY * 0.38;
                double moonR = Math.max(22, Math.min(46, width * 0.036));

                gc.clearRect(0, 0, width, height);

                if (showSky) {
                    // sky.
                    LinearGradient sky = new LinearGradient(0, 0, 0, horizonY, false, CycleMethod.NO_CYCLE,
                            new Stop(0, Color.rgb(3, 9, 20)),
                            new Stop(0.6, Color.rgb(7, 20, 38)),
                            new Stop(1, Color.rgb(13, 34, 55)));
                    gc.setFill(sky);
                    gc.fillRect(0, 0, width, horizonY);

                    // stars (gentle twinkle).
                    for (double[] s : starsFrac) {
                        double tw = 0.35 + 0.45 * (0.5 + 0.5 * Math.sin(t[0] * 1.4 + s[2] * 20));
                        gc.setFill(Color.rgb(230, 240, 255, tw * 0.7));
                        double r = 0.6 + s[2] * 1.1;
                        gc.fillOval(s[0] * width, s[1] * horizonY, r, r);
                    }

                    // moon.
                    RadialGradient moonGlow = new RadialGradient(0, 0, moonX, moonY, moonR * 5.2,
                            false, CycleMethod.NO_CYCLE,
                            new Stop(0, Color.rgb(255, 244, 214, 0.35)),
                            new Stop(0.35, Color.rgb(255, 232, 180, 0.10)),
                            new Stop(1, Color.rgb(255, 232, 180, 0.0)));
                    RadialGradient moonBody = new RadialGradient(0, 0, moonX - moonR * 0.3, moonY - moonR * 0.3, moonR * 1.6,
                            false, CycleMethod.NO_CYCLE,
                            new Stop(0, Color.rgb(255, 250, 235, 0.95)),
                            new Stop(1, Color.rgb(255, 224, 168, 0.85)));
                    gc.setFill(moonGlow);
                    gc.fillOval(moonX - moonR * 5.2, moonY - moonR * 5.2, moonR * 10.4, moonR * 10.4);
                    gc.setFill(moonBody);
                    gc.fillOval(moonX - moonR, moonY - moonR, moonR * 2, moonR * 2);
                }

                // base sea fill — spans the whole canvas when there's no sky band.
                LinearGradient sea = new LinearGradient(0, horizonY, 0, height, false, CycleMethod.NO_CYCLE,
                        new Stop(0, midSea),
                        new Stop(1, deepSea));
                gc.setFill(sea);
                gc.fillRect(0, horizonY, width, height - horizonY);

                if (showSky) {
                    // soft horizon glow where the moon meets the water.
                    gc.setFill(Color.rgb(255, 232, 190, 0.10));
                    gc.fillRect(0, horizonY, width, 3);

                    // moonlit shimmer column on the water — thin broken highlights.
                    for (int i = 0; i < 26; i++) {
                        double frac = i / 26.0;
                        double y = horizonY + frac * (height - horizonY);
                        double spread = 10 + frac * 46;
                        double wobble = Math.sin(t[0] * 2.2 + i * 1.3) * spread * 0.5;
                        double segW = 10 + frac * 26;
                        double alpha = (1 - frac) * 0.22;
                        gc.setFill(Color.rgb(255, 240, 205, alpha));
                        gc.fillRoundRect(moonX + wobble - segW / 2.0, y, segW, 1.6 + frac * 1.4, 4, 4);
                    }
                }

                // layered filled swells, drawn back (dim) to front (brighter, warmer near shore).
                for (double[] layer : layers) {
                    double speed = layer[0], amp = layer[1], wavelen = layer[2];
                    double yFrac = layer[3], baseAlpha = layer[4], warm = layer[5];
                    double baseY = horizonY + (height - horizonY) * yFrac;
                    double phase = t[0] * speed * 6.0;

                    Color base = deepSea.interpolate(Color.rgb(68, 184, 255), 0.10 + yFrac * 0.10)
                            .interpolate(Color.rgb(255, 209, 102), warm * 0.12);
                    Color fillColor = Color.color(base.getRed(), base.getGreen(), base.getBlue(), baseAlpha);
                    Color crestColor = base.brighter().deriveColor(0, 1, 1, Math.min(1, baseAlpha + 0.15));

                    gc.beginPath();
                    gc.moveTo(0, height + 4);
                    gc.lineTo(0, baseY);
                    for (double x = 0; x <= width; x += 6) {
                        double y = baseY + Math.sin((x / wavelen) * 2 * Math.PI + phase) * amp
                                + Math.sin((x / (wavelen * 2.7)) + phase * 1.6) * amp * 0.3;
                        gc.lineTo(x, y);
                    }
                    gc.lineTo(width, height + 4);
                    gc.closePath();
                    gc.setFill(fillColor);
                    gc.fill();

                    // a thin brighter crest line riding the top of this swell.
                    gc.setStroke(crestColor);
                    gc.setLineWidth(1.2);
                    gc.beginPath();
                    for (double x = 0; x <= width; x += 6) {
                        double y = baseY + Math.sin((x / wavelen) * 2 * Math.PI + phase) * amp
                                + Math.sin((x / (wavelen * 2.7)) + phase * 1.6) * amp * 0.3;
                        if (x == 0) gc.moveTo(x, y); else gc.lineTo(x, y);
                    }
                    gc.stroke();
                }

                // subtle vignette so foreground ui text stays readable.
                gc.setFill(Color.rgb(2, 6, 12, 0.28));
                gc.fillRect(0, 0, width, height * 0.16);
            }
        };
        timer.start();

        return canvas;
    }

    /**
     * a brighter, daytime "light sea" backdrop — no moon/stars, a soft sun
     * glow instead, and lighter sky/water tones than {@link #animatedoceanscene}.
     * same sizing contract: bind to a region and add as the first stackpane child.
     */
    public static Canvas lightSeaScene(Region sizeSource) {
        Canvas canvas = new Canvas();
        canvas.widthProperty().bind(sizeSource.widthProperty());
        canvas.heightProperty().bind(sizeSource.heightProperty());
        canvas.setMouseTransparent(true);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        double[][] layers = {
                {0.12, 8, 220, 0.10, 0.45, 0},
                {0.20, 11, 170, 0.32, 0.55, 0.05},
                {0.30, 15, 140, 0.56, 0.68, 0.10},
                {0.42, 19, 110, 0.80, 0.82, 0.16},
        };
        Color deepSea = Color.rgb(18, 66, 104);
        Color midSea = Color.rgb(30, 96, 140);

        final double[] t = {0};
        AnimationTimer timer = new AnimationTimer() {
            private long last = -1;

            @Override
            public void handle(long now) {
                if (last < 0) last = now;
                double dt = (now - last) / 1_000_000_000.0;
                last = now;
                t[0] += dt;

                double width = canvas.getWidth();
                double height = canvas.getHeight();
                if (width <= 0 || height <= 0) return;

                double horizonY = height * 0.38;
                double sunX = width * 0.80;
                double sunY = horizonY * 0.34;
                double sunR = Math.max(20, Math.min(40, width * 0.03));

                gc.clearRect(0, 0, width, height);

                LinearGradient sky = new LinearGradient(0, 0, 0, horizonY, false, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.rgb(22, 58, 92)),
                        new Stop(0.6, Color.rgb(34, 82, 122)),
                        new Stop(1, Color.rgb(58, 118, 160)));
                gc.setFill(sky);
                gc.fillRect(0, 0, width, horizonY);

                // sun glow.
                RadialGradient sunGlow = new RadialGradient(0, 0, sunX, sunY, sunR * 5.0,
                        false, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.rgb(255, 244, 214, 0.4)),
                        new Stop(0.35, Color.rgb(255, 226, 170, 0.14)),
                        new Stop(1, Color.rgb(255, 226, 170, 0.0)));
                RadialGradient sunBody = new RadialGradient(0, 0, sunX, sunY, sunR * 1.4,
                        false, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.rgb(255, 250, 235, 0.95)),
                        new Stop(1, Color.rgb(255, 214, 140, 0.85)));
                gc.setFill(sunGlow);
                gc.fillOval(sunX - sunR * 5.0, sunY - sunR * 5.0, sunR * 10.0, sunR * 10.0);
                gc.setFill(sunBody);
                gc.fillOval(sunX - sunR, sunY - sunR, sunR * 2, sunR * 2);

                // a few soft daylight clouds drifting slowly.
                gc.setFill(Color.rgb(255, 255, 255, 0.10));
                for (int i = 0; i < 4; i++) {
                    double cx = ((i * 260 + t[0] * 6) % (width + 200)) - 100;
                    double cy = horizonY * (0.18 + i * 0.16);
                    gc.fillOval(cx, cy, 90, 22);
                    gc.fillOval(cx + 30, cy - 8, 70, 20);
                }

                // base sea fill beneath the horizon.
                LinearGradient sea = new LinearGradient(0, horizonY, 0, height, false, CycleMethod.NO_CYCLE,
                        new Stop(0, midSea),
                        new Stop(1, deepSea));
                gc.setFill(sea);
                gc.fillRect(0, horizonY, width, height - horizonY);

                // soft horizon glow line.
                gc.setFill(Color.rgb(255, 240, 210, 0.16));
                gc.fillRect(0, horizonY, width, 3);

                // sunlit shimmer column on the water.
                for (int i = 0; i < 26; i++) {
                    double frac = i / 26.0;
                    double y = horizonY + frac * (height - horizonY);
                    double spread = 10 + frac * 46;
                    double wobble = Math.sin(t[0] * 2.2 + i * 1.3) * spread * 0.5;
                    double segW = 10 + frac * 26;
                    double alpha = (1 - frac) * 0.20;
                    gc.setFill(Color.rgb(255, 240, 205, alpha));
                    gc.fillRoundRect(sunX + wobble - segW / 2.0, y, segW, 1.6 + frac * 1.4, 4, 4);
                }

                // layered filled swells, brighter cyan tones for the daylight feel.
                for (double[] layer : layers) {
                    double speed = layer[0], amp = layer[1], wavelen = layer[2];
                    double yFrac = layer[3], baseAlpha = layer[4], warm = layer[5];
                    double baseY = horizonY + (height - horizonY) * yFrac;
                    double phase = t[0] * speed * 6.0;

                    Color base = deepSea.interpolate(Color.rgb(120, 210, 255), 0.16 + yFrac * 0.14)
                            .interpolate(Color.rgb(255, 220, 160), warm * 0.10);
                    Color fillColor = Color.color(base.getRed(), base.getGreen(), base.getBlue(), baseAlpha);
                    Color crestColor = base.brighter().deriveColor(0, 1, 1, Math.min(1, baseAlpha + 0.15));

                    gc.beginPath();
                    gc.moveTo(0, height + 4);
                    gc.lineTo(0, baseY);
                    for (double x = 0; x <= width; x += 6) {
                        double y = baseY + Math.sin((x / wavelen) * 2 * Math.PI + phase) * amp
                                + Math.sin((x / (wavelen * 2.7)) + phase * 1.6) * amp * 0.3;
                        gc.lineTo(x, y);
                    }
                    gc.lineTo(width, height + 4);
                    gc.closePath();
                    gc.setFill(fillColor);
                    gc.fill();

                    gc.setStroke(crestColor);
                    gc.setLineWidth(1.2);
                    gc.beginPath();
                    for (double x = 0; x <= width; x += 6) {
                        double y = baseY + Math.sin((x / wavelen) * 2 * Math.PI + phase) * amp
                                + Math.sin((x / (wavelen * 2.7)) + phase * 1.6) * amp * 0.3;
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
