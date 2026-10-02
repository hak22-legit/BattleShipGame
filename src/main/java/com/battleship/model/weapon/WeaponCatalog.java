package com.battleship.model.weapon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * the arsenal registry: which weapons exist and how their stable ids map to the
 * strategy objects that implement them.
 *
 * <p>the catalog is the single extension point of the weapon system
 * (open/closed principle). core code asks the catalog for {@link #all()} or
 * looks a weapon up by id; a new weapon is added by {@link #register(weapon)}ing
 * a new {@link weapon} implementation — no existing class changes.</p>
 */
public final class WeaponCatalog {

    private static final List<Weapon> REGISTRY = new CopyOnWriteArrayList<>();

    static {
        register(new StandardShell());
        register(new SalvoBarrage());
        register(new NuclearWarhead());
    }

    private WeaponCatalog() {
    }

    /** registers a weapon (idempotent for an id that is already known). */
    public static void register(Weapon weapon) {
        if (weapon == null) return;
        boolean known = REGISTRY.stream().anyMatch(existing -> existing.id().equals(weapon.id()));
        if (!known) REGISTRY.add(weapon);
    }

    /** every known weapon, in registration order — this drives the weapon console. */
    public static List<Weapon> all() {
        return Collections.unmodifiableList(REGISTRY);
    }

    /** looks a weapon up by its stable id, e.g. when decoding a wire message. */
    public static Optional<Weapon> byId(String id) {
        if (id == null) return Optional.empty();
        return REGISTRY.stream().filter(w -> w.id().equals(id)).findFirst();
    }

    /** the always-available fallback weapon. */
    public static Weapon standard() {
        return byId(StandardShell.ID).orElseThrow();
    }

    /** the single-round area weapon that the quiz resupply tops back up. */
    public static Weapon nuclear() {
        return byId(NuclearWarhead.ID).orElseThrow();
    }

    /** the limited-ammo line weapon. */
    public static Weapon salvo() {
        return byId(SalvoBarrage.ID).orElseThrow();
    }

    /** aliases for fluent and backward-compatible naming. */
    public static Weapon standardShell() { return standard(); }
    public static Weapon salvoBarrage() { return salvo(); }
    public static Weapon nuclearWarhead() { return nuclear(); }
    public static Weapon defaultWeapon() { return standard(); }

    /** every weapon id, for diagnostics and tests. */
    public static List<String> ids() {
        List<String> ids = new ArrayList<>();
        for (Weapon weapon : REGISTRY) ids.add(weapon.id());
        return ids;
    }
}
