package com.battleship.view;

import com.battleship.controller.GameController;
import com.battleship.model.GameMode;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * Cinematic Naval Strategy "SELECT GAME MODE" screen.
 * Reproduces the visual hierarchy and aesthetics from the naval command reference:
 * - Upper Center: Officer captains positioned behind the 3D metallic gold title & naval subtitle
 * - Three command cards: [ VS AI ] (Single Player) | [ HOTSEAT ] (Same Device, Active) | [ ONLINE ] (LAN/QR Match)
 * - Tactical artwork windows, military pill badges, radio difficulty selectors, gold bordered SELECT buttons
 * - Centered ← BACK button
 */
public class GameModeSelectView {

    private final ViewNavigator nav;
    private final GameController controller;

    private StackPane root;
    private GameMode selectedAiDifficulty = GameMode.AI_EASY;
    private String selectedOnlineSubmode = "LAN";
    private int activeCardIndex = 1; // 0 = AI, 1 = Hotseat (default focused), 2 = Online

    private final List<VBox> modeCards = new ArrayList<>();
    private Button easyBtn;
    private Button normalBtn;
    private Button hardBtn;
    private VBox lanChoice;
    private VBox qrChoice;

    public GameModeSelectView(ViewNavigator nav, GameController controller) {
        this.nav = nav;
        this.controller = controller;
    }

    public StackPane build() {
        root = new StackPane();
        root.setAlignment(Pos.CENTER);

        // -------------------------------------------------------------
        // Layer 0: Night Ocean Background with Cover Scaling
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

            ImageView bgView = new ImageView(bg);
            bgView.setPreserveRatio(true);
            bgView.setSmooth(true);
            bgView.setMouseTransparent(true);

            DoubleBinding bgScale = Bindings.createDoubleBinding(() -> {
                double rw = root.getWidth();
                double rh = root.getHeight();
                if (rw <= 0 || rh <= 0 || bg.getWidth() <= 0 || bg.getHeight() <= 0) return 1.0;
                return Math.max(rw / bg.getWidth(), rh / bg.getHeight());
            }, root.widthProperty(), root.heightProperty());

            bgView.scaleXProperty().bind(bgScale);
            bgView.scaleYProperty().bind(bgScale);
            root.getChildren().add(bgView);
        } else {
            javafx.scene.canvas.Canvas ocean = DecorUtil.animatedOceanScene(root);
            root.getChildren().add(ocean);
        }

        // Layer 1: Dark Navy Vignette Overlay (keeps cards as the strong visual focus)
        Region darkOverlay = new Region();
        darkOverlay.setStyle("-fx-background-color: linear-gradient(to bottom, rgba(2, 10, 22, 0.35) 0%, rgba(2, 10, 22, 0.65) 100%);");
        darkOverlay.prefWidthProperty().bind(root.widthProperty());
        darkOverlay.prefHeightProperty().bind(root.heightProperty());
        darkOverlay.setMouseTransparent(true);
        root.getChildren().add(darkOverlay);

        // -------------------------------------------------------------
        // Layer 2: Main Layout (Header + Cards + Back)
        // -------------------------------------------------------------
        VBox layout = new VBox();
        layout.setId("mode-select-layout");
        layout.setAlignment(Pos.TOP_CENTER);
        layout.setFillWidth(false);

        // Responsive top padding: ~2.5% to 4% of viewport height
        Runnable updateLayoutPadding = () -> {
            double h = root.getHeight();
            if (h <= 0) h = 768;
            double topPad = Math.max(14.0, Math.min(38.0, h * 0.035));
            layout.setPadding(new Insets(topPad, 16, 16, 16));
        };
        root.heightProperty().addListener((obs, oldVal, newVal) -> updateLayoutPadding.run());
        updateLayoutPadding.run();

        // 1. PAGE HEADER (Decorative Captains + Title + Subtitle)
        VBox header = createHeader();

        // 2. THREE GAME MODE PANELS
        HBox cardsBox = createCardsRow();

        // 3. BACK BUTTON
        Button back = new Button("\u2190  BACK");
        back.setId("btn-mode-back");
        back.getStyleClass().add("mode-back-btn");
        back.setOnAction(e -> {
            nav.getAudio().playClick();
            nav.showMainMenu();
        });

        // Responsive spacing between header, cards, and back button
        Runnable updateSpacing = () -> {
            double h = root.getHeight();
            if (h <= 0) h = 768;
            double gapCards = (h >= 900) ? 20.0 : 12.0;
            double gapBack = (h >= 900) ? 20.0 : 14.0;
            VBox.setMargin(cardsBox, new Insets(gapCards, 0, 0, 0));
            VBox.setMargin(back, new Insets(gapBack, 0, 0, 0));
        };
        root.heightProperty().addListener((obs, oldVal, newVal) -> updateSpacing.run());
        updateSpacing.run();

        layout.getChildren().addAll(header, cardsBox, back);

        // Responsive scroll wrapper for narrow mobile screens
        ScrollPane scrollPane = new ScrollPane(layout);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-padding: 0;");

        root.getChildren().add(scrollPane);

        // Entrance animation
        layout.setOpacity(0.05);
        layout.setTranslateY(14);
        FadeTransition fade = new FadeTransition(Duration.millis(420), layout);
        fade.setFromValue(0.05);
        fade.setToValue(1.0);
        TranslateTransition rise = new TranslateTransition(Duration.millis(420), layout);
        rise.setFromY(14);
        rise.setToY(0);
        fade.play();
        rise.play();

        // Set default active card styling
        updateActiveCardVisuals();

        return root;
    }

    private VBox createHeader() {
        VBox headerBox = new VBox(0);
        headerBox.setAlignment(Pos.CENTER);

        // Decorative Officer Captains sitting behind the title
        Image captainsImg = ImageResources.background("captains");
        ImageView captainsView = null;
        if (captainsImg != null) {
            captainsView = new ImageView(captainsImg);
            captainsView.setPreserveRatio(true);
            captainsView.setSmooth(true);
            captainsView.setMouseTransparent(true);

            DoubleBinding captainH = Bindings.createDoubleBinding(() -> {
                double h = root.getHeight();
                if (h <= 0) h = 768;
                return Math.max(85.0, Math.min(145.0, h * 0.13));
            }, root.heightProperty());

            captainsView.fitHeightProperty().bind(captainH);

            DropShadow captainGlow = new DropShadow(BlurType.GAUSSIAN, Color.rgb(218, 170, 60, 0.25), 18, 0.15, 0, 0);
            DropShadow captainShadow = new DropShadow(BlurType.GAUSSIAN, Color.rgb(0, 0, 0, 0.65), 20, 0.20, 0, 6);
            captainGlow.setInput(captainShadow);
            captainsView.setEffect(captainGlow);
            headerBox.getChildren().add(captainsView);
        }

        // Title and Subtitle Container
        VBox titleContainer = new VBox(4);
        titleContainer.setAlignment(Pos.CENTER);

        // Title overlapping lower chest of captains
        Label title = new Label("SELECT GAME MODE");
        title.getStyleClass().add("mode-select-title");

        DoubleBinding titleOverlap = Bindings.createDoubleBinding(() -> {
            double h = root.getHeight();
            if (h <= 0) h = 768;
            return -Math.max(16.0, Math.min(28.0, h * 0.026));
        }, root.heightProperty());
        title.translateYProperty().bind(titleOverlap);

        // Subtitle: ⚓ CHOOSE YOUR BATTLE ⚓
        Region leftLine = new Region();
        leftLine.getStyleClass().add("mode-select-subtitle-line");

        Label subtitle = new Label("\u2693  CHOOSE YOUR BATTLE  \u2693");
        subtitle.getStyleClass().add("mode-select-subtitle");

        Region rightLine = new Region();
        rightLine.getStyleClass().add("mode-select-subtitle-line");

        HBox subtitleRow = new HBox(12, leftLine, subtitle, rightLine);
        subtitleRow.setAlignment(Pos.CENTER);
        subtitleRow.translateYProperty().bind(titleOverlap);

        titleContainer.getChildren().addAll(title, subtitleRow);
        headerBox.getChildren().add(titleContainer);

        return headerBox;
    }

    private HBox createCardsRow() {
        HBox cardsBox = new HBox(22);
        cardsBox.setAlignment(Pos.CENTER);

        // Responsive card width & height
        DoubleBinding cardWidthBinding = Bindings.createDoubleBinding(() -> {
            double w = root.getWidth();
            if (w <= 0) w = 1366;
            double target = w * 0.24;
            return Math.max(295.0, Math.min(380.0, target));
        }, root.widthProperty());

        DoubleBinding cardHeightBinding = Bindings.createDoubleBinding(() -> {
            double h = root.getHeight();
            if (h <= 0) h = 768;
            double target = h * 0.58;
            return Math.max(435.0, Math.min(515.0, target));
        }, root.heightProperty());

        modeCards.clear();

        // 1. VS AI CARD
        VBox aiCard = createAiCard(cardWidthBinding, cardHeightBinding);
        modeCards.add(aiCard);

        // 2. HOTSEAT CARD (Default focused)
        VBox hotseatCard = createHotseatCard(cardWidthBinding, cardHeightBinding);
        modeCards.add(hotseatCard);

        // 3. ONLINE CARD
        VBox onlineCard = createOnlineCard(cardWidthBinding, cardHeightBinding);
        modeCards.add(onlineCard);

        cardsBox.getChildren().addAll(aiCard, hotseatCard, onlineCard);
        return cardsBox;
    }

    // =============================================================
    // CARD 1: VS AI (Single Player)
    // =============================================================
    private VBox createAiCard(DoubleBinding widthBinding, DoubleBinding heightBinding) {
        VBox card = createBaseCard(widthBinding, heightBinding, 0);

        // Header: Warship Icon + "VS AI"
        Node warshipIcon = createUiIcon("icon-warship", 32, 28);
        Label title = new Label("VS AI");
        title.getStyleClass().add("mode-panel-title");

        HBox headerRow = new HBox(10, warshipIcon, title);
        headerRow.setAlignment(Pos.CENTER);

        // Badge: SINGLE PLAYER
        Label badge = new Label("SINGLE PLAYER");
        badge.getStyleClass().addAll("mode-pill-badge", "mode-pill-badge-blue");

        // Middle: Tactical Art
        Node artwork = createCardArtwork("mode-art-ai", widthBinding, 115.0);

        // Interaction: CHOOSE DIFFICULTY
        Label sectionLabel = new Label("CHOOSE DIFFICULTY");
        sectionLabel.getStyleClass().add("mode-section-label");

        easyBtn = createDifficultyButton("EASY", GameMode.AI_EASY);
        normalBtn = createDifficultyButton("NORMAL", GameMode.AI_NORMAL);
        hardBtn = createDifficultyButton("HARD", GameMode.AI_HARD);

        VBox difficultyBox = new VBox(6, easyBtn, normalBtn, hardBtn);
        difficultyBox.setAlignment(Pos.CENTER);
        updateDifficultyButtons();

        VBox optionsArea = new VBox(6, sectionLabel, difficultyBox);
        optionsArea.setAlignment(Pos.CENTER);

        // Primary SELECT Button
        Button selectBtn = createSelectButton(() -> selectMode(selectedAiDifficulty));
        selectBtn.setId("btn-select-ai");

        VBox.setVgrow(optionsArea, Priority.ALWAYS);

        card.getChildren().addAll(headerRow, badge, artwork, optionsArea, selectBtn);
        return card;
    }

    private Button createDifficultyButton(String label, GameMode mode) {
        Button b = new Button();
        b.setPrefWidth(220);
        b.setMaxWidth(260);

        b.setOnAction(e -> {
            nav.getAudio().playClick();
            selectedAiDifficulty = mode;
            activeCardIndex = 0;
            updateDifficultyButtons();
            updateActiveCardVisuals();
        });

        BorderPane inner = new BorderPane();
        inner.setMouseTransparent(true);

        Label indicator = new Label("○");
        indicator.setStyle("-fx-font-size: 13px; -fx-text-fill: #6ba7db;");

        Label text = new Label(label);

        BorderPane.setAlignment(indicator, Pos.CENTER_LEFT);
        BorderPane.setAlignment(text, Pos.CENTER);
        BorderPane.setMargin(indicator, new Insets(0, 0, 0, 10));

        inner.setLeft(indicator);
        inner.setCenter(text);
        b.setGraphic(inner);

        return b;
    }

    private void updateDifficultyButtons() {
        configureDifficultyBtn(easyBtn, "EASY", selectedAiDifficulty == GameMode.AI_EASY);
        configureDifficultyBtn(normalBtn, "NORMAL", selectedAiDifficulty == GameMode.AI_NORMAL);
        configureDifficultyBtn(hardBtn, "HARD", selectedAiDifficulty == GameMode.AI_HARD);
    }

    private void configureDifficultyBtn(Button btn, String label, boolean isSelected) {
        if (btn == null) return;
        btn.getStyleClass().removeAll("mode-option-btn", "mode-option-btn-selected");
        btn.getStyleClass().add(isSelected ? "mode-option-btn-selected" : "mode-option-btn");

        BorderPane bp = (BorderPane) btn.getGraphic();
        if (bp != null) {
            Label ind = (Label) bp.getLeft();
            if (ind != null) {
                ind.setText(isSelected ? "●" : "○");
                ind.setStyle(isSelected ? "-fx-font-size: 13px; -fx-text-fill: #ffffff;" : "-fx-font-size: 13px; -fx-text-fill: #5b8fb8;");
            }
        }
    }

    // =============================================================
    // CARD 2: HOTSEAT (Same Device, Default Active)
    // =============================================================
    private VBox createHotseatCard(DoubleBinding widthBinding, DoubleBinding heightBinding) {
        VBox card = createBaseCard(widthBinding, heightBinding, 1);

        // Header: Anchor Icon + "HOTSEAT"
        Node anchorIcon = createUiIcon("icon-anchor", 34, 34);
        Label title = new Label("HOTSEAT");
        title.getStyleClass().addAll("mode-panel-title", "mode-panel-title-gold");

        HBox headerRow = new HBox(10, anchorIcon, title);
        headerRow.setAlignment(Pos.CENTER);

        // Badge: SAME DEVICE
        Label badge = new Label("SAME DEVICE");
        badge.getStyleClass().addAll("mode-pill-badge", "mode-pill-badge-gold");

        // Middle: Tactical Art
        Node artwork = createCardArtwork("mode-art-hotseat", widthBinding, 140.0);

        // Information text
        Label matchLabel = new Label("PLAYER 1  vs  PLAYER 2");
        matchLabel.setStyle("-fx-font-family: 'Segoe UI', Arial, sans-serif; -fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #e8f2fc;");

        Label subLabel = new Label("Two players • One device");
        subLabel.setStyle("-fx-font-family: 'Segoe UI', Arial, sans-serif; -fx-font-size: 11px; -fx-text-fill: #85a4c2;");

        VBox infoArea = new VBox(6, matchLabel, subLabel);
        infoArea.setAlignment(Pos.CENTER);
        VBox.setVgrow(infoArea, Priority.ALWAYS);

        // Primary SELECT Button
        Button selectBtn = createSelectButton(() -> selectMode(GameMode.HOTSEAT));
        selectBtn.setId("btn-select-hotseat");

        card.getChildren().addAll(headerRow, badge, artwork, infoArea, selectBtn);
        return card;
    }

    // =============================================================
    // CARD 3: ONLINE (LAN / QR Match)
    // =============================================================
    private VBox createOnlineCard(DoubleBinding widthBinding, DoubleBinding heightBinding) {
        VBox card = createBaseCard(widthBinding, heightBinding, 2);

        // Header: Compass Rose Icon + "ONLINE"
        Node compassIcon = createUiIcon("compass-rose", 34, 34);
        Label title = new Label("ONLINE");
        title.getStyleClass().add("mode-panel-title");

        HBox headerRow = new HBox(10, compassIcon, title);
        headerRow.setAlignment(Pos.CENTER);

        // Badge: LAN / QR MATCH
        Label badge = new Label("LAN / QR MATCH");
        badge.getStyleClass().addAll("mode-pill-badge", "mode-pill-badge-blue");

        // Middle: Tactical Art
        Node artwork = createCardArtwork("mode-art-online", widthBinding, 115.0);

        // Sub-mode options: LAN MATCH and QR MATCH
        lanChoice = createSubmodeBox("icon-wifi", "LAN MATCH", "LAN");
        qrChoice = createSubmodeBox("icon-qr", "QR MATCH", "QR");

        HBox submodeRow = new HBox(10, lanChoice, qrChoice);
        submodeRow.setAlignment(Pos.CENTER);
        updateOnlineSubmodeVisuals();

        VBox submodeArea = new VBox(8, submodeRow);
        submodeArea.setAlignment(Pos.CENTER);
        VBox.setVgrow(submodeArea, Priority.ALWAYS);

        // Primary SELECT Button
        Button selectBtn = createSelectButton(() -> {
            nav.getAudio().playClick();
            controller.setMode(GameMode.ONLINE);
            nav.showMultiplayerLobby();
        });
        selectBtn.setId("btn-select-online");

        card.getChildren().addAll(headerRow, badge, artwork, submodeArea, selectBtn);
        return card;
    }

    private VBox createSubmodeBox(String iconName, String label, String submodeId) {
        Node icon = createUiIcon(iconName, 26, 26);
        Label text = new Label(label);
        text.setStyle("-fx-font-family: 'Segoe UI', Arial, sans-serif; -fx-font-size: 11px; -fx-font-weight: bold; -fx-letter-spacing: 1px;");

        VBox box = new VBox(6, icon, text);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(10, 14, 10, 14));
        box.setPrefWidth(125);
        box.setCursor(javafx.scene.Cursor.HAND);

        box.setOnMouseClicked(e -> {
            nav.getAudio().playClick();
            selectedOnlineSubmode = submodeId;
            activeCardIndex = 2;
            updateOnlineSubmodeVisuals();
            updateActiveCardVisuals();
        });

        return box;
    }

    private void updateOnlineSubmodeVisuals() {
        if (lanChoice == null || qrChoice == null) return;
        boolean isLan = "LAN".equals(selectedOnlineSubmode);
        styleSubmodeBox(lanChoice, isLan);
        styleSubmodeBox(qrChoice, !isLan);
    }

    private void styleSubmodeBox(VBox box, boolean active) {
        box.getStyleClass().removeAll("mode-option-btn", "mode-option-btn-selected");
        box.getStyleClass().add(active ? "mode-option-btn-selected" : "mode-option-btn");
    }

    // =============================================================
    // Shared Card Chrome & Component Helpers
    // =============================================================
    private VBox createBaseCard(DoubleBinding widthBinding, DoubleBinding heightBinding, int cardIndex) {
        VBox card = new VBox(12);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(20, 18, 20, 18));
        card.prefWidthProperty().bind(widthBinding);
        card.prefHeightProperty().bind(heightBinding);
        card.getStyleClass().addAll("mode-panel");

        // Hover micro-animation (smooth translateY(-3.5px) over 220ms)
        TranslateTransition hoverAnim = new TranslateTransition(Duration.millis(220), card);
        card.setOnMouseEntered(e -> {
            hoverAnim.stop();
            hoverAnim.setToY(-3.5);
            hoverAnim.play();
        });
        card.setOnMouseExited(e -> {
            hoverAnim.stop();
            hoverAnim.setToY(0.0);
            hoverAnim.play();
        });

        // Click on card selects it as active
        card.setOnMouseClicked(e -> {
            activeCardIndex = cardIndex;
            updateActiveCardVisuals();
        });

        return card;
    }

    private void updateActiveCardVisuals() {
        for (int i = 0; i < modeCards.size(); i++) {
            VBox card = modeCards.get(i);
            card.getStyleClass().removeAll("mode-panel-active");
            if (i == activeCardIndex) {
                card.getStyleClass().add("mode-panel-active");
                card.setScaleX(1.015);
                card.setScaleY(1.015);
            } else {
                card.setScaleX(1.0);
                card.setScaleY(1.0);
            }
        }
    }

    private Node createCardArtwork(String imageName, DoubleBinding cardWidthBinding, double defaultHeight) {
        Image img = ImageResources.ui(imageName);
        if (img == null) {
            img = ImageResources.background("menu-background");
        }

        ImageView iv = new ImageView();
        if (img != null) {
            iv.setImage(img);
        }
        iv.setPreserveRatio(false);
        iv.setSmooth(true);
        iv.setMouseTransparent(true);

        // Width fits within card with padding
        iv.fitWidthProperty().bind(cardWidthBinding.subtract(36));
        iv.setFitHeight(defaultHeight);

        // Rounded corners via clip
        Rectangle clip = new Rectangle();
        clip.setArcWidth(12);
        clip.setArcHeight(12);
        clip.widthProperty().bind(iv.fitWidthProperty());
        clip.heightProperty().bind(iv.fitHeightProperty());
        iv.setClip(clip);

        // Subtle dark inner frame
        StackPane frame = new StackPane(iv);
        frame.setAlignment(Pos.CENTER);
        frame.setStyle("-fx-border-color: rgba(36, 87, 122, 0.45); -fx-border-radius: 6; -fx-border-width: 1;");
        frame.setMouseTransparent(true);

        return frame;
    }

    private Node createUiIcon(String iconName, double width, double height) {
        Image img = ImageResources.ui(iconName);
        if (img != null) {
            ImageView iv = new ImageView(img);
            iv.setFitWidth(width);
            iv.setFitHeight(height);
            iv.setPreserveRatio(true);
            iv.setSmooth(true);
            iv.setMouseTransparent(true);
            return iv;
        }
        Label fallback = new Label("\u2693");
        fallback.setStyle("-fx-font-size: 22px; -fx-text-fill: #ffd166;");
        return fallback;
    }

    private Button createSelectButton(Runnable onAction) {
        Button b = new Button();
        b.getStyleClass().add("mode-action-btn");
        b.setPrefHeight(44);
        b.setMaxWidth(Double.MAX_VALUE);

        BorderPane bp = new BorderPane();
        bp.setMouseTransparent(true);

        Label text = new Label("SELECT");
        text.getStyleClass().add("mode-action-text");

        Label chevron = new Label("\u203A");
        chevron.getStyleClass().add("mode-action-chevron");

        BorderPane.setAlignment(text, Pos.CENTER);
        BorderPane.setAlignment(chevron, Pos.CENTER_RIGHT);
        BorderPane.setMargin(chevron, new Insets(0, 14, 0, 0));

        bp.setCenter(text);
        bp.setRight(chevron);
        b.setGraphic(bp);

        b.setOnAction(e -> {
            nav.getAudio().playClick();
            onAction.run();
        });

        // Hover chevron micro-animation
        TranslateTransition chevMove = new TranslateTransition(Duration.millis(180), chevron);
        b.setOnMouseEntered(e -> {
            chevMove.stop();
            chevMove.setToX(3.0);
            chevMove.play();
        });
        b.setOnMouseExited(e -> {
            chevMove.stop();
            chevMove.setToX(0.0);
            chevMove.play();
        });

        return b;
    }

    private void selectMode(GameMode mode) {
        nav.getAudio().playClick();
        controller.setMode(mode);
        nav.showBoardSelect();
    }
}
