package com.battleship.view;

import com.battleship.model.Orientation;
import com.battleship.model.ShipType;
import com.battleship.model.weapon.CrossBomb;
import com.battleship.model.weapon.NuclearWarhead;
import com.battleship.model.weapon.StandardShell;
import com.battleship.model.weapon.Weapon;
import javafx.scene.image.Image;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * central place to load and cache the game's bundled art (ship hulls, weapon
 * icons, hit/miss effects, menu decorations). every lookup is classpath-based
 * ("/images/..."), cached after first use, and returns {@code null} instead of
 * throwing if an asset is missing so callers can gracefully fall back to the
 * plain color rendering that predates this class.
 */
public final class ImageResources {

    private ImageResources() { }

    private static final ConcurrentMap<String, Image> CACHE = new ConcurrentHashMap<>();

    private static Image load(String classpathPath) {
        return CACHE.computeIfAbsent(classpathPath, path -> {
            try (InputStream in = ImageResources.class.getResourceAsStream(path)) {
                return in == null ? null : new Image(in);
            } catch (Exception e) {
                return null;
            }
        });
    }

    /**
     * hull art for a ship type/orientation. patrol_boat has no bespoke art
     * (it's a 2-length ship, like destroyer) so it borrows the destroyer sprite.
     */
    public static Image ship(ShipType type, Orientation orientation) {
        String name = type.getAssetName();
        return load("/images/ships/" + name + "-"
                + (orientation.isHorizontal() ? "h" : "v") + ".png");
    }


    /**
     * icon for a weapon, keyed by its stable id. weapons have no bundled art by
     * default (a plugin weapon simply gets no icon), so the mapping is data, not
     * behavior — adding a weapon to the game never requires touching this class.
     */
    private static final Map<String, String> WEAPON_ICONS = Map.of(
            StandardShell.ID, "launcher-default",
            StandardShell.LEGACY_ID, "launcher-default",
            CrossBomb.ID, "launcher-cross",
            NuclearWarhead.ID, "launcher-nuclear"
    );

    public static Image weaponIcon(Weapon weapon) {
        String name = WEAPON_ICONS.get(weapon.id());
        return name == null ? null : load("/images/ui/" + name + ".png");
    }

    /** hit/miss/fire effect art. pass just the base name, e.g. "hit-explosion", "miss-splash", "fire-1". */
    public static Image effect(String baseName) {
        return load("/images/effects/" + baseName + ".png");
    }

    /** menu/chrome art. pass just the base name, e.g. "logo-battleship", "wave-line", "compass-rose", "icon-anchor". */
    public static Image ui(String baseName) {
        return load("/images/ui/" + baseName + ".png");
    }

    /** background art. pass base name (e.g. "menu-background"). checks .jpg then .png. */
    public static Image background(String baseName) {
        Image img = load("/images/backgrounds/" + baseName + ".jpg");
        if (img != null) {
            return img;
        }
        return load("/images/backgrounds/" + baseName + ".png");
    }
}
