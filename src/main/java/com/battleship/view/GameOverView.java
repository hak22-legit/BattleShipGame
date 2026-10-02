package com.battleship.view;

import com.battleship.controller.GameController;
import com.battleship.model.CellStatus;
import com.battleship.model.Coordinate;
import com.battleship.model.FleetReadout;
import com.battleship.model.GameMode;
import com.battleship.model.MatchStatistics;
import com.battleship.model.Player;

import com.battleship.model.projection.ShipSnapshot;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

/**
 * reveals both boards fully. sunk ships use rendersunkship()
 * (all cells recolored); surviving ships use the normal ship render.
 * restyled to match the "fleet command" theme: full-bleed sea backdrop
 * (gold-tinted for a win, storm-tinted for a loss), board-card framed
 * reveal boards, and a pill-style stat readout.
 */
public class GameOverView {

    private final ViewNavigator nav;
    private final GameController controller;
    private final Player winner;

    public GameOverView(ViewNavigator nav, GameController controller, Player winner) {
        this.nav = nav;
        this.controller = controller;
        this.winner = winner;
    }

    public StackPane build() {
        boolean isHotseat = controller.getSelectedMode() == GameMode.HOTSEAT;
        boolean playerWon = isHotseat || controller.isFirstPlayer(winner);
        nav.getAudio().playGameOver(playerWon);

        String bannerText;
        String subtitleText;
        if (isHotseat) {
            bannerText = "\uD83C\uDFC6  " + winner.getName().toUpperCase() + " WINS!";
            subtitleText = "\u2693  THE OPPOSING FLEET HAS BEEN SENT TO THE BOTTOM  \u2693";
        } else if (playerWon) {
            bannerText = "\uD83C\uDFC6  VICTORY";
            subtitleText = "\u2693  THE ENEMY FLEET HAS BEEN DESTROYED  \u2693";
        } else {
            bannerText = "\u2620  DEFEAT";
            subtitleText = "\u2693  YOUR FLEET HAS BEEN LOST  \u2693";
        }

        Label banner = new Label(bannerText);
        banner.setFont(Font.font("Arial Black", FontWeight.BOLD, isHotseat ? 44 : 52));
        banner.getStyleClass().add(playerWon ? "app-title" : "defeat-banner");

        Label subtitle = new Label(subtitleText);
        subtitle.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 13));
        subtitle.getStyleClass().add("app-subtitle");

        String p1Label = isHotseat ? controller.getPlayerName(1).toUpperCase() + "'S FLEET" : "YOUR FLEET";
        String p2Label = controller.getPlayerName(2).toUpperCase() + "'S FLEET";
        VBox ownBoard = revealedBoardCard(p1Label, controller.getPlayerFleet(1));
        VBox enemyBoard = revealedBoardCard(p2Label, controller.getPlayerFleet(2));

        HBox boards = new HBox(28, ownBoard, enemyBoard);
        boards.setAlignment(Pos.CENTER);

        HBox stats = buildStats();

        Button reEngage = new Button("\u21bb  RE-ENGAGE");
        reEngage.setPrefWidth(190);
        reEngage.setPrefHeight(46);
        reEngage.getStyleClass().addAll("primary-button", "featured-button");
        reEngage.setOnAction(e -> { nav.getAudio().playClick(); nav.showBoardSelect(); });

        Button returnToPort = new Button("RETURN TO PORT");
        returnToPort.setPrefWidth(190);
        returnToPort.setPrefHeight(46);
        returnToPort.getStyleClass().add("ghost-button");
        returnToPort.setOnAction(e -> { nav.getAudio().playClick(); nav.getAudio().playMenuMusic(); nav.showMainMenu(); });

        HBox buttons = new HBox(16, reEngage, returnToPort);
        buttons.setAlignment(Pos.CENTER);

        VBox titleBlock = new VBox(6, banner, subtitle);
        titleBlock.setAlignment(Pos.CENTER);

        VBox layout = new VBox(22, titleBlock, boards, stats, buttons);
        layout.setAlignment(Pos.CENTER);
        layout.setFillWidth(false);
        layout.setPadding(new Insets(28, 24, 28, 24));

        StackPane root = new StackPane();
        javafx.scene.canvas.Canvas ocean = DecorUtil.animatedOceanScene(root, 0.0);
        root.getChildren().add(ocean);

        // a soft mood wash over the sea: warm gold for a win, cool red for a loss.
        Region mood = new Region();
        mood.getStyleClass().add(playerWon ? "mood-wash-win" : "mood-wash-loss");
        mood.setMouseTransparent(true);
        mood.prefWidthProperty().bind(root.widthProperty());
        mood.prefHeightProperty().bind(root.heightProperty());
        root.getChildren().add(mood);

        root.getChildren().add(layout);

        layout.setOpacity(0.0);
        layout.setTranslateY(16);
        FadeTransition fade = new FadeTransition(Duration.millis(420), layout);
        fade.setToValue(1.0);
        TranslateTransition rise = new TranslateTransition(Duration.millis(420), layout);
        rise.setToY(0);
        fade.play();
        rise.play();

        return root;
    }

    private VBox revealedBoardCard(String label, FleetReadout fleet) {
        Label title = new Label(label);
        title.getStyleClass().add("board-card-title");

        BoardGridPane grid = new BoardGridPane(fleet.size());
        for (ShipSnapshot s : fleet.fleet()) {
            if (s.isSunk()) {
                grid.renderSunkShip(s, false);
            } else {
                grid.renderShip(s);
            }
        }
        for (int r = 0; r < fleet.size(); r++) {
            for (int c = 0; c < fleet.size(); c++) {
                Coordinate coord = new Coordinate(r, c);
                CellStatus status = fleet.cellStatus(coord);
                if (status == CellStatus.MISS) {
                    grid.renderShot(coord, CellStatus.MISS, false);
                } else if (status == CellStatus.HIT) {
                    grid.renderShot(coord, CellStatus.HIT, false);
                }
            }
        }

        VBox card = new VBox(14, title, grid);
        card.getStyleClass().add("board-card");
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(Region.USE_PREF_SIZE);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        return card;
    }

    private HBox buildStats() {
        FleetReadout defenderFleet = controller.isFirstPlayer(winner)
                ? controller.getPlayerFleet(2)
                : controller.getPlayerFleet(1);

        MatchStatistics stats = MatchStatistics.from(defenderFleet);

        HBox row = new HBox(0,
                statPill("SHOTS FIRED", String.valueOf(stats.totalShots())),
                statDivider(),
                statPill("HITS", String.valueOf(stats.hits())),
                statDivider(),
                statPill("ACCURACY", String.format("%.1f%%", stats.accuracy())),
                statDivider(),
                statPill("SHIPS SUNK", String.valueOf(stats.shipsSunk())));
        row.getStyleClass().add("side-card");
        row.setAlignment(Pos.CENTER);
        row.setPadding(new Insets(16, 26, 16, 26));
        row.setMaxWidth(Region.USE_PREF_SIZE);
        return row;
    }


    private VBox statPill(String label, String value) {
        Label v = new Label(value);
        v.getStyleClass().addAll("accent-text", "stat-pill-value");
        Label l = new Label(label);
        l.getStyleClass().add("dim-text");
        VBox box = new VBox(4, v, l);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(0, 22, 0, 22));
        return box;
    }

    private Region statDivider() {
        Region divider = new Region();
        divider.getStyleClass().add("stat-divider");
        divider.setPrefWidth(1);
        divider.setMaxWidth(1);
        divider.setPrefHeight(34);
        return divider;
    }
}
