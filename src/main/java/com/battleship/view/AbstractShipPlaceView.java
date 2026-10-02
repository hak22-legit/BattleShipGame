package com.battleship.view;

import com.battleship.controller.GameController;
import com.battleship.model.Coordinate;
import com.battleship.model.Orientation;
import com.battleship.model.Player;
import com.battleship.model.projection.ShipSnapshot;
import com.battleship.model.ShipType;
import javafx.animation.TranslateTransition;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.geometry.Point2D;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public abstract class AbstractShipPlaceView {

    protected final ViewNavigator nav;
    protected final GameController controller;
    protected final Player player;
    protected final GameAudio audio;

    protected BoardGridPane boardGridPane;
    protected ShipDockPane dockPane;
    protected Label orientationLabel;
    protected Label countLabel;
    protected Button readyButton;

    /** current ship orientation, toggled by r / right-click / the rotate button. */
    protected Orientation orientation = Orientation.HORIZONTAL;

    private final List<Coordinate> ghostCells = new ArrayList<>();
    private StackPane root;
    private final StackPane dragPreview = new StackPane();
    private ShipType draggedType;
    private Coordinate draggedFrom;
    private Coordinate hoverCell;
    private boolean suppressClick;
    private double pointerX;
    private double pointerY;

    protected AbstractShipPlaceView(ViewNavigator nav, GameController controller, Player player) {
        this.nav = nav;
        this.controller = controller;
        this.player = player;
        this.audio = nav.getAudio();
    }

    // ================= template method =================

    private static double computePlacementCellSize(int size) {
        if (size <= 5) return 84;
        if (size <= 8) return 52;
        return 42;
    }

    protected BoardGridPane createBoardGrid() {
        int boardSize = controller.getSelectedTheater().getBoardSize();
        return new BoardGridPane(boardSize, computePlacementCellSize(boardSize));
    }

    /** assembles the shared placement skeleton. subclasses customize via hooks only. */
    public final StackPane build() {
        dockPane = createDock();
        boardGridPane = createBoardGrid();
        setupDragTargets();

        orientationLabel = new Label();
        orientationLabel.getStyleClass().addAll("accent-text", "orientation-label");
        updateOrientationLabel();

        countLabel = new Label();

        readyButton = new Button(readyButtonLabel());
        readyButton.getStyleClass().add("primary-button");
        readyButton.setOnAction(e -> {
            audio.playClick();
            onReadyPressed();
        });

        Pane layout = assembleLayout();
        root = decorateRoot(layout);

        dragPreview.setManaged(false);
        dragPreview.setMouseTransparent(true);
        dragPreview.setVisible(false);
        dragPreview.getStyleClass().add("placement-drag-preview");
        root.getChildren().add(dragPreview);
        dockPane.setOnDragStarted((type, event) -> beginDrag(type, null, event));

        root.setFocusTraversable(true);
        root.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.R) { toggleOrientation(); e.consume(); }
            else if (e.getCode() == KeyCode.ESCAPE && draggedType != null) {
                finishDrag();
                e.consume();
            }
        });
        root.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (e.getButton() == MouseButton.PRIMARY) suppressClick = false;
        });
        root.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
            if (draggedType != null) { updateDrag(e.getSceneX(), e.getSceneY()); e.consume(); }
        });
        root.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
            if (draggedType != null && e.getButton() == MouseButton.PRIMARY) {
                dropShip(e.getSceneX(), e.getSceneY());
                e.consume();
            }
        });
        root.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
            if (suppressClick && e.getButton() == MouseButton.PRIMARY) e.consume();
        });
        root.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.SECONDARY) toggleOrientation();
        });
        root.requestFocus();

        onViewShown();
        refreshAll();
        return root;
    }

    // ================= hooks (subclass responsibilities) =================

    /** assembles the screen-specific chrome around the shared widgets. */
    protected abstract Pane assembleLayout();

    /** optional root decoration (e.g. the animated ocean backdrop). */
    protected abstract StackPane decorateRoot(Pane layout);

    /** handles the ready button (local: confirm + route; network: socket handshake). */
    protected abstract void onReadyPressed();

    /** confirmation text shown by the shared exit dialog. */
    protected abstract String exitPrompt();

    /** extra teardown once the player confirms exit, before returning to the menu. */
    protected abstract void onExitConfirmed();

    // ================= overridable hooks (sensible defaults) =================

    /** extra reason to keep the ready button disabled (e.g. ready already sent). */
    protected boolean isReadyLocked() { return false; }

    /** called once the screen is fully assembled and wired, before the first refresh. */
    protected void onViewShown() { }

    /** called after every orientation change. */
    protected void onOrientationToggled() { }

    /** called at the end of {@link #refreshall()} for extra per-screen labels. */
    protected void onRefreshed() { }

    /** ready button caption. */
    protected String readyButtonLabel() { return "READY"; }

    // ================= shared behaviour =================

    /** builds the dock tray; subclasses add style class / width in {@link #assemblelayout()}. */
    protected final ShipDockPane createDock() {
        return new ShipDockPane(controller, player);
    }

    /** shared exit button wired to the common confirm dialog. */
    protected final Button buildExitButton() {
        Button exit = new Button("EXIT");
        exit.getStyleClass().add("danger-button");
        exit.setOnAction(e -> {
            audio.playClick();
            confirmExit();
        });
        return exit;
    }

    /** In-game mouse gestures keep keyboard rotation available throughout a drag. */
    protected void setupDragTargets() {
        int size = boardGridPane.getSize();
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                final int row = r, col = c;
                StackPane cell = boardGridPane.getCell(r, c);

                cell.setOnDragDetected(event -> {
                    if (!event.isPrimaryButtonDown() || isReadyLocked()) return;
                    Coordinate from = new Coordinate(row, col);
                    ShipSnapshot ship = shipAt(from);
                    if (ship != null) {
                        orientation = ship.orientation();
                        updateOrientationLabel();
                        beginDrag(ship.type(), from, event);
                        event.consume();
                    }
                });

                cell.setOnMouseEntered(event -> {
                    if (draggedType != null || isReadyLocked()) return;
                    hoverCell = new Coordinate(row, col);
                    ShipType selected = dockPane.getSelectedShip();
                    if (selected != null) {
                        showGhost(row, col, selected);
                    }
                });

                cell.setOnMouseExited(event -> {
                    if (draggedType != null) return;
                    hoverCell = null;
                    clearGhost();
                });

                cell.setOnMouseClicked(event -> {
                    if (event.getButton() != MouseButton.PRIMARY || isReadyLocked() || suppressClick) return;
                    Coordinate clicked = new Coordinate(row, col);
                    // An occupied cell always returns its hull, even with a dock selection.
                    if (shipAt(clicked) != null) {
                        clearGhost();
                        if (controller.removeShipAt(player, clicked)) {
                            dockPane.clearSelection();
                            audio.playRemoveShip();
                            refreshAll();
                        }
                        event.consume();
                        return;
                    }
                    ShipType selected = dockPane.getSelectedShip();
                    if (selected != null) {
                        clearGhost();
                        boolean placed = controller.placeShip(player, selected, clicked, orientation);
                        if (placed) {
                            audio.playPlaceShip();
                            dockPane.clearSelection();
                            refreshAll();
                        } else {
                            shakeCell(cell);
                        }
                    }
                    event.consume();
                });
            }
        }
    }

    private ShipSnapshot shipAt(Coordinate coordinate) {
        return player.fleet().stream().filter(ship -> ship.cells().contains(coordinate)).findFirst().orElse(null);
    }

    private void beginDrag(ShipType type, Coordinate from, MouseEvent event) {
        if (isReadyLocked() || draggedType != null) return;
        draggedType = type;
        draggedFrom = from;
        suppressClick = true;
        dockPane.setDragging(true);
        root.requestFocus();
        renderDragPreview();
        dragPreview.setVisible(true);
        updateDrag(event.getSceneX(), event.getSceneY());
    }

    private void renderDragPreview() {
        double cell = boardGridPane.getCellSize();
        double length = cell * draggedType.getSize() + draggedType.getSize() - 1;
        double width = orientation.isHorizontal() ? length : cell;
        double height = orientation.isHorizontal() ? cell : length;
        dragPreview.getChildren().clear();
        javafx.scene.image.Image sprite = ImageResources.ship(draggedType, orientation);
        if (sprite != null) {
            ImageView image = new ImageView(sprite);
            image.setFitWidth(width);
            image.setFitHeight(height);
            image.setPreserveRatio(false);
            dragPreview.getChildren().add(image);
        } else {
            Label label = new Label(draggedType.name().replace('_', ' '));
            label.getStyleClass().add("accent-text");
            label.setWrapText(true);
            dragPreview.getChildren().add(label);
        }
        dragPreview.resize(width, height);
    }

    private boolean containsPointer(Node node, double sceneX, double sceneY) {
        Point2D point = node.sceneToLocal(sceneX, sceneY);
        return node.contains(point);
    }

    private Coordinate cellAt(double sceneX, double sceneY) {
        for (int r = 0; r < boardGridPane.getSize(); r++) {
            for (int c = 0; c < boardGridPane.getSize(); c++) {
                if (containsPointer(boardGridPane.getCell(r, c), sceneX, sceneY)) return new Coordinate(r, c);
            }
        }
        return null;
    }

    private void updateDrag(double sceneX, double sceneY) {
        pointerX = sceneX;
        pointerY = sceneY;
        hoverCell = cellAt(sceneX, sceneY);
        clearGhost();
        Point2D point = root.sceneToLocal(sceneX, sceneY);
        if (hoverCell != null) {
            showGhost(hoverCell.getRow(), hoverCell.getCol(), draggedType);
            Point2D origin = boardGridPane.getCell(hoverCell).localToScene(0, 0);
            point = root.sceneToLocal(origin);
        }
        dragPreview.relocate(point.getX(), point.getY());
        boolean overDock = containsPointer(dockPane, sceneX, sceneY);
        dockPane.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("drop-target"), overDock);
    }

    private void dropShip(double sceneX, double sceneY) {
        if (!isReadyLocked()) {
            Coordinate destination = cellAt(sceneX, sceneY);
            if (containsPointer(dockPane, sceneX, sceneY)) {
                if (draggedFrom != null && controller.removeShipAt(player, draggedFrom)) audio.playRemoveShip();
            } else if (destination != null) {
                boolean placed = draggedFrom == null
                        ? controller.placeShip(player, draggedType, destination, orientation)
                        : controller.moveShip(player, draggedFrom, destination, orientation);
                if (placed) audio.playPlaceShip();
                else shakeCell(boardGridPane.getCell(destination));
            }
        }
        finishDrag();
    }

    /** No domain mutation happens until a valid drop, so cancel needs no rollback. */
    private void finishDrag() {
        clearGhost();
        draggedType = null;
        draggedFrom = null;
        hoverCell = null;
        suppressClick = true;
        dragPreview.setVisible(false);
        dockPane.setDragging(false);
        dockPane.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("drop-target"), false);
        dockPane.clearSelection();
        refreshAll();
    }

    private void showGhost(int row, int col, ShipType type) {
        clearGhost();
        boolean valid = draggedFrom == null
                ? controller.canPlace(player, type, new Coordinate(row, col), orientation)
                : controller.canMoveShip(player, draggedFrom, new Coordinate(row, col), orientation);
        String stateClass = valid ? BoardGridPane.GHOST_VALID : BoardGridPane.GHOST_INVALID;
        for (int i = 0; i < type.getSize(); i++) {
            int gr = orientation.isHorizontal() ? row : row + i;
            int gc = orientation.isHorizontal() ? col + i : col;
            if (gr < 0 || gr >= boardGridPane.getSize() || gc < 0 || gc >= boardGridPane.getSize()) continue;
            Coordinate ghostCoord = new Coordinate(gr, gc);
            boardGridPane.setCellState(ghostCoord, stateClass);
            ghostCells.add(ghostCoord);
        }
    }

    private void clearGhost() {
        for (Coordinate c : ghostCells) {
            boardGridPane.resetCellStyle(c.getRow(), c.getCol());
        }
        ghostCells.clear();
        // re-render any already-placed ships that may have been under the ghost.
        for (com.battleship.model.projection.ShipSnapshot s : player.fleet()) {
            boardGridPane.renderShip(s);
        }
    }

    private void shakeCell(StackPane cell) {
        TranslateTransition shake = new TranslateTransition(Duration.millis(50), cell);
        shake.setByX(4);
        shake.setCycleCount(6);
        shake.setAutoReverse(true);
        shake.play();
    }

    /** re-renders the board, dock and counter, then re-evaluates the ready gating. */
    protected void refreshAll() {
        boardGridPane.clearAll();
        for (com.battleship.model.projection.ShipSnapshot s : player.fleet()) {
            boardGridPane.renderShip(s);
        }
        dockPane.refresh();
        int placed = player.deployedShipCount();
        int total = controller.getSelectedTheater().getTotalShipCount();
        countLabel.setText("Ships placed: " + placed + " / " + total);
        readyButton.setDisable(!controller.isPlacementComplete(player) || isReadyLocked());
        onRefreshed();
    }

    protected void toggleOrientation() {
        if (isReadyLocked()) return;
        orientation = orientation.toggle();
        updateOrientationLabel();
        if (draggedType != null) {
            renderDragPreview();
            updateDrag(pointerX, pointerY);
        } else if (hoverCell != null && dockPane.getSelectedShip() != null) {
            showGhost(hoverCell.getRow(), hoverCell.getCol(), dockPane.getSelectedShip());
        }
        onOrientationToggled();
    }

    protected void updateOrientationLabel() {
        orientationLabel.setText("Current orientation: " + (orientation.isHorizontal() ? "HORIZONTAL" : "VERTICAL"));
    }

    private void confirmExit() {
        if (AlertUtil.showConfirmation(nav.window(), "Exit Game", exitPrompt())) {
            onExitConfirmed();
            audio.stopBgm();
            audio.playMenuMusic();
            nav.showMainMenu();
        }
    }
}
