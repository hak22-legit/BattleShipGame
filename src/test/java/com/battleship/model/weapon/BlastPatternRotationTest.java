package com.battleship.model.weapon;

import com.battleship.model.Coordinate;
import com.battleship.model.Orientation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlastPatternRotationTest {

    @Test
    void horizontalPatternCoverageMatchesDimensions() {
        BlastPattern salvo = BlastPattern.of(1, 3);
        List<Coordinate> cells = salvo.coverage(new Coordinate(2, 2));

        assertEquals(3, cells.size());
        assertTrue(cells.contains(new Coordinate(2, 2)));
        assertTrue(cells.contains(new Coordinate(2, 3)));
        assertTrue(cells.contains(new Coordinate(2, 4)));
    }

    @Test
    void verticalPatternCoverageTransposesCorrectly() {
        BlastPattern salvo = BlastPattern.of(1, 3);
        BlastPattern vertical = salvo.rotatedTo(Orientation.VERTICAL);

        assertEquals(3, vertical.rows());
        assertEquals(1, vertical.cols());

        // parameterless coverage on the already-rotated pattern
        List<Coordinate> cells = vertical.coverage(new Coordinate(2, 2));
        assertEquals(3, cells.size());
        assertTrue(cells.contains(new Coordinate(2, 2)));
        assertTrue(cells.contains(new Coordinate(3, 2)));
        assertTrue(cells.contains(new Coordinate(4, 2)));

        // base pattern with orientation.vertical produces the same result
        List<Coordinate> baseOriented = salvo.coverage(new Coordinate(2, 2), Orientation.VERTICAL);
        assertEquals(cells, baseOriented);
    }

    @Test
    void nuclearPatternRotatesProperly() {
        BlastPattern nuclear = BlastPattern.of(2, 3); // 2 rows, 3 cols
        BlastPattern vertical = nuclear.rotatedTo(Orientation.VERTICAL); // 3 rows, 2 cols

        assertEquals(3, vertical.rows());
        assertEquals(2, vertical.cols());

        List<Coordinate> cells = vertical.coverage(new Coordinate(1, 1));
        assertEquals(6, cells.size());
        assertTrue(cells.contains(new Coordinate(1, 1)));
        assertTrue(cells.contains(new Coordinate(1, 2)));
        assertTrue(cells.contains(new Coordinate(2, 1)));
        assertTrue(cells.contains(new Coordinate(2, 2)));
        assertTrue(cells.contains(new Coordinate(3, 1)));
        assertTrue(cells.contains(new Coordinate(3, 2)));
    }
}

