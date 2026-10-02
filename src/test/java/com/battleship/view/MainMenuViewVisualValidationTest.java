package com.battleship.view;

import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainMenuViewVisualValidationTest {

    @BeforeAll
    static void initFx() throws Exception {
        FxTestSupport.startToolkit();
    }

    @Test
    void testRenderAndMeasureAcrossResolutions() throws Exception {
        FxTestSupport.onFxThread(() -> {
            double[][] resolutions = {
                    {1920, 1080},
                    {1536, 864},
                    {1366, 768}
            };

            for (double[] res : resolutions) {
                double w = res[0];
                double h = res[1];

                TestNav nav = new TestNav();
                MainMenuView menuView = new MainMenuView(nav);
                StackPane root = menuView.build();

                Scene scene = new Scene(root, w, h);
                scene.getStylesheets().add(getClass().getResource("/styles/battleship.css").toExternalForm());
                root.resize(w, h);
                root.applyCss();
                root.layout();

                // Find key elements
                Node captainsNode = root.lookupAll(".image-view").stream()
                        .filter(iv -> iv != root.getChildren().get(0)) // exclude bgView
                        .findFirst()
                        .orElse(null);
                assertNotNull(captainsNode, "Captains node must be present at " + w + "x" + h);

                Button playBtn = (Button) root.lookup(".menu-play-button");
                assertNotNull(playBtn, "Play button must be present");

                List<Node> navButtons = root.lookupAll(".menu-nav-button").stream().toList();
                assertTrue(navButtons.size() >= 3, "At least 3 nav buttons present");

                Button exitBtn = (Button) root.lookupAll(".menu-nav-button").stream()
                        .filter(b -> "EXIT".equals(((Button) b).getUserData()))
                        .findFirst()
                        .orElse(null);
                assertNotNull(exitBtn, "Exit button must be present");

                // Find hero composition VBox and set opacity to 1.0 for snapshot
                VBox heroVBox = root.getChildren().stream()
                        .filter(n -> n instanceof VBox)
                        .map(n -> (VBox) n)
                        .findFirst()
                        .orElse(null);
                if (heroVBox != null) {
                    heroVBox.setOpacity(1.0);
                    heroVBox.setTranslateY(0);
                }

                root.applyCss();
                root.layout();

                // Measure positions relative to scene
                Bounds captainsBounds = captainsNode.localToScene(captainsNode.getBoundsInLocal());
                Bounds playBounds = playBtn.localToScene(playBtn.getBoundsInLocal());
                Bounds exitBounds = exitBtn.localToScene(exitBtn.getBoundsInLocal());

                System.out.printf("[%dx%d] Captains Y: %.1f - %.1f (height: %.1f, %.1f%%vh)%n",
                        (int) w, (int) h, captainsBounds.getMinY(), captainsBounds.getMaxY(),
                        captainsBounds.getHeight(), (captainsBounds.getHeight() / h) * 100);

                System.out.printf("[%dx%d] Play button Y: %.1f, Exit button bottom Y: %.1f (%.1f%%vh)%n",
                        (int) w, (int) h, playBounds.getMinY(), exitBounds.getMaxY(),
                        (exitBounds.getMaxY() / h) * 100);

                // Assertions
                assertTrue(exitBounds.getMaxY() < h,
                        "Exit button bottom (" + exitBounds.getMaxY() + ") must be within viewport height " + h);
                assertTrue(exitBounds.getMaxY() <= h * 0.94,
                        "Entire hero composition should fit within 94% of viewport height at " + w + "x" + h);
                assertTrue(captainsBounds.getHeight() >= (h >= 1000 ? 400 : (h >= 800 ? 320 : 270)),
                        "Captain height should be appropriately large at " + w + "x" + h);

                // Snapshot and save to artifacts directory
                try {
                    SnapshotParameters sp = new SnapshotParameters();
                    sp.setFill(Color.BLACK);
                    WritableImage snapshot = root.snapshot(sp, null);
                    int iw = (int) snapshot.getWidth();
                    int ih = (int) snapshot.getHeight();
                    PixelReader pr = snapshot.getPixelReader();
                    BufferedImage bi = new BufferedImage(iw, ih, BufferedImage.TYPE_INT_ARGB);
                    for (int py = 0; py < ih; py++) {
                        for (int px = 0; px < iw; px++) {
                            bi.setRGB(px, py, pr.getArgb(px, py));
                        }
                    }
                    File out = new File("C:/Users/Admin/.gemini/antigravity-ide/brain/f4134102-c97d-4f10-ab95-c19022b874c9/menu_" + (int)w + "x" + (int)h + ".png");
                    ImageIO.write(bi, "png", out);
                    System.out.println("Saved snapshot: " + out.getAbsolutePath());
                } catch (Exception ex) {
                    System.err.println("Snapshot failed: " + ex.getMessage());
                }
            }
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
        @Override public Stage getStage() { return new Stage(); }
        @Override public GameAudio getAudio() { return audio; }
    }
}
