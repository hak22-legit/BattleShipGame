package com.battleship.net;

import com.battleship.controller.LauncherFireResult;
import com.battleship.controller.ShotResolution;
import com.battleship.controller.ShotResolver;
import com.battleship.model.CellStatus;
import com.battleship.model.Player;
import com.battleship.model.ShotResult;
import com.battleship.model.fog.TrackingGrid;
import com.battleship.model.projection.ShipSnapshot;

import com.battleship.model.weapon.Weapon;
import com.battleship.model.weapon.WeaponCatalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * encapsulates the controller-level logic for a network battle (fixes v7 / srp),
 * keeping the view free from dto construction and network i/o.
 *
 * <p>enemy knowledge is recorded into the local player's {@link trackinggrid}
 * (smell 5.2), and shots are resolved through the shared {@link shotresolution}
 * against the local player's {@link com.battleship.model.shottarget} surface
 * (v1.1).</p>
 */
public class NetworkBattleMediator {

    private final NetworkGameSession session;
    private final Player me;
    /** shot-resolution strategy, injectable for tests (fixes f5). */
    private final ShotResolution shotResolution;

    /** production constructor — uses the standard shot resolver. */
    public NetworkBattleMediator(NetworkGameSession session) {
        this(session, ShotResolver.STANDARD);
    }

    /** testable constructor — inject the shot-resolution strategy (dip, fixes f5). */
    public NetworkBattleMediator(NetworkGameSession session, ShotResolution shotResolution) {
        this.session = session;
        this.me = session.getMe();
        this.shotResolution = shotResolution;
    }

    public record IncomingFireOutcome(
            LauncherFireResult resolution,
            boolean lost,
            boolean anyHit,
            boolean anySunk) {}

    /**
     * resolves an incoming shot against the local fleet, formats and dispatches
     * the fireresult reply message across the network session, and returns the outcome.
     */
    public IncomingFireOutcome resolveAndReply(NetMessage.Fire fire) {
        Optional<Weapon> weapon = WeaponCatalog.byId(fire.weaponId());
        if (weapon.isEmpty()) {
            // unknown weapon id (e.g. a peer running a newer plugin set): reply with
            // an empty, harmless result rather than corrupting the local fleet.
            LauncherFireResult empty = new LauncherFireResult(List.of(), List.of());
            if (session.getSession() != null) {
                session.getSession().send(new NetMessage.FireResult(List.of(), List.of(), me.isFleetDestroyed()));
            }
            return new IncomingFireOutcome(empty, me.isFleetDestroyed(), false, false);
        }

        LauncherFireResult resolution = shotResolution.resolve(
                me, weapon.get(), fire.anchor(), fire.orientation());

        boolean lost = me.isFleetDestroyed();
        boolean anyHit = resolution.results().stream().anyMatch(ShotResult::isHit);
        boolean anySunk = resolution.anySunk();

        List<NetMessage.CellResult> cellResults = new ArrayList<>();
        for (ShotResult r : resolution.results()) {
            cellResults.add(new NetMessage.CellResult(r.coordinate(), r.outcome()));
        }
        List<NetMessage.SunkShipInfo> sunkInfos = new ArrayList<>();
        for (ShipSnapshot sunk : resolution.sunkShips()) {
            sunkInfos.add(new NetMessage.SunkShipInfo(sunk.type(), sunk.cells()));
        }
        if (session.getSession() != null) {
            session.getSession().send(new NetMessage.FireResult(cellResults, sunkInfos, lost));
            if (lost) {
                session.getSession().send(new NetMessage.FleetReveal(me.fleet()));
            }
        }

        return new IncomingFireOutcome(resolution, lost, anyHit, anySunk);
    }

    /**
     * records the defender's report of my own shot in my tracking grid: this is the
     * only way a network admiral ever learns anything (true fog of war).
     */
    public void recordObservedResult(NetMessage.FireResult result) {
        TrackingGrid knowledge = me.trackingGrid();
        for (NetMessage.CellResult cell : result.results()) {
            knowledge.recordShotOutcome(cell.coordinate(), cell.outcome());
        }
        for (NetMessage.SunkShipInfo sunk : result.sunkShips()) {
            knowledge.recordWreck(sunk.shipType(), sunk.cells());
        }
    }
}

