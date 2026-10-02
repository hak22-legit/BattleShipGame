package com.battleship.view;

import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainMenuViewTest {

    @BeforeAll
    static void initFx() throws Exception {
        FxTestSupport.startToolkit();
    }

    @Test
    void testBackgroundLoads() throws Exception {
        FxTestSupport.onFxThread(() -> {
            Image bg = ImageResources.background("menu-background");
            assertNotNull(bg, "menu-background image should load successfully");
            assertFalse(bg.isError(), "menu-background image should load without errors");
        });
    }

    @Test
    void testBuildMainMenuView() throws Exception {
        FxTestSupport.onFxThread(() -> {
            TestNav nav = new TestNav();
            MainMenuView menuView = new MainMenuView(nav);
            StackPane root = menuView.build();

            assertNotNull(root, "Root StackPane must not be null");
            assertNotNull(root.getBackground(), "Root StackPane should have background configured");

            // Verify background ImageView exists in the root children
            boolean hasBgImage = root.getChildren().stream()
                    .anyMatch(node -> node instanceof ImageView);
            assertTrue(hasBgImage, "Should contain background ImageView child node");

            // Verify menu buttons exist in the hierarchy
            long buttonCount = root.lookupAll(".button").size();
            assertTrue(buttonCount >= 4, "Should contain at least 4 menu buttons");

            // Verify play button has menu-play-button style
            boolean hasPlayButton = root.lookupAll(".menu-play-button").size() == 1;
            assertTrue(hasPlayButton, "Should contain one featured menu-play-button");

            // Verify nav buttons
            long navButtons = root.lookupAll(".menu-nav-button").size();
            assertTrue(navButtons >= 3, "Should contain at least 3 menu-nav-button instances");
        });
    }

    @Test
    void testResponsiveLayoutAcrossResolutions() throws Exception {
        FxTestSupport.onFxThread(() -> {
            double[][] resolutions = {
                {1920, 1080}, // Full HD Desktop
                {1366, 768},  // Standard Laptop
                {1024, 768},  // Tablet / 4:3
                {480, 800}    // Mobile / Narrow Viewport
            };

            for (double[] res : resolutions) {
                double w = res[0];
                double h = res[1];

                TestNav nav = new TestNav();
                MainMenuView menuView = new MainMenuView(nav);
                StackPane root = menuView.build();

                javafx.scene.Scene scene = new javafx.scene.Scene(root, w, h);
                root.resize(w, h);
                root.applyCss();
                root.layout();

                // Ensure root dimensions updated
                assertTrue(root.getWidth() > 0, "Root width should be positive at " + w + "x" + h);
                assertTrue(root.getHeight() > 0, "Root height should be positive at " + w + "x" + h);

                // Verify all 4 menu buttons are laid out cleanly
                Button playBtn = (Button) root.lookup(".menu-play-button");
                assertNotNull(playBtn, "Play button must exist at " + w + "x" + h);
                assertTrue(playBtn.isVisible(), "Play button must be visible at " + w + "x" + h);

                long buttonCount = root.lookupAll(".button").size();
                assertTrue(buttonCount >= 4, "All 4 buttons must exist at " + w + "x" + h);
            }
        });
    }

    @Test
    void testButtonActionPreservation() throws Exception {
        FxTestSupport.onFxThread(() -> {
            boolean[] actionsTriggered = new boolean[4]; // play, howToPlay, options, exit

            TestNav nav = new TestNav() {
                @Override public void showModeSelect() { actionsTriggered[0] = true; }
                @Override public javafx.stage.Stage getStage() {
                    return new javafx.stage.Stage() {
                        @Override public void close() { actionsTriggered[3] = true; }
                    };
                }
            };

            MainMenuView menuView = new MainMenuView(nav);
            StackPane root = menuView.build();

            // 1. Play button triggers showModeSelect
            Button play = (Button) root.lookup(".menu-play-button");
            assertNotNull(play);
            play.fire();
            assertTrue(actionsTriggered[0], "Play button must trigger showModeSelect");

            // 2. How to Play button opens overlay
            Button howToPlay = root.lookupAll(".menu-nav-button").stream()
                    .filter(b -> "HOW TO PLAY".equals(((Button) b).getUserData()))
                    .map(b -> (Button) b)
                    .findFirst()
                    .orElse(null);
            assertNotNull(howToPlay, "HOW TO PLAY button must exist");
            howToPlay.fire();
            boolean hasHowToPlayOverlay = root.getChildren().stream()
                    .anyMatch(node -> node.getStyleClass().contains("overlay-backdrop"));
            assertTrue(hasHowToPlayOverlay, "How to Play must add overlay-backdrop to root");

            // 3. Exit button triggers stage close
            Button exit = root.lookupAll(".menu-nav-button").stream()
                    .filter(b -> "EXIT".equals(((Button) b).getUserData()))
                    .map(b -> (Button) b)
                    .findFirst()
                    .orElse(null);
            assertNotNull(exit, "EXIT button must exist");
            exit.fire();
            assertTrue(actionsTriggered[3], "Exit button must trigger stage close");
        });
    }

    private static class TestNav implements ViewNavigator {
        private final GameAudio audio = new SilentAudio();

        @Override public void showMainMenu() { }
        @Override public void showModeSelect() { }
        @Override public void showMultiplayerLobby() { }
        @Override public void showBoardSelect() { }
        @Override public void showShipPlacement() { }
        @Override public void showPassScreen(Runnable onContinue) { }
        @Override public void showBattle() { }
        @Override public void showGameOver(com.battleship.model.Player winner) { }
        @Override public void showNetworkShipPlacement(com.battleship.net.NetworkGameSession session) { }
        @Override public void showNetworkBattle(com.battleship.net.NetworkGameSession session) { }
        @Override public void showNetworkGameOver(com.battleship.net.NetworkGameSession session, boolean won) { }
        @Override public void setScreen(javafx.scene.Parent root) { }
        @Override public javafx.stage.Stage getStage() { return new javafx.stage.Stage(); }
        @Override public GameAudio getAudio() { return audio; }
    }
}
