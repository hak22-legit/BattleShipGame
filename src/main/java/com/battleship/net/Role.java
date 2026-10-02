package com.battleship.net;

/**
 * which side of a network match this session belongs to — replaces the raw
 * {@code boolean ishost} flag (fixes p3, primitive obsession). an enum makes
 * the two roles explicit and extensible (e.g. a future spectator role).
 */
public enum Role {
    /** created the match and owns the listening socket. */
    HOST,
    /** connected to an existing match via an invite code. */
    CLIENT
}
