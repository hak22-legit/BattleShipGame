package com.battleship.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

/** the turn enum replaces the raw 0/1 turn index (p1). */
class TurnTest {

    @Test
    void nextAlternatesBetweenPlayers() {
        assertEquals(Turn.PLAYER_2, Turn.PLAYER_1.next());
        assertEquals(Turn.PLAYER_1, Turn.PLAYER_2.next());
        assertNotSame(Turn.PLAYER_1, Turn.PLAYER_1.next());
    }
}
