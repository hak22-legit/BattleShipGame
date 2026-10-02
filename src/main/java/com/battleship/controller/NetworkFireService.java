package com.battleship.controller;

import com.battleship.model.Coordinate;
import com.battleship.model.Orientation;
import com.battleship.model.Player;
import com.battleship.model.weapon.Weapon;
import com.battleship.model.weapon.WeaponCatalog;

/**
 * owns the domain mutations of a network shot (fixes f7 — feature envy in
 * {@code networkbattleview.resolveshot}): ammo consumption, weapon reset
 * and the quiz-gated nuclear resupply. the view keeps only ui and network
 * i/o responsibilities.
 */
public class NetworkFireService {

    /** everything the attacker's client must transmit after a shot. */
    public record NetworkShotOrder(Weapon weapon, Coordinate anchor, Orientation orientation) {}

    /**
     * applies the shot's domain bookkeeping to the shooter (consume ammo,
     * reset the weapon per rule 1) and returns the order to send over the wire.
     * consume is a no-op for the infinite standard shell.
     */
    public NetworkShotOrder fireNetworkShot(Player shooter, Weapon weapon,
                                            Coordinate anchor, Orientation orientation) {
        shooter.consumeAmmo(weapon);
        shooter.resetWeaponAfterShot();
        return new NetworkShotOrder(weapon, anchor, orientation);
    }

    /** resupplies ammunition for the specified weapon. */
    public void resupplyAmmo(Player shooter, Weapon weapon, int amount) {
        shooter.resupplyAmmo(weapon, amount);
    }

    /** tops nuclear ammo back up after a successful quiz resupply. */
    public void resupplyNuclear(Player shooter) {
        resupplyAmmo(shooter, WeaponCatalog.nuclear(), 1);
    }
}

