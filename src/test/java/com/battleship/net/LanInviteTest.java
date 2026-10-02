package com.battleship.net;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LanInviteTest {
    @Test void copiedInvitePreservesLeadingZeroCode() {
        LanInvite original = new LanInvite("192.168.1.23", 55123, "0042");
        assertEquals(original, LanInvite.parse(original.encode()));
        assertEquals(original, LanInvite.parse(" battleship: 192.168.1.23 : 55123 : 0042 "));
        assertEquals(original, LanInvite.parse("192.168.1.23:55123:0042"));
    }
    @Test void rejectsMalformedAddressesPortsAndCodes() {
        for (String invalid : new String[]{"256.1.1.1:1234:1234", "192.168.1:1234:1234",
                "192.168.1.2:0:1234", "192.168.1.2:65536:1234", "192.168.1.2:abc:1234",
                "192.168.1.2:1234:123", "192.168.1.2:1234:abcd", "192.168.1.2:1234:1234:extra"}) {
            assertThrows(IllegalArgumentException.class, () -> LanInvite.parse(invalid), invalid);
        }
    }
}
