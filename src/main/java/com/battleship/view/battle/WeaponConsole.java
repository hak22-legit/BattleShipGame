package com.battleship.view.battle;

import com.battleship.model.Player;
import com.battleship.model.weapon.Weapon;
import com.battleship.model.weapon.WeaponCatalog;
import com.battleship.view.CssClasses;
import com.battleship.view.ImageResources;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

import java.util.function.Consumer;

/**
 * The tactical weapon console: one button per registered {@link Weapon}, plus its
 * ammunition readout, board size visibility rules, and ammo depletion handling.
 */
public final class WeaponConsole {

    private final HBox bar = new HBox(10);

    public WeaponConsole() {
        bar.setAlignment(Pos.CENTER);
    }

    /** The weapon bar node to drop into a layout. */
    public HBox node() {
        return bar;
    }

    /**
     * Rebuilds the buttons for the given player.
     *
     * @param player      whose magazine is displayed
     * @param boardSize   battlefield size (weapons may be unavailable on small boards)
     * @param turnAllows  whether the local admiral may act right now
     * @param onSelect    invoked when an enabled button is pressed
     */
    public void refresh(Player player, int boardSize, boolean turnAllows, Consumer<Weapon> onSelect) {
        if (player == null) return;

        // Auto-revert selection to SINGLE (Default) if current weapon is empty or unavailable on this board
        Weapon current = player.selectedWeapon();
        if (current != null) {
            boolean available = current.availableFor(boardSize);
            boolean hasAmmo = current.hasInfiniteAmmo() || player.ammoCount(current) > 0;
            if (!available || !hasAmmo) {
                player.selectWeapon(WeaponCatalog.defaultWeapon());
            }
        }

        bar.getChildren().clear();
        for (Weapon weapon : WeaponCatalog.all()) {
            bar.getChildren().add(buildButton(player, boardSize, turnAllows, weapon, onSelect));
        }
    }

    /**
     * Finds the button corresponding to a given weapon for testing and inspection.
     */
    public Button getButton(Weapon weapon) {
        if (weapon == null) return null;
        String targetId = "weapon-btn-" + weapon.id().toLowerCase();
        for (javafx.scene.Node node : bar.getChildren()) {
            if (node instanceof Button btn && targetId.equals(btn.getId())) {
                return btn;
            }
        }
        return null;
    }

    private Button buildButton(Player player, int boardSize, boolean turnAllows,
                               Weapon weapon, Consumer<Weapon> onSelect) {
        boolean available = weapon.availableFor(boardSize);
        int ammo = player.ammoCount(weapon);
        boolean hasAmmo = weapon.hasInfiniteAmmo() || ammo > 0;
        boolean enabled = available && hasAmmo && turnAllows;

        String ammoText = weapon.hasInfiniteAmmo() ? "\u221E" : String.valueOf(ammo);
        Button button = new Button(weapon.displayName() + "  (" + ammoText + ")");
        button.setId("weapon-btn-" + weapon.id().toLowerCase());
        button.getStyleClass().add(CssClasses.WEAPON_BUTTON);

        // Visibility & managed bindings based on board size:
        // 5x5: Cross and Nuclear buttons hidden and unmanaged
        // 8x8: Nuclear button hidden and unmanaged, Cross visible (2)
        // 10x10: Cross visible (3), Nuclear visible (1)
        if (!available) {
            button.setVisible(false);
            button.setManaged(false);
        } else {
            button.setVisible(true);
            button.setManaged(true);
        }

        Image icon = ImageResources.weaponIcon(weapon);
        if (icon != null) {
            ImageView iv = new ImageView(icon);
            iv.setFitWidth(20);
            iv.setFitHeight(20);
            iv.setPreserveRatio(true);
            button.setGraphic(iv);
        }

        button.getStyleClass().add(stateClass(player, weapon, enabled));
        button.setDisable(!enabled);

        String tooltipText;
        if (!weapon.allowsResupply()) {
            tooltipText = ammo + " of 1 nuclear strike remaining this match (10x10 only). No refills.";
        } else if (weapon.hasInfiniteAmmo()) {
            tooltipText = "Unlimited ammunition";
        } else {
            tooltipText = ammo + " rounds remaining";
        }
        button.setTooltip(new Tooltip(tooltipText));

        button.setOnAction(e -> {
            if (enabled) {
                onSelect.accept(weapon);
            }
        });
        return button;
    }

    private String stateClass(Player player, Weapon weapon, boolean enabled) {
        if (!enabled) return CssClasses.WEAPON_DISABLED;
        return player.selectedWeapon() == weapon ? CssClasses.WEAPON_SELECTED : CssClasses.WEAPON_ENABLED;
    }
}
