package com.battleship.model;

import com.battleship.model.weapon.Weapon;

/**
 * a complete firing order: which weapon, where, and in which orientation.
 *
 * <p>used by the ai pipeline ("choose, then fire") and by the network layer when
 * a shot travels over the wire. replaces the duplicated {@code aishotplan} record.</p>
 */
public record ShotOrder(Weapon weapon, Coordinate anchor, Orientation orientation) {
}
