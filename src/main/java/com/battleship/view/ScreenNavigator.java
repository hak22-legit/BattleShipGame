package com.battleship.view;

import com.battleship.model.Player;
import com.battleship.net.NetworkGameSession;
import javafx.scene.Parent;

/**
 * narrow role interface dedicated strictly to screen and view routing.
 *
 * <p>satisfies the <strong>interface segregation principle (isp)</strong> by allowing
 * screens and dialogs to depend solely on navigation capabilities without forcing a
 * dependency on window stages or audio managers.</p>
 */
public interface ScreenNavigator {

    // ---------- local (vs ai / hotseat) flow ----------

    void showMainMenu();
    void showModeSelect();
    void showBoardSelect();
    void showShipPlacement();
    void showPassScreen(Runnable onContinue);
    void showBattle();
    void showGameOver(Player winner);
    void showMultiplayerLobby();

    // ---------- network ("play with a friend") flow ----------

    void showNetworkShipPlacement(NetworkGameSession session);
    void showNetworkBattle(NetworkGameSession session);
    void showNetworkGameOver(NetworkGameSession session, boolean won);

    /** lets standalone screens push themselves directly. */
    void setScreen(Parent root);
}
