package com.battleship.net;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/** small helpers for discovering this machine's lan address and a free tcp port. */
public final class NetUtil {

    private NetUtil() { }

    /** best-effort guess at this machine's lan ipv4 address (falls back to loopback). */
    public static String getLocalIp() {
        return getLocalIps().get(0);
    }

    /** All active IPv4 addresses, so the host can choose its Wi-Fi/Ethernet LAN. */
    public static List<String> getLocalIps() {
        return getLocalAddresses().stream().map(LanAddress::ip).toList();
    }

    public record LanAddress(String ip, String adapter, boolean virtual) {
        @Override public String toString() { return adapter + " — " + ip + (virtual ? " (virtual / VPN)" : ""); }
    }

    public static List<LanAddress> getLocalAddresses() {
        try {
            List<LanAddress> candidates = new ArrayList<>();
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                NetworkInterface iface = ifaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;
                String name = iface.getDisplayName() == null ? iface.getName() : iface.getDisplayName();
                String lower = name.toLowerCase(java.util.Locale.ROOT);
                boolean virtual = iface.isVirtual() || lower.matches(".*(virtual|vmware|vbox|vpn|hyper-v|wsl|docker|tap-|tun|wireguard|tailscale|loopback).*" );
                Enumeration<InetAddress> addrs = iface.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        candidates.add(new LanAddress(addr.getHostAddress(), name, virtual));
                    }
                }
            }
            candidates.sort(java.util.Comparator.comparingInt((LanAddress a) ->
                    a.ip().startsWith("169.254.") ? 2 : a.virtual() ? 1 : 0));
            return candidates.isEmpty() ? List.of(new LanAddress("127.0.0.1", "This computer only", false)) : List.copyOf(candidates);
        } catch (SocketException e) {
            return List.of(new LanAddress("127.0.0.1", "This computer only", false));
        }
    }

    /** asks the os for an available ephemeral tcp port. */
    public static int findFreePort() {
        try (ServerSocket s = new ServerSocket(0)) {
            return s.getLocalPort();
        } catch (Exception e) {
            return 55055;
        }
    }
}
