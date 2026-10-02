package com.battleship.ai;

/** ai difficulty tiers, mapped to concrete aistrategy implementations by aifactory. */
public enum Difficulty {
    ENSIGN,     // easy   -> randomai
    LIEUTENANT, // normal -> hunttargetai
    ADMIRAL     // hard   -> smartai
}
