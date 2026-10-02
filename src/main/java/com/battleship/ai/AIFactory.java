package com.battleship.ai;

import com.battleship.model.GameMode;

/** factory that instantiates the correct aistrategy for a given difficulty or gamemode. */
public class AIFactory {

    private AIFactory() { } // prevent instantiation

    public static AIStrategy create(Difficulty difficulty) {
        return switch (difficulty) {
            case ENSIGN -> new RandomAI();
            case LIEUTENANT -> new HuntTargetAI();
            case ADMIRAL -> new SmartAI();
        };
    }

    /** returns null for hotseat, online, or null input — no ai needed. prefer {@link #createoptional(gamemode)}. */
    public static AIStrategy create(GameMode mode) {
        if (mode == null || mode == GameMode.HOTSEAT || mode == GameMode.ONLINE) {
            return null;
        }
        return switch (mode) {
            case AI_EASY -> new RandomAI();
            case AI_NORMAL -> new HuntTargetAI();
            case AI_HARD -> new SmartAI();
            default -> null;
        };
    }

    /** returns an optional aistrategy, empty for hotseat. */
    public static java.util.Optional<AIStrategy> createOptional(GameMode mode) {
        return java.util.Optional.ofNullable(create(mode));
    }
}

