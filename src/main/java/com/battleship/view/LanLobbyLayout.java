package com.battleship.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Shared, scroll-safe framing for LAN setup. */
final class LanLobbyLayout {
    private LanLobbyLayout() { }
    static Label text(String text, String style) {
        Label label = new Label(text);
        label.getStyleClass().add(style);
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }
    static VBox field(String title, Node control) {
        Label label = text(title, "lan-field-label");
        label.setLabelFor(control);
        return new VBox(7, label, control);
    }
    static VBox card(Node... children) {
        VBox card = new VBox(14, children);
        card.getStyleClass().add("lan-card");
        return card;
    }
    static StackPane page(String title, String subtitle, Node content, Node back) {
        VBox page = new VBox(12, text("MULTIPLAYER  /  LOCAL NETWORK", "lan-eyebrow"),
                text(title, "lan-title"), text(subtitle, "lan-description"), content, back);
        page.setMaxWidth(900);
        page.setPadding(new Insets(26));
        VBox wrapper = new VBox(page);
        wrapper.setAlignment(Pos.CENTER);
        ScrollPane scroll = new ScrollPane(wrapper);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("lan-scroll");
        scroll.viewportBoundsProperty().addListener((obs, old, bounds) -> wrapper.setMinHeight(bounds.getHeight()));
        return new StackPane(scroll);
    }
}
