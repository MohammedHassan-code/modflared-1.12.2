package dev.httxrafa.modflared.tunnel.manager;

import com.google.gson.reflect.TypeToken;
import dev.httxrafa.modflared.Modflared;
import dev.httxrafa.modflared.binary.Cloudflared;
import dev.httxrafa.modflared.interfaces.mixin.IConnection;
import dev.httxrafa.modflared.tunnel.RunningTunnel;
import dev.httxrafa.modflared.tunnel.TunnelStatus;
import net.minecraft.network.NetworkManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

/**
 * Manages the lifecycle of Cloudflare tunnels for Minecraft connections.
 * This is done by checking if the server has a TXT record with the value
 * "cloudflared-use-tunnel" or "cloudflared-route=<route>"
 *
 * @see <a href="https://stackoverflow.com/a/57612280">How do you tell whether a string is an IP or a hostname</a>
 */
public class TunnelManager {

    public static final Logger CLOUDFLARE_LOGGER = LogManager.getLogger("Cloudflared");

    private static final String DATA_FOLDER_NAME = "modflared";
    private static final String FORCED_TUNNELS_FILE_NAME = "forced_tunnels.json";

    public static final File DATA_FOLDER = new File(DATA_FOLDER_NAME);
    private static final File FORCED_TUNNELS_FILE = new File(DATA_FOLDER, FORCED_TUNNELS_FILE_NAME);

    // Regex to detect raw IP addresses (IPv4 or IPv6) - if it's an IP, we skip DNS lookup
    private static final Pattern IP_PATTERN = Pattern.compile(
            "^(([0-9]{1,3}\\.){3}[0-9]{1,3})" +                                 // IPv4
            "|" +
            "(([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4})" +                        // IPv6 full
            "|" +
            "((([0-9a-fA-F]{1,4}:)*[0-9a-fA-F]{1,4})?::([0-9a-fA-F]{1,4}:)*[0-9a-fA-F]{1,4})" + // IPv6 compressed
            "$"
    );

    private volatile Cloudflared cloudflared;
    private volatile boolean setupError = false;

    private final List<String> forcedTunnels = new ArrayList<String>();
    private final List<RunningTunnel> runningTunnels = new ArrayList<RunningTunnel>();

    public void initDirectories() {
        if (!DATA_FOLDER.exists()) {
            DATA_FOLDER.mkdirs();
        }
    }

    public void prepareBinary() {
        Modflared.EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    cloudflared = Cloudflared.create().get();
                    cloudflared.prepare().get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    Modflared.LOGGER.error("Interrupted while preparing cloudflared", e);
                    setupError = true;
                } catch (ExecutionException e) {
                    Modflared.LOGGER.error("Failed to prepare cloudflared", e);
                    setupError = true;
                }
            }
        });
    }

    public void loadForcedTunnels() {
        if (!FORCED_TUNNELS_FILE.exists()) {
            // Write default empty config
            try {
                Files.write(FORCED_TUNNELS_FILE.toPath(),
                        Modflared.GSON.toJson(new ArrayList<String>()).getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                Modflared.LOGGER.error("Failed to write default forced_tunnels.json", e);
            }
            return;
        }
        try {
            List<String> loaded = Modflared.GSON.fromJson(
                    new InputStreamReader(new FileInputStream(FORCED_TUNNELS_FILE)),
                    new TypeToken<List<String>>() {}.getType()
            );
            if (loaded != null) {
                forcedTunnels.addAll(loaded);
                Modflared.LOGGER.info("Loaded " + forcedTunnels.size() + " forced tunnel(s) from " + FORCED_TUNNELS_FILE_NAME);
            }
        } catch (Exception e) {
            Modflared.LOGGER.error("Failed to load forced_tunnels.json", e);
        }
    }

    /**
     * Called when the client is about to connect to a server.
     * Determines whether a Cloudflare tunnel should be used.
     */
    public TunnelStatus handleConnect(InetSocketAddress address) {
        if (setupError || cloudflared == null) {
            return new TunnelStatus(null, TunnelStatus.State.FAILED_TO_DETERMINE);
        }

        String host = address.getHostString();

        // Don't tunnel raw IP addresses
        if (isIpAddress(host)) {
            return new TunnelStatus(null, TunnelStatus.State.DONT_USE);
        }

        // Check forced tunnels list first
        for (String forced : forcedTunnels) {
            if (forced.equalsIgnoreCase(host)) {
                Modflared.LOGGER.info("Server " + host + " is in forced tunnels list - routing via cloudflared");
                return startTunnel(host);
            }
        }

        // Check DNS TXT records
        try {
            String route = checkDnsTxtRecord(host);
            if (route != null) {
                Modflared.LOGGER.info("Server " + host + " has cloudflared DNS record - routing via " + route);
                return startTunnel(route);
            }
        } catch (Exception e) {
            Modflared.LOGGER.warn("Failed to check DNS TXT record for " + host + ": " + e.getMessage());
            return new TunnelStatus(null, TunnelStatus.State.FAILED_TO_DETERMINE);
        }

        return new TunnelStatus(null, TunnelStatus.State.DONT_USE);
    }

    private TunnelStatus startTunnel(String host) {
        RunningTunnel.Access access = RunningTunnel.Access.localWithRandomPort(host);
        RunningTunnel tunnel = cloudflared.createTunnel(access);
        if (tunnel == null) {
            return new TunnelStatus(null, TunnelStatus.State.FAILED_TO_DETERMINE);
        }
        synchronized (runningTunnels) {
            runningTunnels.add(tunnel);
        }
        return new TunnelStatus(tunnel, TunnelStatus.State.USE);
    }

    /**
     * Checks DNS TXT records for cloudflared configuration.
     * Returns the tunnel hostname to use, or null if not configured.
     */
    private String checkDnsTxtRecord(String host) throws Exception {
        // Use Java's built-in DNS resolution via JNDI to look up TXT records
        javax.naming.directory.InitialDirContext ctx = null;
        try {
            java.util.Hashtable<String, String> env = new java.util.Hashtable<String, String>();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            env.put("java.naming.provider.url", "dns:");
            ctx = new javax.naming.directory.InitialDirContext(env);
            javax.naming.directory.Attributes attrs = ctx.getAttributes(host, new String[]{"TXT"});
            javax.naming.directory.Attribute txtRecords = attrs.get("TXT");
            if (txtRecords == null) return null;

            javax.naming.NamingEnumeration<?> values = txtRecords.getAll();
            while (values.hasMore()) {
                String record = values.next().toString();
                if (record.equals("cloudflared-use-tunnel")) {
                    return host;
                }
                if (record.startsWith("cloudflared-route=")) {
                    return record.substring("cloudflared-route=".length()).trim();
                }
            }
        } finally {
            if (ctx != null) {
                try { ctx.close(); } catch (Exception ignored) {}
            }
        }
        return null;
    }

    /**
     * Associates the running tunnel with the network connection so we can clean it up later.
     */
    public void prepareConnection(TunnelStatus status, NetworkManager manager) {
        if (status.getState() == TunnelStatus.State.USE && status.getRunningTunnel() != null) {
            ((IConnection) manager).setRunningTunnel(status.getRunningTunnel());
        }
    }

    public void closeTunnel(RunningTunnel tunnel) {
        tunnel.closeTunnel();
        synchronized (runningTunnels) {
            runningTunnels.remove(tunnel);
        }
        Modflared.LOGGER.info("Closed cloudflared tunnel for " + tunnel.getAccess().getHostname());
    }

    public void closeTunnels() {
        synchronized (runningTunnels) {
            for (RunningTunnel tunnel : runningTunnels) {
                tunnel.closeTunnel();
            }
            runningTunnels.clear();
        }
        Modflared.LOGGER.info("All cloudflared tunnels closed");
    }

    public static void logSetupError() {
        Modflared.LOGGER.error("Cloudflared setup failed. Tunneling will be unavailable.");
    }

    private static boolean isIpAddress(String host) {
        return IP_PATTERN.matcher(host).matches();
    }
}
