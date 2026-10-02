package com.battleship.net;

import com.battleship.model.CellStatus;
import com.battleship.model.Coordinate;
import com.battleship.model.Orientation;
import com.battleship.model.ShipType;
import com.battleship.model.projection.ShipSnapshot;

import java.util.List;

/**
 * type-safe wire protocol for host&lt;-&gt;client messages: each variant is its
 * own record carrying only the fields it needs (sealed hierarchy — the compiler
 * enforces exhaustiveness; stringly-typed dispatch like `case "banana"` is
 * impossible).
 *
 * <p>wire format: one json object per line via {@link netmessagecodec}, which adds
 * a "type" discriminator field for gson transport.</p>
 *
 * <p>{@link fire} carries a <em>weapon id</em> (a stable string owned by the weapon
 * strategy) rather than an enum constant, so a plugin weapon can travel the wire
 * without a new release of the protocol class (v3.1).</p>
 *
 * message flow:
 *   hello        client -> host       first message after tcp connect (join code)
 *   welcome      host -> client       accepted; battlefield to use
 *   reject       host -> client       bad code / host busy; connection will close
 *   ready        either direction     sender has finished ship placement
 *   start        host -> client       "host" or "client" goes first
 *   fire         attacker -> defender weapon id, anchor cell and orientation
 *   fire_result  defender -> attacker resolved cells, sunk ships, lost flag
 */
public sealed interface NetMessage {

    record Hello(String code) implements NetMessage { }

    record Welcome(String theater) implements NetMessage { }

    record Reject(String reason) implements NetMessage { }

    /** Lobby readiness is separate from completing fleet deployment. */
    record LobbyReady() implements NetMessage { }

    /** Only the host starts deployment, after both lobby confirmations. */
    record BeginDeployment() implements NetMessage { }

    record Ready() implements NetMessage { }

    record Start(String firstPlayer) implements NetMessage { }

    record TurnExpired(long turnNumber) implements NetMessage { }

    record Fire(String weaponId, Coordinate anchor, Orientation orientation) implements NetMessage { }

    record FireResult(List<CellResult> results, List<SunkShipInfo> sunkShips, boolean defenderLost)
            implements NetMessage { }

    /** Full fleet reveal sent upon game completion so both players know where ships were placed. */
    record FleetReveal(List<ShipSnapshot> ships) implements NetMessage { }

    /** one resolved cell from a fire_result. */
    record CellResult(Coordinate coordinate, CellStatus outcome) { }

    /** a ship that was sunk by a fire_result, with every cell it occupied. */
    record SunkShipInfo(ShipType shipType, List<Coordinate> cells) { }
}
