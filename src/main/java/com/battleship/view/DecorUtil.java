package com.battleship.view;

import com.battleship.view.decor.CompassWatermark;
import com.battleship.view.decor.OceanRibbonRenderer;
import com.battleship.view.decor.OceanSceneRenderer;
import com.battleship.view.decor.RadarSweepRenderer;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

/**
 * compatibility facade over the focused decorative renderers now living in
 * {@link com.battleship.view.decor} (fixes the former god object: each visual
 * effect has its own single-responsibility class). this class only forwards
 * calls, so existing screens keep their concise one-line usage.
 */
final class DecorUtil {

    private DecorUtil() { }

    /** @see compasswatermark#compasswatermark(double) */
    static ImageView compassWatermark(double size) {
        return CompassWatermark.compassWatermark(size);
    }

    /** @see radarsweeprenderer#animatedradarsweep(double) */
    static StackPane animatedRadarSweep(double size) {
        return RadarSweepRenderer.animatedRadarSweep(size);
    }

    /** @see oceanribbonrenderer#animatedoceanribbon(double, double) */
    static Canvas animatedOceanRibbon(double width, double height) {
        return OceanRibbonRenderer.animatedOceanRibbon(width, height);
    }

    /** @see oceanscenerenderer#animatedoceanscene(region) */
    static Canvas animatedOceanScene(Region sizeSource) {
        return OceanSceneRenderer.animatedOceanScene(sizeSource);
    }

    /** @see oceanscenerenderer#animatedoceanscene(region, double) */
    static Canvas animatedOceanScene(Region sizeSource, double horizonFrac) {
        return OceanSceneRenderer.animatedOceanScene(sizeSource, horizonFrac);
    }

    /** @see oceanscenerenderer#lightseascene(region) */
    static Canvas lightSeaScene(Region sizeSource) {
        return OceanSceneRenderer.lightSeaScene(sizeSource);
    }
}
