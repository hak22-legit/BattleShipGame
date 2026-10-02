package com.battleship.model;

/** selected game mode from gamemodeselectview. */
public enum GameMode {
    AI_EASY("vs AI - Easy"),
    AI_NORMAL("vs AI - Normal"),
    AI_HARD("vs AI - Hard"),
    HOTSEAT("1v1 Hotseat"),
    ONLINE("Play with a Friend");

    private final String label;

    GameMode(String label) { this.label = label; }

    public String getLabel() { return label; }

    public boolean isVsAi() { return this != HOTSEAT && this != ONLINE; }
}
