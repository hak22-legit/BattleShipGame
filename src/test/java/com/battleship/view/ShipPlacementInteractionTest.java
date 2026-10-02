package com.battleship.view;

import com.battleship.controller.GameController;
import com.battleship.model.*;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import static org.junit.jupiter.api.Assertions.*;

class ShipPlacementInteractionTest {
    @BeforeAll static void startToolkit() throws Exception { FxTestSupport.startToolkit(); }

    @Test void clickReturnsOccupiedShipEvenWithAnotherDockShipSelected() throws Exception {
        FxTestSupport.onFxThread(() -> {
            Fixture f = new Fixture();
            f.controller.placeShip(f.player, ShipType.PATROL_BOAT, new Coordinate(0, 0), Orientation.HORIZONTAL);
            f.view.refreshAll();
            f.view.dockPane.selectShip(ShipType.SUBMARINE);
            Node cell = f.view.boardGridPane.getCell(0, 1);
            mouse(cell, MouseEvent.MOUSE_PRESSED, center(cell), true);
            mouse(cell, MouseEvent.MOUSE_CLICKED, center(cell), false);
            assertEquals(0, f.player.deployedShipCount());
            assertNull(f.view.dockPane.getSelectedShip());
            assertTrue(f.view.readyButton.isDisabled());
            assertEquals(2, f.controller.getRemainingShipCounts(f.player).get(ShipType.PATROL_BOAT));
        });
    }

    @Test void rRotatesDockDragAndGhostWithoutMovingPointer() throws Exception {
        FxTestSupport.onFxThread(() -> {
            Fixture f = new Fixture();
            Node source = f.dockShip(ShipType.SUBMARINE);
            f.begin(source);
            Point2D target = center(f.view.boardGridPane.getCell(1, 1));
            mouse(source, MouseEvent.MOUSE_DRAGGED, target, true);
            key(f.root, KeyCode.R);
            assertEquals(Orientation.VERTICAL, f.view.orientation);
            assertTrue(f.view.boardGridPane.getCell(3, 1).getStyleClass().contains(BoardGridPane.GHOST_VALID));
            assertFalse(f.view.boardGridPane.getCell(1, 3).getStyleClass().contains(BoardGridPane.GHOST_VALID));
            assertSame(f.view.dockPane, source.getParent(), "Rotation must not detach the drag source");
            mouse(source, MouseEvent.MOUSE_RELEASED, target, false);
            assertEquals(1, f.player.deployedShipCount());
            assertEquals(Orientation.VERTICAL, f.player.fleet().get(0).orientation());
            assertTrue(f.player.fleet().get(0).cells().contains(new Coordinate(3, 1)));
            assertFalse(f.root.lookup(".placement-drag-preview").isVisible());
        });
    }

    @Test void dragBackToDockRestoresInventoryAndDisablesReady() throws Exception {
        FxTestSupport.onFxThread(() -> {
            Fixture f = new Fixture();
            f.controller.autoPlaceRemaining(f.player);
            f.view.refreshAll();
            f.root.applyCss(); f.root.layout();
            assertFalse(f.view.readyButton.isDisabled());
            var ship = f.player.fleet().get(0);
            Node source = f.view.boardGridPane.getCell(ship.cells().get(0));
            f.begin(source);
            Point2D dock = center(f.view.dockPane);
            mouse(source, MouseEvent.MOUSE_DRAGGED, dock, true);
            mouse(source, MouseEvent.MOUSE_RELEASED, dock, false);
            assertEquals(2, f.player.deployedShipCount());
            assertTrue(f.view.readyButton.isDisabled());
            assertEquals(1, f.controller.getRemainingShipCounts(f.player).get(ship.type()));
            for (Coordinate c : ship.cells()) assertEquals(CellStatus.EMPTY, f.player.cellStatus(c));
        });
    }

    @Test void invalidDropAndEscapeKeepOriginalShipIntact() throws Exception {
        FxTestSupport.onFxThread(() -> {
            Fixture f = new Fixture();
            f.controller.placeShip(f.player, ShipType.SUBMARINE, new Coordinate(0, 0), Orientation.HORIZONTAL);
            f.view.refreshAll();
            var original = f.player.fleet();
            Node source = f.view.boardGridPane.getCell(0, 0);
            f.begin(source);
            Point2D invalid = center(f.view.boardGridPane.getCell(4, 4));
            mouse(source, MouseEvent.MOUSE_DRAGGED, invalid, true);
            key(f.root, KeyCode.R);
            mouse(source, MouseEvent.MOUSE_RELEASED, invalid, false);
            assertEquals(original, f.player.fleet());
            f.begin(source);
            key(f.root, KeyCode.R);
            key(f.root, KeyCode.ESCAPE);
            mouse(source, MouseEvent.MOUSE_RELEASED, center(f.view.dockPane), false);
            mouse(source, MouseEvent.MOUSE_CLICKED, center(source), false);
            assertEquals(original, f.player.fleet());
        });
    }

    @Test void placedShipCanBeRotatedAndMovedAcrossItsOldCells() throws Exception {
        FxTestSupport.onFxThread(() -> {
            Fixture f = new Fixture();
            f.controller.placeShip(f.player, ShipType.SUBMARINE, new Coordinate(1, 1), Orientation.HORIZONTAL);
            f.view.refreshAll();
            Node source = f.view.boardGridPane.getCell(1, 2);
            f.begin(source);
            Point2D target = center(f.view.boardGridPane.getCell(1, 1));
            mouse(source, MouseEvent.MOUSE_DRAGGED, target, true);
            key(f.root, KeyCode.R);
            mouse(source, MouseEvent.MOUSE_RELEASED, target, false);
            assertEquals(1, f.player.deployedShipCount());
            assertEquals(Orientation.VERTICAL, f.player.fleet().get(0).orientation());
            assertEquals(CellStatus.EMPTY, f.player.cellStatus(new Coordinate(1, 2)));
        });
    }

    @Test void lockedPlacementRejectsClickDragAndRotation() throws Exception {
        FxTestSupport.onFxThread(() -> {
            Fixture f = new Fixture();
            f.controller.placeShip(f.player, ShipType.PATROL_BOAT, new Coordinate(0, 0), Orientation.HORIZONTAL);
            f.view.refreshAll();
            var before = f.player.fleet();
            f.view.locked = true;
            Node source = f.view.boardGridPane.getCell(0, 0);
            mouse(source, MouseEvent.MOUSE_CLICKED, center(source), false);
            f.begin(source);
            key(f.root, KeyCode.R);
            mouse(source, MouseEvent.MOUSE_RELEASED, center(f.view.dockPane), false);
            assertEquals(before, f.player.fleet());
            assertEquals(Orientation.HORIZONTAL, f.view.orientation);
        });
    }

    private static void key(Node target, KeyCode key) {
        Event.fireEvent(target, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", key, false, false, false, false));
    }
    private static Point2D center(Node node) {
        var bounds = node.getBoundsInLocal();
        return node.localToScene((bounds.getMinX() + bounds.getMaxX()) / 2,
                (bounds.getMinY() + bounds.getMaxY()) / 2);
    }
    private static void mouse(Node source, EventType<MouseEvent> type, Point2D scene, boolean down) {
        Event.fireEvent(source, new MouseEvent(type, scene.getX(), scene.getY(), scene.getX(), scene.getY(),
                MouseButton.PRIMARY, 1, false, false, false, false, down, false, false,
                false, false, type == MouseEvent.MOUSE_CLICKED, new PickResult(source, scene.getX(), scene.getY())));
    }
    private static class Fixture {
        final GameController controller = new GameController();
        final Player player;
        final TestView view;
        final StackPane root;
        Fixture() {
            controller.setMode(GameMode.AI_EASY);
            controller.setTheater(Theater.SKIRMISH);
            player = controller.getPlacingPlayer();
            ViewNavigator nav = (ViewNavigator) Proxy.newProxyInstance(ViewNavigator.class.getClassLoader(),
                    new Class<?>[]{ViewNavigator.class}, (proxy, method, args) ->
                            method.getName().equals("getAudio") ? SilentAudio.INSTANCE : null);
            view = new TestView(nav, controller);
            root = view.build();
            new Scene(root, 1100, 700);
            root.applyCss(); root.layout();
        }
        Node dockShip(ShipType type) {
            return view.dockPane.getChildren().stream().filter(n -> n.getUserData() == type).findFirst().orElseThrow();
        }
        void begin(Node source) {
            mouse(source, MouseEvent.MOUSE_PRESSED, center(source), true);
            mouse(source, MouseEvent.DRAG_DETECTED, center(source), true);
        }
    }
    private static class TestView extends AbstractShipPlaceView {
        boolean locked;
        TestView(ViewNavigator nav, GameController controller) { super(nav, controller, controller.getPlacingPlayer()); }
        @Override protected Pane assembleLayout() {
            dockPane.setPrefWidth(190);
            return new HBox(25, dockPane, boardGridPane, new VBox(orientationLabel, countLabel, readyButton));
        }
        @Override protected StackPane decorateRoot(Pane layout) { return new StackPane(layout); }
        @Override protected void onReadyPressed() { }
        @Override protected String exitPrompt() { return "Exit"; }
        @Override protected void onExitConfirmed() { }
        @Override protected boolean isReadyLocked() { return locked; }
    }
}
