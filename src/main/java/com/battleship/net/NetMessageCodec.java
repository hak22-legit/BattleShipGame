package com.battleship.net;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * gson wire codec for the sealed {@link netmessage} hierarchy.
 *
 * gson cannot serialize/deserialize a sealed interface polymorphically on its
 * own, so this codec adds a "type" discriminator field on the wire and uses
 * pattern matching to pick the record class when decoding.
 */
public final class NetMessageCodec {

    private static final String TYPE_FIELD = "type";

    private final Gson gson = new Gson();

    public String encode(NetMessage msg) {
        return gson.toJson(envelope(msg));
    }

    public NetMessage decode(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            if (!root.has(TYPE_FIELD)) return null;
            String type = root.get(TYPE_FIELD).getAsString();
            JsonObject payload = root.deepCopy();
            payload.remove(TYPE_FIELD);
            return switch (type) {
                case "HELLO"       -> gson.fromJson((JsonElement) payload, NetMessage.Hello.class);
                case "WELCOME"     -> gson.fromJson((JsonElement) payload, NetMessage.Welcome.class);
                case "REJECT"      -> gson.fromJson((JsonElement) payload, NetMessage.Reject.class);
                case "LOBBY_READY" -> gson.fromJson((JsonElement) payload, NetMessage.LobbyReady.class);
                case "BEGIN_DEPLOYMENT" -> gson.fromJson((JsonElement) payload, NetMessage.BeginDeployment.class);
                case "READY"       -> gson.fromJson((JsonElement) payload, NetMessage.Ready.class);
                case "START"       -> gson.fromJson((JsonElement) payload, NetMessage.Start.class);
                case "TURN_EXPIRED" -> gson.fromJson((JsonElement) payload, NetMessage.TurnExpired.class);
                case "FIRE"        -> gson.fromJson((JsonElement) payload, NetMessage.Fire.class);
                case "FIRE_RESULT" -> gson.fromJson((JsonElement) payload, NetMessage.FireResult.class);
                case "FLEET_REVEAL" -> gson.fromJson((JsonElement) payload, NetMessage.FleetReveal.class);
                default            -> null; // unknown message — ignore, never crash the game
            };
        } catch (Exception malformed) {
            return null;
        }
    }

    private JsonObject envelope(NetMessage msg) {
        String type = switch (msg) {
            case NetMessage.Hello h      -> "HELLO";
            case NetMessage.Welcome w    -> "WELCOME";
            case NetMessage.Reject r     -> "REJECT";
            case NetMessage.LobbyReady r -> "LOBBY_READY";
            case NetMessage.BeginDeployment d -> "BEGIN_DEPLOYMENT";
            case NetMessage.Ready r      -> "READY";
            case NetMessage.Start s      -> "START";
            case NetMessage.TurnExpired t -> "TURN_EXPIRED";
            case NetMessage.Fire f       -> "FIRE";
            case NetMessage.FireResult f -> "FIRE_RESULT";
            case NetMessage.FleetReveal f -> "FLEET_REVEAL";
        };
        JsonObject root = gson.toJsonTree(msg).getAsJsonObject();
        root.addProperty(TYPE_FIELD, type);
        return root;
    }
}
