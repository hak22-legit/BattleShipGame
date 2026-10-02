package com.battleship.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * builds the two modal overlays reachable from the main menu: the
 * "tactical manual" (how to play) and the "operational controls" (options)
 * panels. both are self-contained stackpanes meant to be stacked on top of
 * whatever screen is currently showing, dismissed by removing them again.
 */
final class MenuOverlays {

    private MenuOverlays() { }

    // ---------------------------------------------------------------
    // how to play
    // ---------------------------------------------------------------

    static StackPane howToPlay(GameAudio audio, Runnable onClose) {
        Label title = new Label("HOW TO PLAY: TACTICAL MANUAL");
        title.setFont(Font.font("Arial Black", FontWeight.BOLD, 22));
        title.getStyleClass().add("overlay-title");

        Label subtitle = new Label("FLEET ENGAGEMENT & COMBAT PROTOCOLS");
        subtitle.getStyleClass().add("overlay-subtitle");

        VBox header = new VBox(4, title, subtitle);
        header.setAlignment(Pos.CENTER);
        header.setMaxWidth(Double.MAX_VALUE);

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(14);
        grid.setAlignment(Pos.CENTER);
        grid.add(manualBlock("1. SETUP: PLACE YOUR FLEET",
                "Drag warships from Dock onto your grid. Right-click or press 'R' to rotate."), 0, 0);
        grid.add(manualBlock("2. SELECT TARGET GRID",
                "Switch tactical views between YOUR FLEET and ENEMY WATERS."), 1, 0);
        grid.add(manualBlock("3. FIRE & SPECIAL AMMO",
                "Click a cell in Enemy Waters to fire. Deploy Special Ordnance when available."), 0, 1);
        grid.add(manualBlock("4. TURN & VICTORY",
                "Turns alternate between admirals. Destroy all enemy warships to claim victory."), 1, 1);

        Label tip = new Label("TACTICAL TIP: Switch to Enemy Waters after placing your fleet to begin offensive strikes!");
        tip.getStyleClass().add("manual-tip-banner");
        tip.setMaxWidth(Double.MAX_VALUE);
        tip.setAlignment(Pos.CENTER);

        Button close = new Button("CLOSE");
        close.getStyleClass().addAll("primary-button", "featured-button");
        close.setPrefWidth(160);
        close.setPrefHeight(42);
        close.setOnAction(e -> { audio.playClick(); onClose.run(); });

        VBox content = new VBox(16, header, divider(), grid, tip, divider(), close);
        content.setAlignment(Pos.CENTER);
        VBox.setMargin(close, new Insets(4, 0, 0, 0));

        VBox card = wrapCard(content, 620);
        return backdrop(card);
    }

    private static VBox manualBlock(String heading, String body) {
        Label h = new Label(heading);
        h.getStyleClass().add("manual-block-title");
        h.setAlignment(Pos.CENTER_LEFT);
        Label b = new Label(body);
        b.getStyleClass().add("manual-block-body");
        b.setAlignment(Pos.CENTER_LEFT);
        b.setWrapText(true);
        VBox box = new VBox(6, h, b);
        box.getStyleClass().add("manual-block");
        box.setAlignment(Pos.TOP_LEFT);
        box.setPrefWidth(265);
        box.setMaxWidth(265);
        return box;
    }

    // ---------------------------------------------------------------
    // options
    // ---------------------------------------------------------------

    static StackPane options(GameAudio audio, Runnable onClose) {
        Label title = new Label("SETTINGS: OPERATIONAL CONTROLS");
        title.setFont(Font.font("Arial Black", FontWeight.BOLD, 22));
        title.getStyleClass().add("overlay-title");

        Label subtitle = new Label("SYSTEM AUDIO, DISPLAY & COMMAND MAPPINGS");
        subtitle.getStyleClass().add("overlay-subtitle");

        VBox header = new VBox(4, title, subtitle);
        header.setAlignment(Pos.CENTER);
        header.setMaxWidth(Double.MAX_VALUE);

        // --- audio & communications ---
        Label audioHeading = sectionHeading("1. AUDIO & COMMUNICATIONS");
        Slider master = themedSlider(audio.getMasterVolume() * 100);
        Slider sfx = themedSlider(audio.getSfxVolume() * 100);
        ToggleButton muteAll = new ToggleButton(audio.isMuted() ? "MUTED" : "MUTE ALL");
        muteAll.getStyleClass().add("switch-toggle");
        muteAll.setSelected(audio.isMuted());

        master.valueProperty().addListener((obs, old, val) ->
                audio.setMasterVolume(val.doubleValue() / 100));
        sfx.valueProperty().addListener((obs, old, val) ->
                audio.setSfxVolume(val.doubleValue() / 100));
        muteAll.setOnAction(e -> {
            boolean m = muteAll.isSelected();
            audio.setMuted(m);
            muteAll.setText(m ? "MUTED" : "MUTE ALL");
        });

        GridPane audioGrid = new GridPane();
        audioGrid.setHgap(16);
        audioGrid.setVgap(10);
        audioGrid.add(rowLabel("\uD83D\uDD0A  MASTER VOLUME"), 0, 0);
        audioGrid.add(master, 1, 0);
        audioGrid.add(rowLabel("\uD83C\uDFA7  SFX VOLUME"), 0, 1);
        audioGrid.add(sfx, 1, 1);
        GridPane.setHgrow(master, Priority.ALWAYS);
        GridPane.setHgrow(sfx, Priority.ALWAYS);
        master.setMaxWidth(Double.MAX_VALUE);
        sfx.setMaxWidth(Double.MAX_VALUE);

        HBox audioRow = new HBox(24, audioGrid, muteAll);
        audioRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(audioGrid, Priority.ALWAYS);

        // --- graphics & interface ---
        Label gfxHeading = sectionHeading("2. GRAPHICS & INTERFACE");
        ToggleGroup resGroup = new ToggleGroup();
        ToggleGroup scaleGroup = new ToggleGroup();
        ToggleGroup animGroup = new ToggleGroup();

        GridPane gfxGrid = new GridPane();
        gfxGrid.setHgap(16);
        gfxGrid.setVgap(10);
        gfxGrid.add(rowLabel("\uD83D\uDDA5  RESOLUTION"), 0, 0);
        gfxGrid.add(segmentedGroupWithToggle(resGroup, "1160x740", "1360x820", "Fullscreen"), 1, 0);
        gfxGrid.add(rowLabel("\uD83C\uDFA8  UI SCALE"), 0, 1);
        gfxGrid.add(segmentedGroupWithToggle(scaleGroup, "Standard", "Large"), 1, 1);
        gfxGrid.add(rowLabel("\u2728  ANIMATIONS"), 0, 2);
        gfxGrid.add(segmentedGroupWithToggle(animGroup, "On", "Off"), 1, 2);

        // --- controls & shortcuts ---
        Label ctrlHeading = sectionHeading("3. CONTROLS & SHORTCUTS");
        HBox ctrlRow = new HBox(24,
                keybindPair("\u2328  FIRE", "CLICK"),
                keybindPair("\uD83D\uDDB1  ROTATE", "R / R-CLICK"),
                keybindPair("\uD83D\uDEE5  REMOVE SHIP", "CLICK SHIP"));
        ctrlRow.setAlignment(Pos.CENTER_LEFT);

        Button save = new Button("SAVE & APPLY");
        save.getStyleClass().addAll("primary-button", "featured-button");
        save.setPrefWidth(180);
        save.setPrefHeight(42);

        Button close = new Button("CLOSE");
        close.getStyleClass().add("ghost-button");
        close.setPrefWidth(140);
        close.setPrefHeight(42);
        close.setOnAction(e -> { audio.playClick(); onClose.run(); });

        HBox buttons = new HBox(16, save, close);
        buttons.setMaxWidth(Double.MAX_VALUE);
        buttons.setAlignment(Pos.CENTER);

        VBox content = new VBox(16,
                header, divider(),
                audioHeading, audioRow, divider(),
                gfxHeading, gfxGrid, divider(),
                ctrlHeading, ctrlRow,
                buttons);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setFillWidth(true);
        VBox.setMargin(buttons, new Insets(12, 0, 0, 0));
        buttons.setAlignment(Pos.CENTER);

        VBox card = wrapCard(content, 620);

        save.setOnAction(e -> {
            audio.playClick();
            if (card.getScene() != null && card.getScene().getWindow() instanceof javafx.stage.Stage stage) {
                if (resGroup.getSelectedToggle() != null) {
                    String chosenRes = (String) resGroup.getSelectedToggle().getUserData();
                    if ("1360x820".equals(chosenRes)) {
                        stage.setFullScreen(false);
                        stage.setWidth(1360);
                        stage.setHeight(820);
                        stage.centerOnScreen();
                    } else if ("Fullscreen".equals(chosenRes)) {
                        stage.setFullScreen(true);
                    } else {
                        stage.setFullScreen(false);
                        stage.setWidth(1160);
                        stage.setHeight(740);
                        stage.centerOnScreen();
                    }
                }
            }
            onClose.run();
        });

        return backdrop(card);
    }

    private static HBox segmentedGroupWithToggle(ToggleGroup group, String... options) {
        HBox box = new HBox(6);
        for (int i = 0; i < options.length; i++) {
            ToggleButton tb = new ToggleButton(options[i]);
            tb.getStyleClass().add("segmented-toggle");
            tb.setToggleGroup(group);
            tb.setUserData(options[i]);
            if (i == 0) tb.setSelected(true);
            box.getChildren().add(tb);
        }
        return box;
    }

    private static Label sectionHeading(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("overlay-block-title");
        return l;
    }

    private static Label rowLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("info-text");
        l.setPrefWidth(160);
        l.setMinWidth(160);
        return l;
    }

    private static Slider themedSlider(double value) {
        Slider s = new Slider(0, 100, value);
        s.getStyleClass().add("themed-slider");
        return s;
    }

    private static HBox segmentedGroup(String... options) {
        ToggleGroup group = new ToggleGroup();
        HBox box = new HBox(6);
        for (int i = 0; i < options.length; i++) {
            ToggleButton tb = new ToggleButton(options[i]);
            tb.getStyleClass().add("segmented-toggle");
            tb.setToggleGroup(group);
            if (i == options.length - 1 && options.length > 2) tb.setSelected(true);
            else if (options.length == 2 && i == 0) tb.setSelected(true);
            box.getChildren().add(tb);
        }
        if (group.getSelectedToggle() == null && !box.getChildren().isEmpty()) {
            ((ToggleButton) box.getChildren().get(0)).setSelected(true);
        }
        return box;
    }

    private static VBox keybindPair(String label, String key) {
        Label l = new Label(label);
        l.getStyleClass().add("dim-text");
        Label k = new Label(key);
        k.getStyleClass().add("keybind-box");
        k.setMinWidth(72);
        k.setAlignment(Pos.CENTER);
        VBox box = new VBox(6, l, k);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    // ---------------------------------------------------------------
    // shared chrome
    // ---------------------------------------------------------------

    private static Region divider() {
        Region r = new Region();
        r.getStyleClass().add("overlay-divider");
        r.setMaxWidth(Double.MAX_VALUE);
        return r;
    }

    private static VBox wrapCard(javafx.scene.Node content, double width) {
        VBox card = new VBox(content);
        card.getStyleClass().add("overlay-card");
        card.setMaxWidth(width);
        card.setPrefWidth(width);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        card.setAlignment(Pos.CENTER);
        return card;
    }

    private static StackPane backdrop(javafx.scene.Node card) {
        StackPane overlay = new StackPane(card);
        overlay.getStyleClass().add("overlay-backdrop");
        StackPane.setAlignment(card, Pos.CENTER);
        return overlay;
    }
}
