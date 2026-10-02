package com.battleship.model;

import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

class TurnDeadlineTest {
    @Test void expiresOnceAfterSixtySecondsAndResetsForNextTurn() {
        AtomicLong time = new AtomicLong();
        TurnDeadline deadline = new TurnDeadline(time::get);
        deadline.start();
        assertEquals(60, deadline.secondsLeft());
        time.set(59_999_000_000L);
        assertFalse(deadline.expireIfDue());
        assertEquals(1, deadline.secondsLeft());
        time.set(60_000_000_000L);
        assertTrue(deadline.expireIfDue());
        assertFalse(deadline.expireIfDue());
        deadline.start();
        assertEquals(60, deadline.secondsLeft());
    }
    @Test void stoppedClockDoesNotExpireEvenAfterDelayedPulse() {
        AtomicLong time = new AtomicLong();
        TurnDeadline deadline = new TurnDeadline(time::get);
        deadline.start(); deadline.stop();
        time.set(90_000_000_000L);
        assertFalse(deadline.expireIfDue());
    }
}
