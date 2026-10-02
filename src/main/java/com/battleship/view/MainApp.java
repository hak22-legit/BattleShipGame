package com.battleship.view;

import com.battleship.controller.GameController;
import com.battleship.model.GameState;
import com.battleship.model.Player;
import com.battleship.net.NetworkGameSession;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * javafx application entry point. owns the primary stage/scene and implements
 * {@link viewnavigator} — views receive the navigator abstraction (never this
 * concrete application class), so navigation can be mocked in tests.
 */
public class MainApp extends Application implements ViewNavigator {

    private Stage stage;
    private Scene scene;
    private final GameController controller = new GameController();

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        this.scene = new Scene(new StackPane(), 1160, 740);
        scene.getStylesheets().add(getClass().getResource("/styles/battleship.css").toExternalForm());
        stage.setTitle("Battleship: Naval Command");
        stage.setMinWidth(1060);
        stage.setMinHeight(680);
        stage.setScene(scene);
        getAudio().playMenuMusic();
        showMainMenu();
        stage.show();
    }

    private void setRoot(javafx.scene.Parent root) {
        scene.setRoot(root);
    }

    /** lets standalone screens (e.g. the network multiplayer flow) push themselves directly. */
    public void setScreen(javafx.scene.Parent root) {
        setRoot(root);
    }

    public void showMainMenu() {
        setRoot(new MainMenuView(this).build());
    }

    public void showModeSelect() {
        controller.goToModeSelect();
        setRoot(new GameModeSelectView(this, controller).build());
    }

    public void showMultiplayerLobby() {
        setRoot(new MultiplayerLobbyView(this, controller).build());
    }

    public void showBoardSelect() {
        setRoot(new BoardSelectView(this, controller).build());
    }

    public void showShipPlacement() {
        setRoot(new ShipPlaceView(this, controller).build());
    }

    public void showPassScreen(Runnable onContinue) {
        String nextName = (controller.getState() == GameState.PASS_SCREEN || controller.getState() == GameState.SHIP_PLACEMENT)
                ? controller.getPlacingPlayer().getName()
                : controller.getCurrentPlayer().name();
        setRoot(new PassScreen(nextName, onContinue).build());
    }

    public void showBattle() {
        setRoot(new LocalBattleView(this, controller).build());
    }

    public void showGameOver(Player winner) {
        setRoot(new GameOverView(this, controller, winner).build());
    }

    // ---------- network flow (viewnavigator contract) ----------

    @Override
    public void showNetworkShipPlacement(NetworkGameSession session) {
        setRoot(new NetworkShipPlaceView(this, controller, session).build());
    }

    @Override
    public void showNetworkBattle(NetworkGameSession session) {
        setRoot(new NetworkBattleView(this, controller, session).build());
    }

    @Override
    public void showNetworkGameOver(NetworkGameSession session, boolean won) {
        setRoot(new NetworkGameOverView(this, session, won).build());
    }

    private final GameAudio audio = new SoundManager();

    public Stage getStage() { return stage; }
    public GameController getController() { return controller; }

    /** views play sounds through the gameaudio abstraction, never the singleton. */
    @Override
    public GameAudio getAudio() { return audio; }

    public static void main(String[] args) {
        launch(args);
    }
}
