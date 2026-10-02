package com.battleship.view;

import com.battleship.model.Coordinate;
import com.battleship.model.HumanPlayer;
import com.battleship.model.Orientation;
import com.battleship.model.Player;
import com.battleship.model.ShipType;
import com.battleship.model.Theater;
import com.battleship.model.projection.ShipSnapshot;
import com.battleship.net.NetMessage;
import com.battleship.net.NetworkGameSession;
import com.battleship.net.NetworkSession;
import com.battleship.net.Role;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class NetworkGameOverViewTest {

    @BeforeAll
    static void initFx() throws Exception {
        FxTestSupport.startToolkit();
    }

    private ViewNavigator stubNavigator() {
        return (ViewNavigator) Proxy.newProxyInstance(
                ViewNavigator.class.getClassLoader(),
                new Class<?>[]{ViewNavigator.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getAudio")) return SilentAudio.INSTANCE;
                    if (method.getName().equals("getStage")) return null;
                    return null;
                }
        );
    }

    private NetworkGameSession createSession(Role role) throws Exception {
        var constructor = NetworkSession.class.getDeclaredConstructor(Executor.class);
        constructor.setAccessible(true);
        NetworkSession transport = constructor.newInstance((Executor) Runnable::run);
        Player me = new HumanPlayer("Me", Theater.SKIRMISH);
        return new NetworkGameSession(transport, Theater.SKIRMISH, role, me);
    }

    @Test
    void buildsSuccessfullyWithRevealedFleet() throws Exception {
        FxTestSupport.onFxThread(() -> {
            try {
                NetworkGameSession session = createSession(Role.HOST);
                List<ShipSnapshot> enemyShips = List.of(
                        new ShipSnapshot(ShipType.PATROL_BOAT, List.of(new Coordinate(0, 0), new Coordinate(0, 1)), Orientation.HORIZONTAL, 0, false),
                        new ShipSnapshot(ShipType.SUBMARINE, List.of(new Coordinate(2, 2), new Coordinate(3, 2), new Coordinate(4, 2)), Orientation.VERTICAL, 3, true)
                );
                session.setRevealedEnemyFleet(enemyShips);

                NetworkGameOverView view = new NetworkGameOverView(stubNavigator(), session, false);
                StackPane root = view.build();
                assertNotNull(root);
                assertNotNull(session.getRevealedEnemyFleet());
                assertEquals(2, session.getRevealedEnemyFleet().size());
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void updatesWhenFleetRevealArrivesOverNetwork() throws Exception {
        FxTestSupport.onFxThread(() -> {
            try {
                NetworkGameSession session = createSession(Role.CLIENT);
                assertNull(session.getRevealedEnemyFleet());

                NetworkGameOverView view = new NetworkGameOverView(stubNavigator(), session, true);
                StackPane root = view.build();
                assertNotNull(root);

                // Simulate incoming NetMessage.FleetReveal
                List<ShipSnapshot> enemyShips = List.of(
                        new ShipSnapshot(ShipType.PATROL_BOAT, List.of(new Coordinate(1, 1), new Coordinate(1, 2)), Orientation.HORIZONTAL, 2, true)
                );
                var handlerField = NetworkSession.class.getDeclaredField("onMessage");
                handlerField.setAccessible(true);
                @SuppressWarnings("unchecked")
                Consumer<NetMessage> onMessage = (Consumer<NetMessage>) handlerField.get(session.getSession());
                assertNotNull(onMessage);

                onMessage.accept(new NetMessage.FleetReveal(enemyShips));
                assertEquals(enemyShips, session.getRevealedEnemyFleet());
            } catch (Exception e) {
                fail(e);
            }
        });
    }
}
