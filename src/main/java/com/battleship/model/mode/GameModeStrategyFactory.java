package com.battleship.model.mode;

/**
 * factory for instantiating decoupled game mode strategies.
 * strictly pure domain class with zero JavaFX imports.
 */
public final class GameModeStrategyFactory {

    private GameModeStrategyFactory() {
    }

    public static GameModeStrategy classic() {
        return new ClassicModeStrategy();
    }

    public static GameModeStrategy convoy() {
        return new ConvoyModeStrategy();
    }

    public static GameModeStrategy minefield() {
        return new MinefieldModeStrategy();
    }

    public static GameModeStrategy blitz() {
        return new BlitzModeStrategy();
    }

    public static GameModeStrategy create(String modeName) {
        if (modeName == null) return classic();
        return switch (modeName.trim().toUpperCase()) {
            case "CONVOY" -> convoy();
            case "MINEFIELD" -> minefield();
            case "BLITZ" -> blitz();
            default -> classic();
        };
    }
}
