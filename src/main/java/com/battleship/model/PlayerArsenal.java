package com.battleship.model;

import com.battleship.model.weapon.CrossBomb;
import com.battleship.model.weapon.NuclearWarhead;
import com.battleship.model.weapon.StandardShell;
import com.battleship.model.weapon.Weapon;
import com.battleship.model.weapon.WeaponCatalog;
import com.battleship.model.weapon.WeaponType;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * player weapon inventory managing ammo stock, selected weapon, and firing orientation.
 * strictly pure domain class with zero JavaFX imports.
 *
 * <p>dynamic ammunition matrix strictly bound to board size:
 * <ul>
 *   <li>5x5: Single = ∞, Cross = 0, Nuclear = 0</li>
 *   <li>8x8: Single = ∞, Cross = 2, Nuclear = 0</li>
 *   <li>10x10: Single = ∞, Cross = 3, Nuclear = 1</li>
 * </ul>
 * </p>
 */
public class PlayerArsenal implements AmmoReadout {

    private final int boardSize;
    private final Map<WeaponType, Integer> ammoByType = new EnumMap<>(WeaponType.class);
    private final Map<Weapon, Integer> stock = new LinkedHashMap<>();

    private Weapon selectedWeapon;
    private WeaponType selectedType;
    private Orientation orientation = Orientation.HORIZONTAL;

    public PlayerArsenal(int boardSize) {
        if (boardSize <= 0) {
            throw new IllegalArgumentException("Board size must be positive, got: " + boardSize);
        }
        this.boardSize = boardSize;

        // Dynamic Ammunition Matrix strictly bound to boardSize
        ammoByType.put(WeaponType.SINGLE, Integer.MAX_VALUE);
        if (boardSize >= 10) {
            ammoByType.put(WeaponType.CROSS, 3);
            ammoByType.put(WeaponType.NUCLEAR, 1);
        } else if (boardSize >= 8) {
            ammoByType.put(WeaponType.CROSS, 2);
            ammoByType.put(WeaponType.NUCLEAR, 0);
        } else {
            ammoByType.put(WeaponType.CROSS, 0);
            ammoByType.put(WeaponType.NUCLEAR, 0);
        }

        // Initialize stock map for registered catalog weapons
        for (Weapon weapon : WeaponCatalog.all()) {
            WeaponType type = mapWeaponToType(weapon);
            if (type != null) {
                stock.put(weapon, ammoByType.get(type));
            } else {
                stock.put(weapon, weapon.startingAmmo(boardSize));
            }
        }

        this.selectedType = WeaponType.SINGLE;
        this.selectedWeapon = WeaponCatalog.standard();
    }

    public int boardSize() {
        return boardSize;
    }

    /** allows test suites or custom scenarios to configure ammunition explicitly. */
    public void setAmmo(WeaponType type, int count) {
        if (type == null) return;
        ammoByType.put(type, count);
        Weapon weapon = mapTypeToWeapon(type);
        if (weapon != null) {
            stock.put(weapon, count);
        }
    }

    // ---------- ammunition queries (pure domain) ----------

    public boolean canFire(WeaponType type) {
        if (type == null) return false;
        if (!type.availableFor(boardSize)) return false;
        return hasAmmo(type);
    }

    public boolean canFire(Weapon weapon) {
        if (weapon == null) return false;
        WeaponType type = mapWeaponToType(weapon);
        if (type != null) return canFire(type);
        return weapon.availableFor(boardSize) && hasAmmo(weapon);
    }

    public void consumeAmmo(WeaponType type) {
        if (type == null || type.hasInfiniteAmmo()) return;
        int current = ammoCount(type);
        if (current <= 0) {
            throw new IllegalStateException("No ammunition for " + type.displayName());
        }
        int updated = current - 1;
        ammoByType.put(type, updated);
        Weapon weapon = mapTypeToWeapon(type);
        if (weapon != null) {
            stock.put(weapon, updated);
        }
    }

    public void consumeAmmo(Weapon weapon) {
        if (weapon == null || weapon.hasInfiniteAmmo()) return;
        WeaponType type = mapWeaponToType(weapon);
        if (type != null) {
            consumeAmmo(type);
            return;
        }
        int current = ammoCount(weapon);
        if (current <= 0) {
            throw new IllegalStateException("No ammunition for " + weapon.displayName());
        }
        stock.put(weapon, current - 1);
    }

    public int ammoCount(WeaponType type) {
        if (type == null) return 0;
        Integer count = ammoByType.get(type);
        return count != null ? count : (type.hasInfiniteAmmo() ? Integer.MAX_VALUE : 0);
    }

    @Override
    public int ammoCount(Weapon weapon) {
        if (weapon == null) return 0;
        WeaponType type = mapWeaponToType(weapon);
        if (type != null) return ammoCount(type);
        Integer rounds = stock.get(weapon);
        if (rounds != null) return rounds;
        return weapon.hasInfiniteAmmo() ? Integer.MAX_VALUE : 0;
    }

    public boolean hasAmmo(WeaponType type) {
        if (type == null) return false;
        return type.hasInfiniteAmmo() || ammoCount(type) > 0;
    }

    @Override
    public boolean hasAmmo(Weapon weapon) {
        if (weapon == null) return false;
        return weapon.hasInfiniteAmmo() || ammoCount(weapon) > 0;
    }

    public boolean isAmmoInfinite(WeaponType type) {
        return type != null && type.hasInfiniteAmmo();
    }

    @Override
    public boolean isAmmoInfinite(Weapon weapon) {
        return weapon != null && weapon.hasInfiniteAmmo();
    }

    // ---------- weapon selection & orientation ----------

    public boolean select(WeaponType type) {
        if (!canFire(type)) return false;
        this.selectedType = type;
        this.selectedWeapon = mapTypeToWeapon(type);
        return true;
    }

    public boolean select(Weapon weapon) {
        if (weapon == null) return false;
        WeaponType type = mapWeaponToType(weapon);
        if (type != null) return select(type);
        if (!canFire(weapon)) return false;
        this.selectedWeapon = weapon;
        return true;
    }

    public Weapon selected() {
        return selectedWeapon;
    }

    public WeaponType selectedType() {
        return selectedType;
    }

    public Orientation orientation() {
        return orientation;
    }

    public void toggleOrientation() {
        this.orientation = orientation.toggle();
    }

    public void arm(Weapon weapon, Orientation orientation) {
        if (weapon != null) {
            this.selectedWeapon = weapon;
            this.selectedType = mapWeaponToType(weapon);
        }
        if (orientation != null) {
            this.orientation = orientation;
        }
    }

    public void arm(WeaponType type, Orientation orientation) {
        if (type != null) {
            this.selectedType = type;
            this.selectedWeapon = mapTypeToWeapon(type);
        }
        if (orientation != null) {
            this.orientation = orientation;
        }
    }

    /** resets weapon to canonical SINGLE (Default) artillery after every shot. */
    public void resetAfterShot() {
        this.selectedType = WeaponType.SINGLE;
        this.selectedWeapon = WeaponCatalog.standard();
    }

    public void resupply(WeaponType type, int amount) {
        if (type == null || type.hasInfiniteAmmo() || amount <= 0) return;
        int updated = ammoCount(type) + amount;
        ammoByType.put(type, updated);
        Weapon weapon = mapTypeToWeapon(type);
        if (weapon != null) {
            stock.put(weapon, updated);
        }
    }

    public void resupply(Weapon weapon, int amount) {
        if (weapon == null || weapon.hasInfiniteAmmo() || amount <= 0) return;
        WeaponType type = mapWeaponToType(weapon);
        if (type != null) {
            if (!weapon.allowsResupply()) return;
            resupply(type, amount);
        } else if (weapon.allowsResupply()) {
            stock.put(weapon, ammoCount(weapon) + amount);
        }
    }

    // ---------- mappings ----------

    private WeaponType mapWeaponToType(Weapon weapon) {
        if (weapon == null) return null;
        if (weapon instanceof StandardShell || weapon.id().equalsIgnoreCase(StandardShell.ID)
                || weapon.id().equalsIgnoreCase(StandardShell.LEGACY_ID)) {
            return WeaponType.SINGLE;
        }
        if (weapon instanceof CrossBomb || weapon.id().equalsIgnoreCase(CrossBomb.ID)) {
            return WeaponType.CROSS;
        }
        if (weapon instanceof NuclearWarhead || weapon.id().equalsIgnoreCase(NuclearWarhead.ID)) {
            return WeaponType.NUCLEAR;
        }
        return WeaponType.fromId(weapon.id());
    }

    private Weapon mapTypeToWeapon(WeaponType type) {
        if (type == null) return WeaponCatalog.standard();
        return switch (type) {
            case SINGLE -> WeaponCatalog.standard();
            case CROSS -> WeaponCatalog.crossBomb();
            case NUCLEAR -> WeaponCatalog.nuclear();
        };
    }
}
