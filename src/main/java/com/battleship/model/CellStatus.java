package com.battleship.model;

/** state of a single grid cell. */
public enum CellStatus {
    EMPTY,
    SHIP,
    HIT,
    MISS,
    SUNK
}
