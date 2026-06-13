package dev.httxrafa.modflared.mixin;

import dev.httxrafa.modflared.Modflared;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import java.util.Map;

/**
 * FML coremod plugin for Modflared.
 * Mixin bootstrapping is handled by MixinTweaker (declared in the jar manifest as TweakClass).
 * We also bootstrap the TunnelManager here since FML's @Mod scanner may not pick up the mod class
 * when it's packaged as a coremod. None of these init methods require MC to be running.
 */
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.Name("ModflaredMixinLoader")
public class ModflaredMixinLoader implements IFMLLoadingPlugin {

    public ModflaredMixinLoader() {
        // Initialize the tunnel manager early from the coremod so the mod works
        // regardless of whether FML's @Mod scanner discovers the Modflared class.
        try {
            Modflared.TUNNEL_MANAGER.initDirectories();
            Modflared.TUNNEL_MANAGER.loadForcedTunnels();
            Modflared.TUNNEL_MANAGER.prepareBinary();

            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    Modflared.TUNNEL_MANAGER.closeTunnels();
                    Modflared.EXECUTOR.shutdownNow();
                }
            }, "Modflared-Shutdown"));

            Modflared.LOGGER.info("Modflared tunnel manager initialized from coremod.");
        } catch (Exception e) {
            Modflared.LOGGER.error("Failed to initialize Modflared tunnel manager", e);
        }
    }

    @Override public String[] getASMTransformerClass() { return new String[0]; }
    @Override public String getModContainerClass() { return null; }
    @Override public String getSetupClass() { return null; }
    @Override public void injectData(Map<String, Object> data) {}
    @Override public String getAccessTransformerClass() { return null; }
}
