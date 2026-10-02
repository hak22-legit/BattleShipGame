package com.battleship.model.weapon;

import com.battleship.model.Orientation;

/**
 * a selectable weapon: its identity, its ammunition rules and its blast geometry.
 *
 * <p>replaces the {@code launchertype} enum (v3.1). behavior now lives in
 * ordinary classes implementing this interface, so a plugin may add a
 * {@code torpedo}, {@code sonarsweep} or {@code clusterbomb} by shipping a new
 * implementation and registering it in {@link weaponcatalog} — no core type is
 * recompiled (open/closed principle).</p>
 */
public interface Weapon {

    /** stable identifier used by the wire protocol and for persistence. */
    String id();

    /** label shown on the weapon console. */
    String displayName();

    /** ammunition granted for a battle on a board of the given size. */
    int startingAmmo(int boardSize);

    /** whether this weapon is offered at all on a board of the given size. */
    boolean availableFor(int boardSize);

    /** {@code true} when the weapon never runs out (its stock is not decremented). */
    boolean hasInfiniteAmmo();
    default boolean allowsResupply() { return true; }

    /** the footprint this weapon covers from an anchor, per orientation. */
    BlastPattern blastPattern();

    /** the coordinates covered when this weapon is fired from an anchor. */
    default java.util.List<com.battleship.model.Coordinate> calculateBlastArea(
            com.battleship.model.Coordinate anchor, Orientation orientation) {
        return blastPattern().coverage(anchor, orientation);
    }

    /** whether this weapon requires confirmation/authorization before launching. */
    default boolean requiresAuthorization() {
        return false;
    }

    /** polymorphic audio trigger for firing this weapon. */
    default void playFiringSound(com.battleship.view.SfxAudio audio) {
        audio.playFire();
    }
}
