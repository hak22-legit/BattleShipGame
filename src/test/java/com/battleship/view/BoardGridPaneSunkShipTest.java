package com.battleship.view;

import com.battleship.model.Coordinate;
import com.battleship.model.Orientation;
import com.battleship.model.ShipType;
import com.battleship.model.projection.ShipSnapshot;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BoardGridPaneSunkShipTest {

    @BeforeAll
    static void initFx() throws Exception {
        FxTestSupport.startToolkit();
    }

    @Test
    void testRenderSunkShipRevealsEnemyShipHull() throws Exception {
        FxTestSupport.onFxThread(() -> {
            BoardGridPane grid = new BoardGridPane(10, 40);

            // Simulate enemy waters: sinking a 3-cell Cruiser (horizontal)
            List<Coordinate> cells = List.of(
                    new Coordinate(3, 4),
                    new Coordinate(3, 5),
                    new Coordinate(3, 6)
            );
            ShipSnapshot cruiser = new ShipSnapshot(ShipType.CRUISER, cells, Orientation.HORIZONTAL, 3, true);

            grid.renderSunkShip(cruiser, false);

            for (Coordinate c : cells) {
                StackPane cell = grid.getCell(c.getRow(), c.getCol());
                assertTrue(cell.getStyleClass().contains("board-cell-sunk"), "Cell should have board-cell-sunk style");

                // Check that ship hull graphic (ImageView with viewport) is present
                boolean hasShipHull = cell.getChildren().stream()
                        .anyMatch(node -> node instanceof ImageView iv && iv.getViewport() != null);
                assertTrue(hasShipHull, "Cell " + c + " should contain revealed ship hull sprite");

                // Check that explosion effect is present
                boolean hasExplosion = cell.getChildren().stream()
                        .anyMatch(node -> node instanceof ImageView iv && iv.getViewport() == null);
                assertTrue(hasExplosion, "Cell " + c + " should contain explosion effect");

                // Check that there is no redundant text marker overlapping the explosion
                boolean hasTextMarker = cell.getChildren().stream()
                        .anyMatch(node -> node instanceof Label);
                assertFalse(hasTextMarker, "Cell " + c + " should not have redundant label over explosion");
            }
        });
    }

    @Test
    void testRenderSunkShipWithCoordinateList() throws Exception {
        FxTestSupport.onFxThread(() -> {
            BoardGridPane grid = new BoardGridPane(10, 40);

            List<Coordinate> cells = List.of(
                    new Coordinate(1, 2),
                    new Coordinate(2, 2)
            );

            grid.renderSunkShip(cells, false);

            for (Coordinate c : cells) {
                StackPane cell = grid.getCell(c.getRow(), c.getCol());
                assertTrue(cell.getStyleClass().contains("board-cell-sunk"));

                boolean hasShipHull = cell.getChildren().stream()
                        .anyMatch(node -> node instanceof ImageView iv && iv.getViewport() != null);
                assertTrue(hasShipHull, "Should deduce ship and render hull sprite");
            }
        });
    }
}
