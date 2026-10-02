package com.battleship.view.battle;

import com.battleship.model.Player;
import com.battleship.model.weapon.Weapon;
import com.battleship.model.weapon.WeaponCatalog;
import com.battleship.view.CssClasses;
import com.battleship.view.ImageResources;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

import java.util.function.Consumer;

/**
 * the weapon console: one button per registered {@link weapon}, plus its
 * ammunition readout.
 *
 * <p>extracted from the battle template method (v2.1) and rebuilt on the
 * {@link weaponcatalog} (v3.1): the console iterates whatever the catalog knows,
 * so a plugin weapon shows up automatically. it reads ammunition through the
 * player's read-only delegates and never mutates anything itself.</p>
 */
public final class WeaponConsole {

    private final HBox bar = new HBox(10);

    public WeaponConsole() {
        bar.setAlignment(Pos.CENTER);
    }

    /** the weapon bar node to drop into a layout. */
    public HBox node() {
        return bar;
    }

    /**
     * rebuilds the buttons for the given player.
     *
     * @param player     whose magazine is displayed
     * @param boardsize  battlefield size (weapons may be unavailable on small boards)
     * @param turnallows whether the local admiral may act right now
     * @param onselect   invoked when an enabled button is pressed
     */
    public void refresh(Player player, int boardSize, boolean turnAllows, Consumer<Weapon> onSelect) {
        bar.getChildren().clear();
        for (Weapon weapon : WeaponCatalog.all()) {
            bar.getChildren().add(buildButton(player, boardSize, turnAllows, weapon, onSelect));
        }
    }

    private Button buildButton(Player player, int boardSize, boolean turnAllows,
                               Weapon weapon, Consumer<Weapon> onSelect) {
        boolean available = weapon.availableFor(boardSize);
        int ammo = player.ammoCount(weapon);
        boolean hasAmmo = weapon.hasInfiniteAmmo() || ammo > 0;
        boolean enabled = available && hasAmmo && turnAllows;

        String ammoText = weapon.hasInfiniteAmmo() ? "\u221E" : String.valueOf(ammo);
        Button button = new Button(weapon.displayName() + "  (" + ammoText + ")");
        button.getStyleClass().add(CssClasses.WEAPON_BUTTON);

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
        button.setTooltip(new javafx.scene.control.Tooltip(!weapon.allowsResupply()
                ? ammo + " of 3 nuclear strikes remaining this match. No refills."
                : weapon.hasInfiniteAmmo() ? "Unlimited ammunition" : ammo + " rounds remaining"));
        button.setOnAction(e -> onSelect.accept(weapon));
        return button;
    }

    private String stateClass(Player player, Weapon weapon, boolean enabled) {
        if (!enabled) return CssClasses.WEAPON_DISABLED;
        return player.selectedWeapon() == weapon ? CssClasses.WEAPON_SELECTED : CssClasses.WEAPON_ENABLED;
    }
}
