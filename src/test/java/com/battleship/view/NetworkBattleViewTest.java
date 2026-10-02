package com.battleship.view;

import com.battleship.controller.GameController;
import com.battleship.controller.NetworkFireService;
import com.battleship.model.Coordinate;
import com.battleship.model.GameMode;
import com.battleship.model.HumanPlayer;
import com.battleship.model.Orientation;
import com.battleship.model.Player;
import com.battleship.model.ShipType;
import com.battleship.model.Theater;
import com.battleship.model.weapon.Weapon;
import com.battleship.model.weapon.WeaponCatalog;
import com.battleship.net.NetMessage;
import com.battleship.net.NetworkGameSession;
import com.battleship.net.NetworkSession;
import com.battleship.net.Role;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class NetworkBattleViewTest {
    @BeforeAll
    static void startJavaFx() throws Exception {
        FxTestSupport.startToolkit();
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void waitingPlayerCanAnswerAndLaunchNuclearOnTheirTurn(Role role) throws Exception {
        CompletableFuture<Void> checked = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                verifyQuestionAndWeaponFlow(role);
                checked.complete(null);
            } catch (Throwable failure) {
                checked.completeExceptionally(failure);
            }
        });
        checked.get(10, TimeUnit.SECONDS);
    }

    private void verifyQuestionAndWeaponFlow(Role role) throws Exception {
        Player me = new HumanPlayer("Local player", Theater.SKIRMISH);
        me.deploy(ShipType.PATROL_BOAT, new Coordinate(4, 0), Orientation.HORIZONTAL);
        RecordingController controller = new RecordingController();
        controller.setMode(GameMode.ONLINE);
        controller.setTheater(Theater.SKIRMISH);

        // Use an unconnected transport: incoming messages go through the real view callback.
        var constructor = NetworkSession.class.getDeclaredConstructor(Executor.class);
        constructor.setAccessible(true);
        NetworkSession transport = constructor.newInstance((Executor) Runnable::run);
        NetworkGameSession session = new NetworkGameSession(transport, Theater.SKIRMISH, role, me);
        session.beginMatch(role == Role.CLIENT); // The opponent moves first for either role.
        TestView view = new TestView(navigator(), controller, session);
        view.build();
        assertTrue(view.nuclearButton().isDisabled());
        assertTrue(view.enemyGrid.isDisabled());

        deliver(transport, incomingMiss());
        assertTrue(session.isMyTurn());
        assertFalse(view.enemyGrid.isDisabled());
        assertFalse(view.nuclearButton().isDisabled(), "The waiting player's nuclear button must unlock");

        view.nuclearButton().fire();
        assertSame(WeaponCatalog.nuclear(), me.selectedWeapon());
        view.enemyGrid.getCell(0, 0).getOnMouseClicked().handle(null);
        assertEquals(1, view.questionsAsked);
        assertEquals(3, me.ammoCount(WeaponCatalog.nuclear()));
        assertNull(controller.lastOrder, "A wrong answer must not fire or spend ammo");
        assertTrue(session.isMyTurn());
        assertSame(WeaponCatalog.standard(), me.selectedWeapon());

        view.refreshLauncherBar();
        view.authorized = true;
        view.nuclearButton().fire();
        view.enemyGrid.getCell(0, 0).getOnMouseClicked().handle(null);
        assertEquals(2, view.questionsAsked);
        assertSame(WeaponCatalog.nuclear(), controller.lastOrder.weapon());
        assertEquals(new Coordinate(0, 0), controller.lastOrder.anchor());
        assertEquals(2, me.ammoCount(WeaponCatalog.nuclear()));
        assertFalse(session.isMyTurn());
        assertTrue(view.enemyGrid.isDisabled());
        assertTrue(view.nuclearButton().isDisabled());

        deliver(transport, new NetMessage.FireResult(List.of(), List.of(), false));
        assertTrue(view.nuclearButton().isDisabled());
        deliver(transport, incomingMiss());
        assertFalse(view.nuclearButton().isDisabled(), "Weapons must also unlock on subsequent turns");
        assertEquals(2, view.questionsAsked, "Incoming fire must not ask the defender a launch question");
        transport.close();
        view.stopBattle();
    }

    private NetMessage.Fire incomingMiss() {
        return new NetMessage.Fire(WeaponCatalog.standard().id(), new Coordinate(3, 3), Orientation.HORIZONTAL);
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void authorizationCannotFireAfterTimeoutEvenIfNextTurnArrives(Role role) throws Exception {
        FxTestSupport.onFxThread(() -> {
            try {
                RecordingController controller = new RecordingController();
                controller.setMode(GameMode.ONLINE);
                controller.setTheater(Theater.SKIRMISH);
                Player me = new HumanPlayer("Me", Theater.SKIRMISH);
                me.deploy(ShipType.PATROL_BOAT, new Coordinate(4, 0), Orientation.HORIZONTAL);
                var constructor = NetworkSession.class.getDeclaredConstructor(Executor.class);
                constructor.setAccessible(true);
                NetworkSession transport = constructor.newInstance((Executor) Runnable::run);
                NetworkGameSession session = new NetworkGameSession(transport, Theater.SKIRMISH, role, me);
                session.beginMatch(role == Role.HOST);
                TestView view = new TestView(navigator(), controller, session);
                view.build();
                view.authorized = true;
                view.expireDuringAuthorization = true;
                view.nuclearButton().fire();
                view.enemyGrid.getCell(0, 0).getOnMouseClicked().handle(null);
                assertEquals(2, session.getTurnNumber());
                assertTrue(session.isMyTurn());
                assertNull(controller.lastOrder, "An authorization from an expired turn must never fire");
                assertEquals(3, me.ammoCount(WeaponCatalog.nuclear()));
                view.stopBattle();
                transport.close();
            } catch (Exception error) { throw new RuntimeException(error); }
        });
    }

    @SuppressWarnings("unchecked")
    private static void deliver(NetworkSession transport, NetMessage message) throws Exception {
        var handler = NetworkSession.class.getDeclaredField("onMessage");
        handler.setAccessible(true);
        ((Consumer<NetMessage>) handler.get(transport)).accept(message);
    }

    private ViewNavigator navigator() {
        return (ViewNavigator) Proxy.newProxyInstance(ViewNavigator.class.getClassLoader(),
                new Class<?>[]{ViewNavigator.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getAudio")) return SilentAudio.INSTANCE;
                    if (method.getName().equals("getStage")) return null;
                    throw new AssertionError("Unexpected navigation: " + method.getName());
                });
    }

    private static class RecordingController extends GameController {
        private NetworkFireService.NetworkShotOrder lastOrder;

        @Override
        public NetworkFireService.NetworkShotOrder fireNetworkShot(Player shooter, Weapon weapon,
                                                                   Coordinate anchor, Orientation orientation) {
            lastOrder = super.fireNetworkShot(shooter, weapon, anchor, orientation);
            return lastOrder;
        }
    }

    private static class TestView extends NetworkBattleView {
        private boolean authorized;
        private int questionsAsked;
        private boolean expireDuringAuthorization;
        private final NetworkGameSession testSession;

        TestView(ViewNavigator nav, GameController controller, NetworkGameSession session) {
            super(nav, controller, session);
            this.testSession = session;
        }

        @Override
        protected StackPane decorateRoot(Pane layout) {
            return new StackPane(layout); // No decorative animations or windows in this test.
        }

        @Override
        protected boolean requestNuclearAuthorization() {
            questionsAsked++;
            if (expireDuringAuthorization) {
                onTurnExpired();
                try { deliver(testSession.getSession(), new NetMessage.TurnExpired(testSession.getTurnNumber())); }
                catch (Exception error) { throw new RuntimeException(error); }
            }
            return authorized;
        }

        Button nuclearButton() {
            return launcherBar.getChildren().stream().map(Button.class::cast)
                    .filter(button -> button.getText().startsWith(WeaponCatalog.nuclear().displayName()))
                    .findFirst().orElseThrow();
        }
    }
}
