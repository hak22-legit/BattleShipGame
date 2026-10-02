package com.battleship.view;

import com.battleship.model.CellStatus;
import com.battleship.model.Coordinate;
import com.battleship.model.Player;
import com.battleship.model.fog.MarkerStatus;
import com.battleship.model.fog.TrackingGrid;
import com.battleship.model.projection.ShipSnapshot;
import com.battleship.net.NetMessage;
import com.battleship.net.NetworkGameSession;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
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

import java.util.List;

/**
 * Game over screen for a network match. Reveals both fleets so players see
 * where ships were placed, with an interactive toggle to switch to the fog-of-war
 * observed grid if desired. Includes match statistics and gold/storm atmospheric backdrop.
 */
public class NetworkGameOverView {

    private final ViewNavigator nav;
    private final NetworkGameSession netSession;
    private final boolean won;

    private boolean showObservedOnly = false;
    private HBox boardsRow;
    private VBox enemyCard;
    private HBox statsRow;
    private VBox layout;

    public NetworkGameOverView(ViewNavigator nav, NetworkGameSession netSession, boolean won) {
        this.nav = nav;
        this.netSession = netSession;
        this.won = won;
    }

    public StackPane build() {
        if (netSession.getSession() != null) {
            netSession.getSession().setOnDisconnected(() -> {
                // Peer disconnection at the game-over screen is expected when leaving.
            });
            netSession.getSession().setOnMessage(this::handleGameOverMessage);
        }
        nav.getAudio().stopBgm();
        nav.getAudio().playGameOver(won);

        Label banner = new Label(won ? "\uD83C\uDFC6  VICTORY" : "\u2620  DEFEAT");
        banner.setFont(Font.font("Arial Black", FontWeight.BOLD, 52));
        banner.getStyleClass().add(won ? "app-title" : "defeat-banner");

        Label subtitle = new Label(won
                ? "\u2693  THE ENEMY FLEET HAS BEEN DESTROYED  \u2693"
                : "\u2693  YOUR FLEET HAS BEEN LOST  \u2693");
        subtitle.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 13));
        subtitle.getStyleClass().add("app-subtitle");

        enemyCard = buildEnemyCard();
        boardsRow = new HBox(28, myBoardCard(), enemyCard);
        boardsRow.setAlignment(Pos.CENTER);

        statsRow = buildStats();

        Button returnToPort = new Button("RETURN TO PORT");
        returnToPort.setPrefWidth(190);
        returnToPort.setPrefHeight(46);
        returnToPort.getStyleClass().addAll("primary-button", "featured-button");
        returnToPort.setOnAction(e -> {
            nav.getAudio().playClick();
            nav.getAudio().playMenuMusic();
            if (netSession.getSession() != null) {
                netSession.getSession().close();
            }
            nav.showMainMenu();
        });

        VBox titleBlock = new VBox(6, banner, subtitle);
        titleBlock.setAlignment(Pos.CENTER);

        layout = new VBox(22, titleBlock, boardsRow, statsRow, returnToPort);
        layout.setAlignment(Pos.CENTER);
        layout.setFillWidth(false);
        layout.setPadding(new Insets(24, 24, 24, 24));

        StackPane root = new StackPane();
        javafx.scene.canvas.Canvas ocean = DecorUtil.animatedOceanScene(root, 0.0);
        root.getChildren().add(ocean);

        Region mood = new Region();
        mood.getStyleClass().add(won ? "mood-wash-win" : "mood-wash-loss");
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

    private void handleGameOverMessage(NetMessage msg) {
        if (msg instanceof NetMessage.FleetReveal reveal) {
            netSession.setRevealedEnemyFleet(reveal.ships());
            Platform.runLater(() -> {
                showObservedOnly = false;
                refreshEnemyCard();
                refreshStats();
            });
        }
    }

    private VBox myBoardCard() {
        Label title = new Label("YOUR FLEET");
        title.getStyleClass().add("board-card-title");

        StackPane header = new StackPane(title);
        header.setAlignment(Pos.CENTER);
        header.setMinHeight(26);

        Player me = netSession.getMe();
        BoardGridPane grid = new BoardGridPane(me.size());
        header.maxWidthProperty().bind(grid.widthProperty());

        for (ShipSnapshot s : me.fleet()) {
            if (s.isSunk()) grid.renderSunkShip(s, false); else grid.renderShip(s);
        }
        for (int r = 0; r < me.size(); r++) {
            for (int c = 0; c < me.size(); c++) {
                Coordinate coord = new Coordinate(r, c);
                if (me.cellStatus(coord) == CellStatus.MISS) grid.renderShot(coord, CellStatus.MISS, false);
                if (me.cellStatus(coord) == CellStatus.HIT) grid.renderShot(coord, CellStatus.HIT, false);
            }
        }

        VBox card = new VBox(14, header, grid);
        card.getStyleClass().add("board-card");
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(Region.USE_PREF_SIZE);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        return card;
    }

    private VBox buildEnemyCard() {
        boolean hasReveal = netSession.getRevealedEnemyFleet() != null;
        boolean showingObserved = showObservedOnly || !hasReveal;

        Label title = new Label(showingObserved ? "ENEMY WATERS (AS OBSERVED)" : "ENEMY FLEET");
        title.getStyleClass().add("board-card-title");

        StackPane header = new StackPane(title);
        header.setAlignment(title, Pos.CENTER);
        header.setMinHeight(26);

        if (hasReveal) {
            Button toggleBtn = new Button(showingObserved ? "\u2693 FLEET" : "\uD83D\uDC41 OBSERVED");
            toggleBtn.getStyleClass().add("ghost-button");
            toggleBtn.setStyle("-fx-font-size: 10px; -fx-padding: 3 9 3 9; -fx-cursor: hand;");
            toggleBtn.setOnAction(e -> {
                nav.getAudio().playClick();
                showObservedOnly = !showObservedOnly;
                refreshEnemyCard();
            });
            StackPane.setAlignment(toggleBtn, Pos.CENTER_RIGHT);
            header.getChildren().add(toggleBtn);
        }

        BoardGridPane grid = showingObserved ? createObservedGrid() : createRevealedEnemyGrid();
        header.maxWidthProperty().bind(grid.widthProperty());

        VBox card = new VBox(14, header, grid);
        card.getStyleClass().add("board-card");
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(Region.USE_PREF_SIZE);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        return card;
    }

    private void refreshEnemyCard() {
        VBox newCard = buildEnemyCard();
        int idx = boardsRow.getChildren().indexOf(enemyCard);
        if (idx >= 0) {
            boardsRow.getChildren().set(idx, newCard);
            enemyCard = newCard;
        }
    }

    private BoardGridPane createRevealedEnemyGrid() {
        TrackingGrid knowledge = netSession.getEnemyKnowledge();
        int size = knowledge.size();
        BoardGridPane grid = new BoardGridPane(size);

        List<ShipSnapshot> enemyFleet = netSession.getRevealedEnemyFleet();
        if (enemyFleet != null) {
            for (ShipSnapshot s : enemyFleet) {
                if (s.isSunk()) {
                    grid.renderSunkShip(s, false);
                } else {
                    grid.renderShip(s);
                }
            }
        }

        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                Coordinate coord = new Coordinate(r, c);
                MarkerStatus status = knowledge.observedStatus(coord);
                if (status == MarkerStatus.MISS) {
                    grid.renderShot(coord, CellStatus.MISS, false);
                } else if (status == MarkerStatus.HIT) {
                    grid.renderShot(coord, CellStatus.HIT, false);
                }
            }
        }
        return grid;
    }

    private BoardGridPane createObservedGrid() {
        TrackingGrid knowledge = netSession.getEnemyKnowledge();
        int size = knowledge.size();
        BoardGridPane grid = new BoardGridPane(size);
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                Coordinate coord = new Coordinate(r, c);
                MarkerStatus status = knowledge.observedStatus(coord);
                if (status == MarkerStatus.HIT) grid.renderShot(coord, CellStatus.HIT, false);
                else if (status == MarkerStatus.MISS) grid.renderShot(coord, CellStatus.MISS, false);
            }
        }
        for (TrackingGrid.DiscoveredWreck wreck : knowledge.confirmedSunk()) {
            grid.renderSunkShip(wreck.type(), wreck.orientation(), wreck.cells(), false);
        }
        return grid;
    }

    private HBox buildStats() {
        TrackingGrid knowledge = netSession.getEnemyKnowledge();
        int size = knowledge.size();
        int hits = 0;
        int misses = 0;
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                MarkerStatus s = knowledge.observedStatus(new Coordinate(r, c));
                if (s == MarkerStatus.HIT || s == MarkerStatus.SUNK) hits++;
                else if (s == MarkerStatus.MISS) misses++;
            }
        }
        int total = hits + misses;
        double accuracy = total == 0 ? 0.0 : (100.0 * hits / total);
        long shipsSunk = netSession.getRevealedEnemyFleet() != null
                ? netSession.getRevealedEnemyFleet().stream().filter(ShipSnapshot::isSunk).count()
                : knowledge.confirmedSunk().size();

        HBox row = new HBox(0,
                statPill("SHOTS FIRED", String.valueOf(total)),
                statDivider(),
                statPill("HITS", String.valueOf(hits)),
                statDivider(),
                statPill("ACCURACY", String.format("%.1f%%", accuracy)),
                statDivider(),
                statPill("SHIPS SUNK", String.valueOf(shipsSunk)));
        row.getStyleClass().add("side-card");
        row.setAlignment(Pos.CENTER);
        row.setPadding(new Insets(16, 26, 16, 26));
        row.setMaxWidth(Region.USE_PREF_SIZE);
        return row;
    }

    private void refreshStats() {
        if (statsRow != null && layout != null) {
            HBox newStats = buildStats();
            int idx = layout.getChildren().indexOf(statsRow);
            if (idx >= 0) {
                layout.getChildren().set(idx, newStats);
                statsRow = newStats;
            }
        }
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
