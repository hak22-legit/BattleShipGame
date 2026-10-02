package com.battleship.view.battle;

import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * newest-first attack log — one of the autonomous components a battle screen is
 * composed from (replaces part of the {@code abstractbattleview} template method).
 */
public final class BattleLog {

    private static final int MAX_ENTRIES = 50;

    private final VBox entries = new VBox(6);
    private final VBox card;

    public BattleLog() {
        entries.setFillWidth(true);

        ScrollPane scroll = new ScrollPane(entries);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(200);
        scroll.setPannable(true);
        scroll.getStyleClass().add("attack-log-scroll");

        Label title = new Label("ATTACK LOG");
        title.getStyleClass().add("side-card-title");

        card = new VBox(10, title, scroll);
        card.getStyleClass().add("side-card");
        VBox.setVgrow(card, Priority.ALWAYS);
    }

    /** the styled card node to drop into a layout. */
    public VBox node() {
        return card;
    }

    public void add(String text, String type) {
        Label entry = new Label(text);
        entry.setWrapText(true);
        entry.setMaxWidth(200);
        entry.getStyleClass().addAll("log-entry", "log-entry-" + type);
        entries.getChildren().add(0, entry);
        while (entries.getChildren().size() > MAX_ENTRIES) {
            entries.getChildren().remove(entries.getChildren().size() - 1);
        }
    }
}
