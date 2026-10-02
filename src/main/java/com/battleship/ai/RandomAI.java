package com.battleship.ai;

import com.battleship.model.Coordinate;
import com.battleship.model.ShotResult;
import com.battleship.model.fog.TrackingGrid;

import java.security.SecureRandom;
import java.util.List;

/** ensign (easy) difficulty: uniform random selection over unshot cells. */
public class RandomAI implements AIStrategy {

    private final SecureRandom random = new SecureRandom();

    @Override
    public Coordinate chooseTarget(TrackingGrid knowledge) {
        List<Coordinate> unshot = knowledge.unshotCells();
        return unshot.get(random.nextInt(unshot.size()));
    }

    @Override
    public void notifyResult(ShotResult result) {
        // no state to update.
    }
}
