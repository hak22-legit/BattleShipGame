package com.battleship.ai;

import com.battleship.model.Coordinate;
import com.battleship.model.fog.TrackingGrid;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * stateless hunt-mode searcher: picks a random unshot cell using checkerboard
 * parity — exploiting the fact that the smallest ship occupies 2 cells, so at
 * least one cell of every ship lies on a (row + col)-even square.
 *
 * <p>shared by {@link hunttargetai} and {@link smartai} so the search heuristic
 * lives in exactly one place. reads the {@link trackinggrid} (knowledge), not the
 * enemy's real grid.</p>
 */
public final class ParityHunter {

    private ParityHunter() { }

    /**
     * picks a random cell to hunt on, preferring even (row + col) parity.
     *
     * @param knowledge the attacker's knowledge of the defender's waters
     * @param random    the strategy's randomness source
     * @return an unshot coordinate; even-parity when any remains available
     */
    public static Coordinate pick(TrackingGrid knowledge, Random random) {
        List<Coordinate> unshot = knowledge.unshotCells();
        List<Coordinate> parity = new ArrayList<>();
        for (Coordinate c : unshot) {
            if ((c.getRow() + c.getCol()) % 2 == 0) parity.add(c);
        }
        List<Coordinate> pool = parity.isEmpty() ? unshot : parity;
        return pool.get(random.nextInt(pool.size()));
    }
}
