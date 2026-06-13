package dev.httxrafa.modflared.mixin.client;

import dev.httxrafa.modflared.Modflared;
import dev.httxrafa.modflared.interfaces.mixin.IConnectScreen;
import dev.httxrafa.modflared.tunnel.TunnelStatus;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.network.NetworkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

@Mixin(targets = "net.minecraft.client.multiplayer.GuiConnecting$1")
public class GuiConnectingThreadMixin {

    // Synthetic field - anonymous inner class reference to its enclosing GuiConnecting.
    // This name is compiler-generated and never obfuscated.
    @Shadow(remap = false)
    private GuiConnecting this$0;

    @Redirect(
            method = "run",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/NetworkManager;createNetworkManagerAndConnect(Ljava/net/InetAddress;IZ)Lnet/minecraft/network/NetworkManager;"
            )
    )
    private NetworkManager modflared$routeDirectConnect(InetAddress address, int port, boolean useNativeTransport) throws UnknownHostException {
        InetSocketAddress original = new InetSocketAddress(address, port);
        TunnelStatus status = Modflared.TUNNEL_MANAGER.handleConnect(original);

        if (this$0 != null) {
            ((IConnectScreen) this$0).setStatus(status);
        }

        InetSocketAddress target;
        if (status.getState() == TunnelStatus.State.USE && status.getRunningTunnel() != null) {
            target = status.getRunningTunnel().getAccess().getTunnelAddress();
        } else {
            target = original;
        }

        // MCP name: createNetworkManagerAndConnect — reobfJar translates to func_181124_a
        NetworkManager manager = NetworkManager.createNetworkManagerAndConnect(
                InetAddress.getByName(target.getHostString()),
                target.getPort(),
                useNativeTransport
        );
        Modflared.TUNNEL_MANAGER.prepareConnection(status, manager);
        return manager;
    }
}
