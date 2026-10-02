package com.battleship.model;

import java.util.function.LongSupplier;

/** Monotonic turn deadline; a late UI pulse still expires exactly once. */
public final class TurnDeadline {
    public static final int TURN_SECONDS = 60;
    private final LongSupplier nanoTime;
    private long deadline;
    private boolean running;
    public TurnDeadline() { this(System::nanoTime); }
    public TurnDeadline(LongSupplier nanoTime) { this.nanoTime = nanoTime; }
    public void start() { deadline = nanoTime.getAsLong() + TURN_SECONDS * 1_000_000_000L; running = true; }
    public void stop() { running = false; }
    public int secondsLeft() { return (int) Math.max(0, Math.ceil((deadline - nanoTime.getAsLong()) / 1_000_000_000.0)); }
    public boolean expireIfDue() {
        if (!running || secondsLeft() > 0) return false;
        running = false;
        return true;
    }
}
