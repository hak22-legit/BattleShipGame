package com.battleship.view.decor;

import javafx.animation.RotateTransition;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

/**
 * a slow, continuously spinning, semi-transparent compass rose watermark.
 * one visual effect, one class (srp — extracted from the former decorutil
 * god object).
 */
public final class CompassWatermark {

    private CompassWatermark() { }

    /**
     * creates the spinning compass rose meant to sit behind other content
     * (add it as the first child of a stackpane). returns null if the art
     * asset couldn't be loaded, so callers can skip it cleanly.
     */
    public static ImageView compassWatermark(double size) {
        Image compass = com.battleship.view.ImageResources.ui("compass-rose");
        if (compass == null) return null;

        ImageView iv = new ImageView(compass);
        iv.setFitWidth(size);
        iv.setFitHeight(size);
        iv.setPreserveRatio(true);
        iv.setOpacity(0.09);
        iv.setMouseTransparent(true);

        RotateTransition spin = new RotateTransition(Duration.seconds(120), iv);
        spin.setByAngle(360);
        spin.setCycleCount(RotateTransition.INDEFINITE);
        spin.play();

        return iv;
    }
}
