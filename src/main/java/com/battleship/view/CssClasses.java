package com.battleship.view;

/**
 * centralized constant definitions for css style classes used throughout the ui (fixes v10).
 * prevents magic strings and typo bugs across view components.
 */
public final class CssClasses {

    private CssClasses() { } // prevent instantiation

    // buttons
    public static final String PRIMARY_BUTTON   = "primary-button";
    public static final String FEATURED_BUTTON  = "featured-button";
    public static final String GHOST_BUTTON     = "ghost-button";
    public static final String DANGER_BUTTON    = "danger-button";
    public static final String WEAPON_BUTTON    = "weapon-button";

    // weapon button states
    public static final String WEAPON_DISABLED  = "weapon-button-disabled";
    public static final String WEAPON_SELECTED  = "weapon-button-selected";
    public static final String WEAPON_ENABLED   = "weapon-button-enabled";

    // typography
    public static final String APP_TITLE        = "app-title";
    public static final String APP_SUBTITLE     = "app-subtitle";
    public static final String INFO_TEXT        = "info-text";
    public static final String DIM_TEXT         = "dim-text";
    public static final String ACCENT_TEXT      = "accent-text";

    // panels & cards
    public static final String CARD_PANEL       = "card-panel";
    public static final String BOARD_CARD       = "board-card";
    public static final String BOARD_CARD_TITLE = "board-card-title";
    public static final String SIDE_CARD        = "side-card";
    public static final String WEAPON_CONSOLE_CARD = "weapon-console-card";
    public static final String SIDE_CARD_TITLE  = "side-card-title";
}

