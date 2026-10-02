package com.battleship.view;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

/**
 * Cinematic Battleship main menu title screen.
 * Presents a unified hero composition:
 *   [ Captain 1 ] [ Captain 2 ] (slightly overlapping transparent silhouettes)
 *               BATTLESHIP (large military emblem overlapping lower captains)
 *            ⚓ NAVAL COMMAND ⚓
 *          ┌───────────────────┐
 *          │ ⚓      PLAY     > │
 *          ├───────────────────┤
 *          │ ?  HOW TO PLAY  > │
 *          ├───────────────────┤
 *          │ ⚙   OPTIONS    > │
 *          ├───────────────────┤
 *          │ ✕     EXIT     > │
 *          └───────────────────┘
 */
public class MainMenuView {

    private final ViewNavigator nav;
    private StackPane root;

    public MainMenuView(ViewNavigator nav) {
        this.nav = nav;
    }

    public StackPane build() {
        root = new StackPane();
        root.setAlignment(Pos.CENTER);

        // -------------------------------------------------------------
        // Layer 0: Background & Atmosphere (100vw x 100vh cover, no distortion)
        // -------------------------------------------------------------
        Image bg = ImageResources.background("menu-background");
        if (bg != null) {
            BackgroundImage bi = new BackgroundImage(
                    bg,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundPosition.CENTER,
                    new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, false, true)
            );
            root.setBackground(new Background(bi));

            // Background ImageView with aspect-ratio preserving cover scaling
            ImageView bgView = new ImageView(bg);
            bgView.setPreserveRatio(true);
            bgView.setSmooth(true);
            bgView.setMouseTransparent(true);

            DoubleBinding bgScaleBinding = Bindings.createDoubleBinding(() -> {
                double rw = root.getWidth();
                double rh = root.getHeight();
                if (rw <= 0 || rh <= 0 || bg.getWidth() <= 0 || bg.getHeight() <= 0) return 1.0;
                double sx = rw / bg.getWidth();
                double sy = rh / bg.getHeight();
                return Math.max(sx, sy);
            }, root.widthProperty(), root.heightProperty());

            bgView.scaleXProperty().bind(bgScaleBinding);
            bgView.scaleYProperty().bind(bgScaleBinding);
            root.getChildren().add(bgView);
        } else {
            Canvas ocean = DecorUtil.animatedOceanScene(root);
            root.getChildren().add(ocean);
        }

        // Layer 1: Subtle Dark Overlay (linear-gradient rgba(2,10,22,0.15) to rgba(2,10,22,0.35))
        Region darkOverlay = new Region();
        darkOverlay.setStyle("-fx-background-color: linear-gradient(to bottom, rgba(2, 10, 22, 0.15) 0%, rgba(2, 10, 22, 0.35) 100%);");
        darkOverlay.prefWidthProperty().bind(root.widthProperty());
        darkOverlay.prefHeightProperty().bind(root.heightProperty());
        darkOverlay.setMouseTransparent(true);
        root.getChildren().add(darkOverlay);

        // Horizon subtle ocean mist ribbon
        Canvas ribbon = DecorUtil.animatedOceanRibbon(1600, 120);
        if (ribbon != null) {
            ribbon.setOpacity(0.08);
            ribbon.setMouseTransparent(true);
            StackPane.setAlignment(ribbon, Pos.CENTER);
            root.getChildren().add(ribbon);
        }

        // -------------------------------------------------------------
        // Layer 2: Hero Composition (Captains + Logo + Subtitle + Menu)
        // -------------------------------------------------------------
        VBox heroComposition = new VBox(0);
        heroComposition.setAlignment(Pos.TOP_CENTER);
        heroComposition.setFillWidth(false);

        // Responsive top padding: approximately 4.5% to 5.5% of viewport height (~5vh)
        Runnable updateHeroPadding = () -> {
            double h = root.getHeight();
            if (h <= 0) h = 768;
            double topPad = Math.max(28.0, Math.min(54.0, h * 0.048));
            heroComposition.setPadding(new Insets(topPad, 0, 0, 0));
        };
        root.heightProperty().addListener((obs, oldVal, newVal) -> updateHeroPadding.run());
        updateHeroPadding.run();

        // -------------------------------------------------------------
        // 1. Captains: Transparent asset directly, no box, no background
        // -------------------------------------------------------------
        DoubleBinding captainHeightBinding = Bindings.createDoubleBinding(() -> {
            double h = root.getHeight();
            if (h <= 0) h = 768;
            double target = h * 0.36;
            return Math.max(260.0, Math.min(420.0, target));
        }, root.heightProperty());

        // Golden rim glow + deep drop shadow directly on captain silhouette
        DropShadow goldenRim = new DropShadow(BlurType.GAUSSIAN, Color.rgb(218, 170, 60, 0.22), 20, 0.15, 0, 0);
        DropShadow captainShadow = new DropShadow(BlurType.GAUSSIAN, Color.rgb(0, 0, 0, 0.65), 26, 0.25, 0, 8);
        goldenRim.setInput(captainShadow);

        Node captainsNode;
        Image captainsImg = ImageResources.background("captains");
        if (captainsImg != null) {
            ImageView captainsView = new ImageView(captainsImg);
            captainsView.fitHeightProperty().bind(captainHeightBinding);
            captainsView.setPreserveRatio(true);
            captainsView.setSmooth(true);
            captainsView.setEffect(goldenRim);
            captainsView.setMouseTransparent(true);
            captainsNode = captainsView;
        } else {
            // Fallback: load officer-left and officer-right side-by-side with slight overlap
            Image leftImg = ImageResources.background("officer-left");
            Image rightImg = ImageResources.background("officer-right");
            ImageView leftView = new ImageView();
            ImageView rightView = new ImageView();
            if (leftImg != null) leftView.setImage(leftImg);
            if (rightImg != null) rightView.setImage(rightImg);
            leftView.fitHeightProperty().bind(captainHeightBinding);
            leftView.setPreserveRatio(true);
            leftView.setSmooth(true);
            rightView.fitHeightProperty().bind(captainHeightBinding);
            rightView.setPreserveRatio(true);
            rightView.setSmooth(true);

            HBox fallbackBox = new HBox(-45, leftView, rightView);
            fallbackBox.setAlignment(Pos.CENTER);
            fallbackBox.setEffect(goldenRim);
            fallbackBox.setMouseTransparent(true);
            captainsNode = fallbackBox;
        }

        // -------------------------------------------------------------
        // 2. Battleship Logo: Large military emblem slightly overlapping lower captains
        // -------------------------------------------------------------
        Image logoImg = ImageResources.ui("logo-battleship");
        ImageView logoView = null;
        if (logoImg != null) {
            logoView = new ImageView(logoImg);
            DoubleBinding logoWidthBinding = Bindings.createDoubleBinding(() -> {
                double w = root.getWidth();
                double h = root.getHeight();
                if (w <= 0) w = 1366;
                if (h <= 0) h = 768;
                double maxAllowedW = Math.max(260.0, w - 40.0);
                double target = Math.min(w * 0.25, h * 0.42);
                return Math.max(320.0, Math.min(Math.min(480.0, maxAllowedW), target));
            }, root.widthProperty(), root.heightProperty());

            logoView.fitWidthProperty().bind(logoWidthBinding);
            logoView.setPreserveRatio(true);
            logoView.setSmooth(true);
            logoView.setEffect(new DropShadow(BlurType.GAUSSIAN, Color.rgb(0, 0, 0, 0.75), 20, 0.35, 0, 6));
        }

        Label fallbackTitle = null;
        if (logoView == null) {
            fallbackTitle = new Label("BATTLESHIP");
            fallbackTitle.setFont(Font.font("Arial Black", FontWeight.BOLD, 46));
            fallbackTitle.getStyleClass().add("app-title");
        }

        // -------------------------------------------------------------
        // 3. Subtitle: ⚓ NAVAL COMMAND ⚓
        // -------------------------------------------------------------
        Region leftLine = new Region();
        leftLine.getStyleClass().add("menu-subtitle-line");
        leftLine.setPrefWidth(44);

        Label subtitle = new Label("\u2693  NAVAL COMMAND  \u2693");
        subtitle.getStyleClass().add("menu-subtitle-text");

        Region rightLine = new Region();
        rightLine.getStyleClass().add("menu-subtitle-line");
        rightLine.setPrefWidth(44);

        HBox subtitleBox = new HBox(12, leftLine, subtitle, rightLine);
        subtitleBox.setAlignment(Pos.CENTER);

        VBox titleBox = new VBox(4);
        titleBox.setAlignment(Pos.CENTER);
        if (logoView != null) {
            titleBox.getChildren().add(logoView);
        } else if (fallbackTitle != null) {
            titleBox.getChildren().add(fallbackTitle);
        }
        titleBox.getChildren().add(subtitleBox);

        // Intentional poster overlap: Logo emblem overlaps the lower body of captains by ~6vh
        Runnable updateLogoOverlap = () -> {
            double h = root.getHeight();
            if (h <= 0) h = 768;
            double overlap = -Math.max(38.0, Math.min(68.0, h * 0.06));
            VBox.setMargin(titleBox, new Insets(overlap, 0, 0, 0));
        };
        root.heightProperty().addListener((obs, oldVal, newVal) -> updateLogoOverlap.run());
        updateLogoOverlap.run();

        // -------------------------------------------------------------
        // 4. Menu: Elevated, begins shortly below NAVAL COMMAND
        // -------------------------------------------------------------
        DoubleBinding menuWidthBinding = Bindings.createDoubleBinding(() -> {
            double w = root.getWidth();
            if (w <= 0) w = 1366;
            double maxAllowedW = Math.max(260.0, w - 48.0);
            double target = w * 0.21;
            return Math.max(320.0, Math.min(Math.min(400.0, maxAllowedW), target));
        }, root.widthProperty());

        DoubleBinding playHeightBinding = Bindings.createDoubleBinding(() -> {
            double h = root.getHeight();
            if (h <= 0) h = 768;
            return (h >= 900) ? 58.0 : (h >= 750 ? 52.0 : 46.0);
        }, root.heightProperty());

        DoubleBinding navHeightBinding = Bindings.createDoubleBinding(() -> {
            double h = root.getHeight();
            if (h <= 0) h = 768;
            return (h >= 900) ? 52.0 : (h >= 750 ? 46.0 : 42.0);
        }, root.heightProperty());

        Button play = navButton("\u2693", "PLAY", true, menuWidthBinding, playHeightBinding);
        play.setOnAction(e -> { nav.getAudio().playClick(); nav.showModeSelect(); });

        Button howToPlay = navButton("?", "HOW TO PLAY", false, menuWidthBinding, navHeightBinding);
        howToPlay.setOnAction(e -> { nav.getAudio().playClick(); showHowToPlay(); });

        Button options = navButton("\u2699", "OPTIONS", false, menuWidthBinding, navHeightBinding);
        options.setOnAction(e -> { nav.getAudio().playClick(); showOptions(); });

        Button exit = navButton("\u2715", "EXIT", false, menuWidthBinding, navHeightBinding);
        exit.setOnAction(e -> { nav.getAudio().playClick(); nav.getStage().close(); });

        VBox buttonBox = new VBox(10, play, howToPlay, options, exit);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.getStyleClass().add("menu-card");

        // Dynamic spacing between Subtitle and Menu
        Runnable updateMenuMargin = () -> {
            double h = root.getHeight();
            if (h <= 0) h = 768;
            double menuGap = (h >= 900) ? 14.0 : 10.0;
            VBox.setMargin(buttonBox, new Insets(menuGap, 0, 0, 0));
        };
        root.heightProperty().addListener((obs, oldVal, newVal) -> updateMenuMargin.run());
        updateMenuMargin.run();

        // Assemble unified hero composition
        heroComposition.getChildren().addAll(captainsNode, titleBox, buttonBox);

        // -------------------------------------------------------------
        // Subtle Footer Note (Fleet Command status)
        // -------------------------------------------------------------
        Label footer = new Label("FLEET COMMAND NETWORK \u2022 READY FOR ORDERS");
        footer.getStyleClass().add("menu-footer-text");
        StackPane.setAlignment(footer, Pos.BOTTOM_CENTER);
        StackPane.setMargin(footer, new Insets(0, 0, 10, 0));

        root.getChildren().addAll(heroComposition, footer);

        // -------------------------------------------------------------
        // Cinematic Animations & Entrance Transitions
        // -------------------------------------------------------------
        setupAnimations(heroComposition, captainsNode, play);

        return root;
    }

    private Button navButton(String iconText, String labelText, boolean featured,
                              DoubleBinding widthBinding, DoubleBinding heightBinding) {
        Button b = new Button();
        b.setUserData(labelText);
        b.setId("btn-" + labelText.toLowerCase().replace(" ", "-"));
        b.prefWidthProperty().bind(widthBinding);
        b.prefHeightProperty().bind(heightBinding);
        b.getStyleClass().add(featured ? "menu-play-button" : "menu-nav-button");

        BorderPane content = new BorderPane();
        content.setMouseTransparent(true);
        // Ensure content spans button width with internal margins
        content.prefWidthProperty().bind(Bindings.max(100.0, b.widthProperty().subtract(32)));

        Label icon = new Label(iconText);
        icon.getStyleClass().add(featured ? "menu-btn-icon-featured" : "menu-btn-icon");

        Label text = new Label(labelText);
        text.getStyleClass().add(featured ? "menu-btn-text-featured" : "menu-btn-text");

        Label chevron = new Label("\u203A");
        chevron.getStyleClass().add(featured ? "menu-btn-chevron-featured" : "menu-btn-chevron");

        BorderPane.setAlignment(icon, Pos.CENTER_LEFT);
        BorderPane.setAlignment(text, Pos.CENTER);
        BorderPane.setAlignment(chevron, Pos.CENTER_RIGHT);

        BorderPane.setMargin(icon, new Insets(0, 0, 0, 16));
        BorderPane.setMargin(chevron, new Insets(0, 16, 0, 0));

        content.setLeft(icon);
        content.setCenter(text);
        content.setRight(chevron);

        b.setGraphic(content);

        // Smooth translateX(3px) on hover with 220ms duration
        TranslateTransition hoverTransition = new TranslateTransition(Duration.millis(220), b);
        b.setOnMouseEntered(e -> {
            hoverTransition.stop();
            hoverTransition.setToX(3.0);
            hoverTransition.play();
        });
        b.setOnMouseExited(e -> {
            hoverTransition.stop();
            hoverTransition.setToX(0.0);
            hoverTransition.play();
        });

        return b;
    }

    private void setupAnimations(VBox heroComposition, Node captainsNode, Button playButton) {
        // 1. Unified hero entrance fade and subtle rise (starts at 0.05, animates to 1.0)
        heroComposition.setOpacity(0.05);
        heroComposition.setTranslateY(14);

        FadeTransition heroFade = new FadeTransition(Duration.millis(500), heroComposition);
        heroFade.setFromValue(0.05);
        heroFade.setToValue(1.0);

        TranslateTransition heroRise = new TranslateTransition(Duration.millis(500), heroComposition);
        heroRise.setFromY(14);
        heroRise.setToY(0);

        ParallelTransition entrance = new ParallelTransition(heroFade, heroRise);
        entrance.play();

        // 2. Gentle ambient ocean swell idle motion on captains
        TranslateTransition captainIdle = new TranslateTransition(Duration.millis(3600), captainsNode);
        captainIdle.setByY(-3.5);
        captainIdle.setAutoReverse(true);
        captainIdle.setCycleCount(Animation.INDEFINITE);
        captainIdle.setInterpolator(Interpolator.EASE_BOTH);
        captainIdle.play();

        // 3. Subtle ambient breathing pulse on PLAY CTA button
        DropShadow playGlow = new DropShadow(BlurType.GAUSSIAN, Color.rgb(255, 209, 102, 0.45), 20, 0.3, 0, 2);
        playButton.setEffect(playGlow);

        Timeline pulse = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(playGlow.radiusProperty(), 18),
                        new KeyValue(playGlow.spreadProperty(), 0.25),
                        new KeyValue(playGlow.colorProperty(), Color.rgb(255, 209, 102, 0.40))),
                new KeyFrame(Duration.millis(1800),
                        new KeyValue(playGlow.radiusProperty(), 28),
                        new KeyValue(playGlow.spreadProperty(), 0.40),
                        new KeyValue(playGlow.colorProperty(), Color.rgb(255, 219, 128, 0.65))),
                new KeyFrame(Duration.millis(3600),
                        new KeyValue(playGlow.radiusProperty(), 18),
                        new KeyValue(playGlow.spreadProperty(), 0.25),
                        new KeyValue(playGlow.colorProperty(), Color.rgb(255, 209, 102, 0.40)))
        );
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.setAutoReverse(false);
        pulse.play();
    }

    private void showHowToPlay() {
        final StackPane[] holder = new StackPane[1];
        holder[0] = MenuOverlays.howToPlay(nav.getAudio(),
                () -> root.getChildren().remove(holder[0]));
        root.getChildren().add(holder[0]);
    }

    private void showOptions() {
        final StackPane[] holder = new StackPane[1];
        holder[0] = MenuOverlays.options(nav.getAudio(),
                () -> root.getChildren().remove(holder[0]));
        root.getChildren().add(holder[0]);
    }
}
