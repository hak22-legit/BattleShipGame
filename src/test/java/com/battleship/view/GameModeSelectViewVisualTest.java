package com.battleship.view;

import com.battleship.controller.GameController;
import com.battleship.model.GameState;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameModeSelectViewVisualTest {

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
                GameController controller = new GameController();
                GameModeSelectView view = new GameModeSelectView(nav, controller);
                StackPane root = view.build();

                Scene scene = new Scene(root, w, h);
                scene.getStylesheets().add(getClass().getResource("/styles/battleship.css").toExternalForm());
                root.resize(w, h);
                root.applyCss();
                root.layout();

                // Find key elements
                List<Node> panels = root.lookupAll(".mode-panel").stream().toList();
                assertEquals(3, panels.size(), "Must have exactly 3 mode panels");

                Button selectAi = (Button) root.lookup("#btn-select-ai");
                Button selectHotseat = (Button) root.lookup("#btn-select-hotseat");
                Button selectOnline = (Button) root.lookup("#btn-select-online");
                Button backBtn = (Button) root.lookup("#btn-mode-back");

                assertNotNull(selectAi, "Select AI button must exist");
                assertNotNull(selectHotseat, "Select Hotseat button must exist");
                assertNotNull(selectOnline, "Select Online button must exist");
                assertNotNull(backBtn, "Back button must exist");

                // Measure layout bounds
                Bounds card0Bounds = panels.get(0).localToScene(panels.get(0).getBoundsInLocal());
                Bounds backBounds = backBtn.localToScene(backBtn.getBoundsInLocal());

                System.out.printf("[%dx%d] Card 0 Y: %.1f - %.1f (height: %.1f)%n",
                        (int) w, (int) h, card0Bounds.getMinY(), card0Bounds.getMaxY(), card0Bounds.getHeight());
                System.out.printf("[%dx%d] Back button bottom Y: %.1f (%.1f%%vh)%n",
                        (int) w, (int) h, backBounds.getMaxY(), (backBounds.getMaxY() / h) * 100);

                // Assertions for responsive layout
                assertTrue(backBounds.getMaxY() <= h,
                        "Back button bottom (" + backBounds.getMaxY() + ") must be within viewport height " + h);
                assertTrue(backBounds.getMaxY() <= h * 0.96,
                        "Selection screen should fit within 96% of viewport height at " + w + "x" + h);
                assertTrue(card0Bounds.getMinY() >= 40.0,
                        "Cards should sit cleanly below header at " + w + "x" + h);

                // Ensure layout opacity is 1.0 for snapshot
                VBox mainVBox = (VBox) root.lookup("#mode-select-layout");
                if (mainVBox != null) {
                    mainVBox.setOpacity(1.0);
                    mainVBox.setTranslateY(0);
                }
                root.applyCss();
                root.layout();

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
                    File out = new File("C:/Users/Admin/.gemini/antigravity-ide/brain/f4134102-c97d-4f10-ab95-c19022b874c9/modeselect_" + (int)w + "x" + (int)h + ".png");
                    ImageIO.write(bi, "png", out);
                    System.out.println("Saved mode select snapshot: " + out.getAbsolutePath());
                } catch (Exception ex) {
                    System.err.println("Snapshot failed: " + ex.getMessage());
                }
            }
        });
    }

    @Test
    void testGameModeSelectionFunctionality() throws Exception {
        FxTestSupport.onFxThread(() -> {
            boolean[] navigated = new boolean[3]; // 0=boardSelect, 1=lobby, 2=mainMenu
            TestNav nav = new TestNav() {
                @Override public void showBoardSelect() { navigated[0] = true; }
                @Override public void showMultiplayerLobby() { navigated[1] = true; }
                @Override public void showMainMenu() { navigated[2] = true; }
            };

            GameController controller = new GameController();
            GameModeSelectView view = new GameModeSelectView(nav, controller);
            StackPane root = view.build();

            Scene scene = new Scene(root, 1366, 768);
            scene.getStylesheets().add(getClass().getResource("/styles/battleship.css").toExternalForm());
            root.applyCss();
            root.layout();

            // 1. Hotseat select
            Button selectHotseat = (Button) root.lookup("#btn-select-hotseat");
            assertNotNull(selectHotseat);
            selectHotseat.fire();
            assertTrue(navigated[0], "Hotseat should trigger showBoardSelect");
            assertEquals(GameState.BOARD_SELECT, controller.getState(), "Controller state should be BOARD_SELECT");

            // 2. Online select
            Button selectOnline = (Button) root.lookup("#btn-select-online");
            assertNotNull(selectOnline);
            selectOnline.fire();
            assertTrue(navigated[1], "Online should trigger showMultiplayerLobby");

            // 3. Back button
            Button backBtn = (Button) root.lookup("#btn-mode-back");
            assertNotNull(backBtn);
            backBtn.fire();
            assertTrue(navigated[2], "Back button should trigger showMainMenu");
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
