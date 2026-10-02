package com.battleship.model;

/** high-level phase of the overall game flow. */
public enum GameState {
    MAIN_MENU,
    MODE_SELECT,
    BOARD_SELECT,
    SHIP_PLACEMENT,
    PASS_SCREEN,
    BATTLE,
    GAME_OVER
}
