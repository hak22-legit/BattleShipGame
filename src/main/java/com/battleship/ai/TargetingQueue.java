package com.battleship.ai;

import com.battleship.model.Coordinate;
import com.battleship.model.fog.TrackingGrid;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * encapsulates the target-following queue used by hunt/target ai strategies.
 * once a hit is registered, orthogonal neighbors are enqueued so the ai
 * "follows the line" on subsequent turns.
 *
 * <p>validity is checked against the attacker's {@link trackinggrid}, so the queue
 * never needs (and never gets) the defender's real board.</p>
 */
public class TargetingQueue {

    private final Deque<Coordinate> queue = new ArrayDeque<>();

    /** returns true if there are queued target cells to pursue. */
    public boolean hasTargets() {
        return !queue.isEmpty();
    }

    /**
     * returns the next valid (unshot) target from the queue, or null if the
     * queue is exhausted (all queued cells have already been shot).
     */
    public Coordinate nextTarget(TrackingGrid knowledge) {
        while (!queue.isEmpty()) {
            Coordinate c = queue.poll();
            if (!knowledge.isAlreadyShelled(c)) return c;
        }
        return null;
    }

    /**
     * enqueues the four orthogonal neighbors of the given coordinate,
     * filtering out any that fall outside the board.
     */
    public void enqueueNeighbors(Coordinate c, int boardSize) {
        int[][] deltas = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] d : deltas) {
            Coordinate n = new Coordinate(c.getRow() + d[0], c.getCol() + d[1]);
            if (n.isWithinBounds(boardSize)) {
                queue.add(n);
            }
        }
    }

    /** clears all queued targets (e.g. after a ship is fully sunk). */
    public void clear() {
        queue.clear();
    }
}
