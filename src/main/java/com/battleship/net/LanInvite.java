package com.battleship.net;

/** Validated connection details shared by the host and join form. */
public record LanInvite(String address, int port, String code) {
    public LanInvite {
        address = address == null ? "" : address.trim();
        code = code == null ? "" : code.trim();
        String[] octets = address.split("\\.", -1);
        if (octets.length != 4) throw new IllegalArgumentException("Enter the host's IPv4 address, for example 192.168.1.23.");
        for (String octet : octets) {
            if (!octet.matches("[0-9]{1,3}") || Integer.parseInt(octet) > 255)
                throw new IllegalArgumentException("Each part of the IP address must be between 0 and 255.");
        }
        if (port < 1 || port > 65535) throw new IllegalArgumentException("Enter a port between 1 and 65535.");
        if (!code.matches("[0-9]{4}")) throw new IllegalArgumentException("Enter the host's 4-digit join code.");
    }

    public static LanInvite fromFields(String address, String port, String code) {
        try {
            return new LanInvite(address, Integer.parseInt(port.trim()), code);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Enter a port between 1 and 65535.");
        }
    }

    public static LanInvite parse(String text) {
        String cleaned = text == null ? "" : text.trim().replaceAll("\\s+", "");
        if (cleaned.regionMatches(true, 0, "BATTLESHIP:", 0, 11)) cleaned = cleaned.substring(11);
        String[] parts = cleaned.split(":", -1);
        if (parts.length != 3) throw new IllegalArgumentException("Paste the full invite copied from the host's screen.");
        return fromFields(parts[0], parts[1], parts[2]);
    }

    public String encode() { return "BATTLESHIP:" + address + ":" + port + ":" + code; }
}
