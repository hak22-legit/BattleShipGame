package com.battleship.net;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LobbyMessageTest {
    @Test void lobbyMessagesRoundTripSeparatelyFromFleetReadiness() {
        NetMessageCodec codec = new NetMessageCodec();
        for (NetMessage message : new NetMessage[]{new NetMessage.LobbyReady(),
                new NetMessage.BeginDeployment(), new NetMessage.Ready()}) {
            assertEquals(message, codec.decode(codec.encode(message)));
        }
        assertNotEquals(codec.encode(new NetMessage.Ready()), codec.encode(new NetMessage.LobbyReady()));
    }
}
