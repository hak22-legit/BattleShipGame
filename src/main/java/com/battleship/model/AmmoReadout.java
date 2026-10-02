package com.battleship.model;

import com.battleship.model.weapon.Weapon;

/**
 * read-only view of a combatant's ammunition, handed to ai strategies and the ui
 * so they can *plan* around weapon stock without ever mutating it (fixes v1:
 * the mutable inventory never leaves {@link player}).
 */
public interface AmmoReadout {

    /** current rounds for this weapon (or 0 when the weapon is not stocked). */
    int ammoCount(Weapon weapon);

    /** true when at least one round is available. */
    boolean hasAmmo(Weapon weapon);

    /** true for weapons that never run out. */
    boolean isAmmoInfinite(Weapon weapon);
}
