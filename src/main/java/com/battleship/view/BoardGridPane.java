package com.battleship.view;

import com.battleship.model.CellStatus;
import com.battleship.model.Coordinate;
import com.battleship.model.Orientation;
import com.battleship.model.ShipType;
import com.battleship.model.projection.ShipSnapshot;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Label;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Line;
import javafx.util.Duration;

import java.util.List;

/**
 * reusable board grid used by the placement screens, both battle screens and the
 * game-over screens.
 *
 * <p>ships are painted from immutable {@link shipsnapshot}s (v1.2) — this widget
 * never receives a mutable domain entity, so a ui component cannot alter the
 * game state it renders.</p>
 */
public class BoardGridPane extends GridPane {

    /** base style class carried by every cell; state classes are toggled on top. */
    public static final String CELL_CLASS = "board-cell";

    /** ghost-preview classes, usable via {@link #setcellstate(coordinate, string)}. */
    public static final String GHOST_VALID = "board-cell-ghost-valid";
    public static final String GHOST_INVALID = "board-cell-ghost-invalid";
    public static final String GHOST_TARGET = "board-cell-ghost-target";

    private static final String CELL_SHIP = "board-cell-ship";
    private static final String CELL_MISS = "board-cell-miss";
    private static final String CELL_HIT = "board-cell-hit";
    private static final String CELL_SUNK = "board-cell-sunk";

    /** every class this grid may toggle on a cell; cleared before a new state is applied. */
    private static final String[] CELL_STATE_CLASSES = {
            CELL_SHIP, CELL_MISS, CELL_HIT, CELL_SUNK,
            GHOST_VALID, GHOST_INVALID, GHOST_TARGET
    };

    private final int size;
    private final StackPane[][] cells;
    private double cellPx;

    public static double computeCellSize(int size) {
        if (size <= 5) return 56;
        if (size <= 8) return 44;
        return 38;
    }

    public BoardGridPane(int size) {
        this(size, computeCellSize(size));
    }

    public BoardGridPane(int size, double cellPx) {
        this.size = size;
        this.cells = new StackPane[size][size];
        this.cellPx = cellPx;
        setHgap(1);
        setVgap(1);
        build();
    }

    public double getCellSize() { return cellPx; }

    public void setCellSize(double newCellPx) {
        if (newCellPx <= 0 || Math.abs(this.cellPx - newCellPx) < 0.5) return;
        this.cellPx = newCellPx;
        double span = newCellPx * 0.32;
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                StackPane cell = cells[r][c];
                cell.setPrefSize(newCellPx, newCellPx);
                cell.setMinSize(newCellPx, newCellPx);
                cell.setMaxSize(newCellPx, newCellPx);
                for (javafx.scene.Node node : cell.getChildren()) {
                    if (node instanceof ImageView iv) {
                        if (iv.getViewport() != null) {
                            iv.setFitWidth(newCellPx);
                            iv.setFitHeight(newCellPx);
                        } else if (cell.getStyleClass().contains(CELL_MISS)) {
                            iv.setFitWidth(newCellPx * 0.75);
                            iv.setFitHeight(newCellPx * 0.75);
                        } else {
                            iv.setFitWidth(newCellPx * 0.85);
                            iv.setFitHeight(newCellPx * 0.85);
                        }
                    } else if (node instanceof Line line) {
                        if (line.getStartX() < 0 && line.getStartY() < 0) {
                            line.setStartX(-span);
                            line.setStartY(-span);
                            line.setEndX(span);
                            line.setEndY(span);
                        } else {
                            line.setStartX(-span);
                            line.setStartY(span);
                            line.setEndX(span);
                            line.setEndY(-span);
                        }
                    }
                }
            }
        }
    }

    private void build() {
        double cellPx = this.cellPx;
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                StackPane cell = new StackPane();
                cell.setPrefSize(cellPx, cellPx);
                cell.setMinSize(cellPx, cellPx);
                cell.setMaxSize(cellPx, cellPx);
                cell.getStyleClass().add(CELL_CLASS);
                cells[r][c] = cell;
                add(cell, c, r);
            }
        }
    }

    public StackPane getCell(int row, int col) { return cells[row][col]; }
    public StackPane getCell(Coordinate c) { return cells[c.getRow()][c.getCol()]; }
    public int getSize() { return size; }

    /** Coordinate headers remain aligned as the board resizes. */
    public GridPane withCoordinates() {
        GridPane wrapper = new GridPane();
        wrapper.setHgap(4); wrapper.setVgap(4);
        javafx.scene.layout.HBox columns = new javafx.scene.layout.HBox(1);
        javafx.scene.layout.VBox rows = new javafx.scene.layout.VBox(1);
        for (int i = 0; i < size; i++) {
            Label column = new Label(String.valueOf((char) ('A' + i)));
            column.getStyleClass().add("board-coordinate");
            column.setAlignment(Pos.CENTER);
            column.prefWidthProperty().bind(cells[0][i].prefWidthProperty());
            column.minWidthProperty().bind(column.prefWidthProperty());
            column.maxWidthProperty().bind(column.prefWidthProperty());
            column.setPrefHeight(20);
            columns.getChildren().add(column);
            Label row = new Label(Integer.toString(i + 1));
            row.getStyleClass().add("board-coordinate");
            row.setAlignment(Pos.CENTER);
            row.setPrefWidth(20);
            row.prefHeightProperty().bind(cells[i][0].prefHeightProperty());
            row.minHeightProperty().bind(row.prefHeightProperty());
            row.maxHeightProperty().bind(row.prefHeightProperty());
            rows.getChildren().add(row);
        }
        wrapper.add(columns, 1, 0); wrapper.add(rows, 0, 1); wrapper.add(this, 1, 1);
        return wrapper;
    }

    public void resetCellStyle(int row, int col) {
        applyCellState(cells[row][col], null);
    }

    /** applies a cell state (or {@code null} for the plain base cell) as a style class. */
    private void applyCellState(StackPane cell, String stateClass) {
        cell.getStyleClass().removeAll(CELL_STATE_CLASSES);
        if (stateClass != null) cell.getStyleClass().add(stateClass);
    }

    /** applies one of the ghost/state classes to a cell; used by the ghost previews. */
    public void setCellState(Coordinate c, String stateClass) {
        applyCellState(cells[c.getRow()][c.getCol()], stateClass);
    }

    /** renders a placed (not-yet-shot) ship, used during placement and on own-fleet boards. */
    public void renderShip(ShipSnapshot ship) {
        Orientation orientation = ship.orientation();
        Image sprite = ImageResources.ship(ship.type(), orientation);
        List<Coordinate> occupied = ship.cells();
        int len = occupied.size();

        for (int i = 0; i < len; i++) {
            Coordinate c = occupied.get(i);
            StackPane cell = cells[c.getRow()][c.getCol()];
            cell.getChildren().clear();
            applyCellState(cell, CELL_SHIP);

            if (sprite != null) {
                // the source art is a single square image per hull; slice out the
                // portion that belongs to this cell along the ship's long axis.
                boolean horizontal = orientation.isHorizontal();
                double sliceW = horizontal ? sprite.getWidth() / len : sprite.getWidth();
                double sliceH = horizontal ? sprite.getHeight() : sprite.getHeight() / len;
                double x = horizontal ? i * sliceW : 0;
                double y = horizontal ? 0 : i * sliceH;

                ImageView iv = new ImageView(sprite);
                iv.setViewport(new Rectangle2D(x, y, sliceW, sliceH));
                iv.setMouseTransparent(true);
                iv.setFitWidth(cellPx);
                iv.setFitHeight(cellPx);
                iv.setPreserveRatio(false);
                cell.getChildren().add(iv);
            }
        }
    }

    /**
     * plays a quick fire-1 -> fire-2 -> fire-3 -> hit-explosion flipbook in the
     * given cell. returns false (and adds nothing) if any frame is missing, so
     * the caller can fall back to the static explosion image or the plain marker.
     */
    private boolean playFireFlipbook(StackPane cell) {
        Image f1 = ImageResources.effect("fire-1");
        Image f2 = ImageResources.effect("fire-2");
        Image f3 = ImageResources.effect("fire-3");
        Image explosion = ImageResources.effect("hit-explosion");
        if (f1 == null || f2 == null || f3 == null || explosion == null) return false;

        ImageView iv = new ImageView(f1);
        iv.setFitWidth(cellPx * 0.85);
        iv.setFitHeight(cellPx * 0.85);
        iv.setPreserveRatio(true);
        iv.setMouseTransparent(true);
        cell.getChildren().add(iv);

        iv.setScaleX(0.5);
        iv.setScaleY(0.5);
        ScaleTransition st = new ScaleTransition(Duration.millis(260), iv);
        st.setToX(1.0);
        st.setToY(1.0);
        st.play();

        Timeline flipbook = new Timeline(
                new KeyFrame(Duration.millis(80), e -> iv.setImage(f2)),
                new KeyFrame(Duration.millis(160), e -> iv.setImage(f3)),
                new KeyFrame(Duration.millis(250), e -> iv.setImage(explosion))
        );
        flipbook.play();
        return true;
    }

    /** renders a miss or a non-fatal hit with animation. */
    public void renderShot(Coordinate coord, CellStatus status) {
        renderShot(coord, status, true);
    }

    /** renders a miss or a non-fatal hit, with optional animation. */
    public void renderShot(Coordinate coord, CellStatus status, boolean animate) {
        StackPane cell = cells[coord.getRow()][coord.getCol()];

        // Check if there is an existing ship hull in this cell (e.g. own fleet or revealed fleet)
        ImageView existingShip = null;
        for (javafx.scene.Node node : cell.getChildren()) {
            if (node instanceof ImageView iv && iv.getViewport() != null) {
                existingShip = iv;
                break;
            }
        }

        cell.getChildren().clear();

        if (status == CellStatus.MISS) {
            applyCellState(cell, CELL_MISS);
            Image splash = ImageResources.effect("miss-splash");
            if (splash != null) {
                ImageView iv = new ImageView(splash);
                iv.setFitWidth(cellPx * 0.75);
                iv.setFitHeight(cellPx * 0.75);
                iv.setPreserveRatio(true);
                iv.setMouseTransparent(true);
                cell.getChildren().add(iv);

                if (animate) {
                    iv.setScaleX(0.25);
                    iv.setScaleY(0.25);
                    iv.setOpacity(0.35);
                    ScaleTransition st = new ScaleTransition(Duration.millis(260), iv);
                    st.setToX(1.0);
                    st.setToY(1.0);
                    FadeTransition ft = new FadeTransition(Duration.millis(260), iv);
                    ft.setToValue(1.0);
                    st.play();
                    ft.play();
                }
            } else {
                javafx.scene.shape.Circle dot = new javafx.scene.shape.Circle();
                dot.radiusProperty().bind(cell.prefWidthProperty().multiply(0.065));
                dot.setFill(javafx.scene.paint.Color.web("#7197b1"));
                dot.setMouseTransparent(true);
                cell.getChildren().add(dot);
            }
        } else if (status == CellStatus.HIT) {
            applyCellState(cell, CELL_HIT);
            if (existingShip != null) {
                // Dim/scorch the damaged ship segment so it's visibly damaged beneath the fire
                ColorAdjust burn = new ColorAdjust();
                burn.setBrightness(-0.25);
                burn.setSaturation(-0.2);
                existingShip.setEffect(burn);
                cell.getChildren().add(existingShip);
            }

            boolean played = animate && playFireFlipbook(cell);
            if (!played) {
                Image explosion = ImageResources.effect("hit-explosion");
                if (explosion != null) {
                    ImageView iv = new ImageView(explosion);
                    iv.setFitWidth(cellPx * 0.85);
                    iv.setFitHeight(cellPx * 0.85);
                    iv.setPreserveRatio(true);
                    iv.setMouseTransparent(true);
                    cell.getChildren().add(iv);

                    if (animate) {
                        iv.setScaleX(0.5);
                        iv.setScaleY(0.5);
                        ScaleTransition st = new ScaleTransition(Duration.millis(220), iv);
                        st.setToX(1.0);
                        st.setToY(1.0);
                        st.play();
                    }
                } else {
                    Label marker = new Label("◆");
                    marker.getStyleClass().add("battle-hit-marker");
                    marker.styleProperty().bind(cell.prefWidthProperty().multiply(0.42).asString("-fx-font-size: %.1fpx;"));
                    marker.setMouseTransparent(true);
                    cell.getChildren().add(marker);
                }
            }
        } else if (status == CellStatus.SUNK) {
            renderSunkShip(List.of(coord), animate);
        }
    }

    /** Quiet wreck markers keep an endgame board readable. */
    public void renderSunkShip(ShipSnapshot ship) {
        renderSunkShip(ship, true);
    }

    /** Renders a sunk ship with hull sprite and damage effects. */
    public void renderSunkShip(ShipSnapshot ship, boolean animate) {
        if (ship == null) return;
        renderSunkShip(ship.type(), ship.orientation(), ship.cells(), animate);
    }

    /** Quiet wreck markers keep an endgame board readable. */
    public void renderSunkShip(List<Coordinate> occupiedCells) {
        renderSunkShip(occupiedCells, true);
    }

    /** Quiet wreck markers keep an endgame board readable, with optional animation. */
    public void renderSunkShip(List<Coordinate> occupiedCells, boolean animate) {
        if (occupiedCells == null || occupiedCells.isEmpty()) return;
        Orientation orientation = (occupiedCells.size() >= 2 && occupiedCells.get(0).getRow() != occupiedCells.get(1).getRow())
                ? Orientation.VERTICAL : Orientation.HORIZONTAL;
        ShipType type = switch (occupiedCells.size()) {
            case 5 -> ShipType.CARRIER;
            case 4 -> ShipType.BATTLESHIP;
            case 3 -> ShipType.CRUISER;
            default -> ShipType.DESTROYER;
        };
        renderSunkShip(type, orientation, occupiedCells, animate);
    }

    /**
     * Renders a sunk ship by revealing its hull (scorched/underwater) and placing
     * the explosion effect on top.
     */
    public void renderSunkShip(ShipType type, Orientation orientation, List<Coordinate> occupiedCells, boolean animate) {
        if (occupiedCells == null || occupiedCells.isEmpty()) return;
        if (orientation == null) {
            orientation = (occupiedCells.size() >= 2 && occupiedCells.get(0).getRow() != occupiedCells.get(1).getRow())
                    ? Orientation.VERTICAL : Orientation.HORIZONTAL;
        }
        int len = occupiedCells.size();
        Image sprite = (type != null) ? ImageResources.ship(type, orientation) : null;
        Image explosion = ImageResources.effect("hit-explosion");

        for (int i = 0; i < len; i++) {
            Coordinate c = occupiedCells.get(i);
            StackPane cell = cells[c.getRow()][c.getCol()];

            ImageView existingShip = null;
            for (javafx.scene.Node node : cell.getChildren()) {
                if (node instanceof ImageView iv && iv.getViewport() != null) {
                    existingShip = iv;
                    break;
                }
            }

            cell.getChildren().clear();
            applyCellState(cell, CELL_SUNK);

            if (existingShip != null) {
                // If ship already existed on the board (e.g. own fleet), burn/darken it
                ColorAdjust burn = new ColorAdjust();
                burn.setBrightness(-0.35);
                burn.setSaturation(-0.25);
                existingShip.setEffect(burn);
                cell.getChildren().add(existingShip);
            } else if (sprite != null) {
                // Reveal the sunk enemy ship hull in enemy waters
                boolean horizontal = orientation.isHorizontal();
                double sliceW = horizontal ? sprite.getWidth() / len : sprite.getWidth();
                double sliceH = horizontal ? sprite.getHeight() : sprite.getHeight() / len;
                double x = horizontal ? i * sliceW : 0;
                double y = horizontal ? 0 : i * sliceH;

                ImageView iv = new ImageView(sprite);
                iv.setViewport(new Rectangle2D(x, y, sliceW, sliceH));
                iv.setMouseTransparent(true);
                iv.setFitWidth(cellPx);
                iv.setFitHeight(cellPx);
                iv.setPreserveRatio(false);

                ColorAdjust burn = new ColorAdjust();
                burn.setBrightness(-0.35);
                burn.setSaturation(-0.25);
                iv.setEffect(burn);
                cell.getChildren().add(iv);
            }

            if (explosion != null) {
                ImageView iv = new ImageView(explosion);
                iv.setFitWidth(cellPx * 0.82);
                iv.setFitHeight(cellPx * 0.82);
                iv.setPreserveRatio(true);
                iv.setMouseTransparent(true);
                iv.setOpacity(0.85);
                cell.getChildren().add(iv);
            } else {
                Label marker = new Label("×");
                marker.getStyleClass().add("battle-sunk-marker");
                marker.styleProperty().bind(cell.prefWidthProperty().multiply(0.65).asString("-fx-font-size: %.1fpx;"));
                marker.setMouseTransparent(true);
                cell.getChildren().add(marker);
            }

            if (animate) {
                cell.setScaleX(1.12);
                cell.setScaleY(1.12);
                ScaleTransition st = new ScaleTransition(Duration.millis(300), cell);
                st.setToX(1.0);
                st.setToY(1.0);
                st.play();
            }
        }
    }
    public void clearAll() {
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                cells[r][c].getChildren().clear();
                applyCellState(cells[r][c], null);
            }
        }
    }
}
