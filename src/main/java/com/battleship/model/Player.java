package com.battleship.model;

import com.battleship.model.fog.TrackingGrid;
import com.battleship.model.projection.ShipSnapshot;
import com.battleship.model.weapon.Weapon;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * a participant in the game — the aggregate root for one admiral.
 *
 * <p>two structural fixes live here:</p>
 * <ul>
 *   <li><strong>v1.1 / smell 5.1</strong> — the primary grid is private and has no
 *       getter. code that wants to deploy hulls, take a shot or read a grid talks
 *       to the narrow {@link fleetdeployment}, {@link shottarget} and
 *       {@link fleetreadout} command surfaces this class implements. the public
 *       {@code getmutableboard()} backdoor is gone for good.</li>
 *   <li><strong>v2.2</strong> — the {@code ishuman} primitive is gone: behaviour
 *       that differs between a person and a machine (who supplies the next shot,
 *       whether results are learned from) is expressed by {@link humanplayer} and
 *       {@link aiplayer} overriding {@link #decideautonomousshot()} and
 *       {@link #observeownshot(shotresult)}.</li>
 * </ul>
 *
 * <p><strong>srp</strong>: identity + fleet + knowledge + arsenal. weapon stocking,
 * arming and aiming were extracted into {@link arsenal}, which this class forwards
 * to so the rest of the game cannot mutate ammunition directly.</p>
 */
public abstract class Player implements FleetReadout, FleetDeployment, ShotTarget, AmmoReadout {

    private final String name;
    /** never exposed — the whole point of the v1.1 fix. */
    private final PrimaryGrid primaryGrid;
    private final TrackingGrid trackingGrid;
    private final Arsenal arsenal;

    /**
     * @param name                  display name
     * @param boardsize             battlefield edge length
     * @param enemyfleetcomposition the opposing roster (published by the rules of the game)
     */
    protected Player(String name, int boardSize, Map<ShipType, Integer> enemyFleetComposition) {
        this.name = Objects.requireNonNull(name, "A player needs a name.");
        this.primaryGrid = new PrimaryGrid(boardSize);
        this.trackingGrid = new TrackingGrid(boardSize, enemyFleetComposition);
        this.arsenal = new Arsenal(boardSize);
    }

    public String name() {
        return name;
    }

    public String getName() {
        return name;
    }

    /**
     * what this player knows about the enemy — safe to expose, because a tracking
     * grid cannot answer questions about unobserved ship positions (v1.3).
     */
    public TrackingGrid trackingGrid() {
        return trackingGrid;
    }

    /**
     * own waters and fleet deployment aggregate (implements fleetreadout,
     * fleetdeployment, and shottarget).
     */
    public PrimaryGrid primaryGrid() {
        return primaryGrid;
    }

    /**
     * ammunition readout component for weapon stock queries.
     */
    public AmmoReadout ammoReadout() {
        return arsenal;
    }

    // ---------- polymorphic turn behaviour (replaces the ishuman flag) ----------

    /** true for machine-controlled players, which act without a ui click. */
    public abstract boolean isAutonomous();

    /** convenience query: true if this player is controlled by a human. */
    public boolean isHuman() {
        return !isAutonomous();
    }

    /**
     * produces this player's next shot on its own, or {@link optional#empty()} when
     * a human must click a cell. {@link aiplayer} overrides this; {@link humanplayer}
     * inherits the empty answer.
     */
    public Optional<ShotOrder> decideAutonomousShot() {
        return Optional.empty();
    }

    /** learning hook: called for the shooter after their own shot was resolved. */
    public void observeOwnShot(ShotResult result) {
        // nothing to learn for a human.
    }

    // ---------- weapon commands (delegated to the arsenal component) ----------

    /** arms a weapon if the battlefield offers it and ammunition remains. */
    public boolean selectWeapon(Weapon weapon) {
        return arsenal.select(weapon);
    }

    /** @deprecated prefer {@link #selectweapon(weapon)} with {@link com.battleship.model.weapon.weaponcatalog#defaultweapon()}. */
    @Deprecated
    public boolean aimDefault() {
        return selectWeapon(com.battleship.model.weapon.WeaponCatalog.defaultWeapon());
    }

    /** @deprecated prefer {@link #selectweapon(weapon)} with {@link com.battleship.model.weapon.weaponcatalog#nuclear()}. */
    @Deprecated
    public boolean aimNuclear() {
        return selectWeapon(com.battleship.model.weapon.WeaponCatalog.nuclear());
    }


    public void toggleWeaponOrientation() {
        arsenal.toggleOrientation();
    }

    /** rule 1: after firing, the admiral must actively re-select a weapon. */
    public void resetWeaponAfterShot() {
        arsenal.resetAfterShot();
    }

    /** arms a weapon + orientation for an automated or networked shot. */
    public void armWeapon(Weapon weapon, Orientation orientation) {
        arsenal.arm(weapon, orientation);
    }

    public Weapon selectedWeapon() {
        return arsenal.selected();
    }

    public Orientation weaponOrientation() {
        return arsenal.orientation();
    }

    public void consumeAmmo(Weapon weapon) {
        arsenal.consume(weapon);
    }

    public void resupplyAmmo(Weapon weapon, int amount) {
        arsenal.resupply(weapon, amount);
    }

    // ---------- ammoreadout ----------

    @Override
    public int ammoCount(Weapon weapon) {
        return arsenal.ammoCount(weapon);
    }

    @Override
    public boolean hasAmmo(Weapon weapon) {
        return arsenal.hasAmmo(weapon);
    }

    @Override
    public boolean isAmmoInfinite(Weapon weapon) {
        return arsenal.isAmmoInfinite(weapon);
    }

    // ---------- fleetreadout ----------

    @Override
    public int size() {
        return primaryGrid.size();
    }

    @Override
    public CellStatus cellStatus(Coordinate c) {
        return primaryGrid.cellStatus(c);
    }

    @Override
    public List<ShipSnapshot> fleet() {
        return primaryGrid.fleet();
    }

    @Override
    public boolean isFleetDestroyed() {
        return primaryGrid.isFleetDestroyed();
    }

    // ---------- fleetdeployment ----------

    @Override
    public boolean canDeploy(ShipType type, Coordinate start, Orientation orientation) {
        return primaryGrid.canDeploy(type, start, orientation);
    }

    @Override
    public boolean deploy(ShipType type, Coordinate start, Orientation orientation) {
        return primaryGrid.deploy(type, start, orientation);
    }

    @Override
    public boolean undeployAt(Coordinate c) {
        return primaryGrid.undeployAt(c);
    }

    @Override
    public boolean canReposition(Coordinate from, Coordinate start, Orientation orientation) {
        return primaryGrid.canReposition(from, start, orientation);
    }

    @Override
    public boolean reposition(Coordinate from, Coordinate start, Orientation orientation) {
        return primaryGrid.reposition(from, start, orientation);
    }

    @Override
    public void clearDeployment() {
        primaryGrid.clearDeployment();
    }

    // ---------- shottarget ----------

    @Override
    public boolean isCellResolved(Coordinate c) {
        return primaryGrid.isCellResolved(c);
    }

    @Override
    public ShotResult receiveShot(Coordinate c) {
        return primaryGrid.receiveShot(c);
    }

    /** hulls currently deployed — the placement counter uses it. */
    public int deployedShipCount() {
        return primaryGrid.deployedShipCount();
    }
}
