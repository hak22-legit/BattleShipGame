package com.battleship.model.weapon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * arsenal registry for canonical weapons: SINGLE (Default), CROSS (Cross Bomb), NUCLEAR (Nuclear).
 * LEVEL_2 is permanently removed.
 * strictly pure domain class with zero JavaFX imports.
 */
public final class WeaponCatalog {

    private static final List<Weapon> REGISTRY = new CopyOnWriteArrayList<>();

    static {
        register(new StandardShell());
        register(new CrossBomb());
        register(new NuclearWarhead());
    }

    private WeaponCatalog() {
    }

    /** registers a weapon (idempotent for an id that is already known). */
    public static void register(Weapon weapon) {
        if (weapon == null) return;
        boolean known = REGISTRY.stream().anyMatch(existing -> existing.id().equalsIgnoreCase(weapon.id()));
        if (!known) REGISTRY.add(weapon);
    }

    /** every known weapon, in registration order. */
    public static List<Weapon> all() {
        return Collections.unmodifiableList(REGISTRY);
    }

    /** looks a weapon up by its stable id (case-insensitive). */
    public static Optional<Weapon> byId(String id) {
        if (id == null) return Optional.empty();
        if (id.equalsIgnoreCase(StandardShell.LEGACY_ID) || id.equalsIgnoreCase(StandardShell.ID)) {
            return REGISTRY.stream().filter(w -> w.id().equalsIgnoreCase(StandardShell.ID)).findFirst();
        }
        return REGISTRY.stream().filter(w -> w.id().equalsIgnoreCase(id)).findFirst();
    }

    /** looks up weapon by WeaponType. */
    public static Weapon byType(WeaponType type) {
        if (type == null) return standard();
        return byId(type.id()).orElse(standard());
    }

    /** canonical default weapon: single 1x1 artillery. */
    public static Weapon standard() {
        return byId(StandardShell.ID).orElseThrow();
    }

    /** canonical cross weapon: 5-cell '+' pattern. */
    public static Weapon crossBomb() {
        return byId(CrossBomb.ID).orElseThrow();
    }

    /** canonical nuclear weapon: 3x3 square block. */
    public static Weapon nuclear() {
        return byId(NuclearWarhead.ID).orElseThrow();
    }

    /** convenience aliases. */
    public static Weapon standardShell() { return standard(); }
    public static Weapon defaultWeapon() { return standard(); }
    public static Weapon cross() { return crossBomb(); }
    public static Weapon nuclearWarhead() { return nuclear(); }

    /** every weapon id, for diagnostics and tests. */
    public static List<String> ids() {
        List<String> ids = new ArrayList<>();
        for (Weapon weapon : REGISTRY) ids.add(weapon.id());
        return ids;
    }
}
