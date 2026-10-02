package com.battleship.net;

import com.battleship.model.Player;
import com.battleship.model.Theater;
import com.battleship.model.fog.TrackingGrid;
import com.battleship.model.projection.ShipSnapshot;

import java.util.List;

/**
 * shared mutable state for one "play with a friend" match. passed by reference
 * between the lobby, placement, and battle screens so they all see the same
 * connection, local player, and enemy knowledge.
 *
 * <p>the bespoke {@code enemytracker} is gone (smell 5.2): a network admiral now
 * keeps their observations in the same {@link trackinggrid} the local game and
 * the ai use, so there is one model of reality instead of two. ammunition lives on
 * {@code me} (its {@code arsenal}) — no need to duplicate it here.</p>
 */
public class NetworkGameSession {

    private final NetworkSession session;
    private final Theater theater;
    private final Role role;
    private final Player me;
    private volatile boolean myTurn;
    private long turnNumber;
    private volatile List<ShipSnapshot> revealedEnemyFleet;
    public long getTurnNumber() { return turnNumber; }
    public void advanceTurn() { turnNumber++; }

    public List<ShipSnapshot> getRevealedEnemyFleet() { return revealedEnemyFleet; }
    public void setRevealedEnemyFleet(List<ShipSnapshot> fleet) { this.revealedEnemyFleet = fleet; }

    /** Ignore stale/duplicate timeout notifications and messages sent on our turn. */
    public boolean acceptRemoteTimeout(long number) {
        if (myTurn || number != turnNumber || isGameOver()) return false;
        turnNumber++;
        beginMyTurn();
        return true;
    }

    public NetworkGameSession(NetworkSession session, Theater theater, Role role, Player me) {
        this.session = session;
        this.theater = theater;
        this.role = role;
        this.me = me;
    }

    public NetworkSession getSession() { return session; }
    public Theater getTheater() { return theater; }
    public Role getRole() { return role; }
    public boolean isHost() { return role == Role.HOST; }
    public Player getMe() { return me; }

    /** what this admiral knows about the enemy — never the enemy's real grid. */
    public TrackingGrid getEnemyKnowledge() { return me.trackingGrid(); }

    public boolean isMyTurn() { return myTurn; }

    /** grants the local player the turn (after a start or an answered fire). */
    public void beginMyTurn() { this.myTurn = true; }

    /** hands the turn to the remote opponent (after firing or when start says so). */
    public void beginOpponentTurn() { this.myTurn = false; }

    /** rich domain-action alias for {@link #beginmyturn()}. */
    public void passTurnToMe() { beginMyTurn(); }

    /** rich domain-action alias for {@link #beginopponentturn()}. */
    public void passTurnToOpponent() { beginOpponentTurn(); }

    /** checks whether the local player is currently allowed to fire/act. */
    public boolean canAct() {
        return myTurn && !isGameOver();
    }

    /** returns true if either fleet has been completely destroyed. */
    public boolean isGameOver() {
        return me.isFleetDestroyed() || getEnemyKnowledge().isFleetFullyAccountedFor();
    }

    /** applies the host's start decision: hostmovesfirst determines whose turn it is. */
    public void beginMatch(boolean hostMovesFirst) {
        turnNumber = 0;
        this.myTurn = (isHost() == hostMovesFirst);
    }
}
