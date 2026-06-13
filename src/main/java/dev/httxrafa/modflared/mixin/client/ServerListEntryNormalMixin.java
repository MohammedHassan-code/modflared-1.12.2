package dev.httxrafa.modflared.mixin.client;

import dev.httxrafa.modflared.Modflared;
import dev.httxrafa.modflared.interfaces.mixin.IServerData;
import dev.httxrafa.modflared.tunnel.TunnelStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.ServerListEntryNormal;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;

@Mixin(ServerListEntryNormal.class)
public abstract class ServerListEntryNormalMixin {

    @Shadow(remap = false) @Final private GuiMultiplayer field_148303_c; // MCP: owner
    @Shadow(remap = false) @Final private ServerData     field_148301_e; // MCP: server

    @Unique private static final ResourceLocation MODFLARED_INDICATOR_LOC =
            new ResourceLocation(Modflared.MOD_ID, "textures/gui/sprites/icon/indicator.png");

    @Unique private static final int MODFLARED_INDICATOR_SIZE         = 10;
    @Unique private static final int MODFLARED_INDICATOR_RIGHT_OFFSET = 28;
    @Unique private static final String MODFLARED_TOOLTIP_KEY         = "gui.multiplayer.tunnel.status.0";
    @Unique private static final String MODFLARED_TOOLTIP_FALLBACK    = "Modflared in use";

    /** Whether we've already attempted to load the texture. */
    @Unique private static boolean modflared$textureReady = false;

    /**
     * Load the indicator PNG directly from our JAR's classpath using DynamicTexture,
     * bypassing the Forge resource pack system (which requires a registered @Mod container).
     * Called lazily on the first render tick after Minecraft is initialized.
     */
    @Unique
    private static void modflared$ensureTexture() {
        if (modflared$textureReady) return;
        modflared$textureReady = true; // set early so we never retry on failure

        try {
            InputStream stream = ServerListEntryNormalMixin.class.getResourceAsStream(
                    "/assets/modflared/textures/gui/sprites/icon/indicator.png"
            );
            if (stream == null) {
                Modflared.LOGGER.warn("Modflared: indicator.png not found in classpath");
                return;
            }
            BufferedImage image = ImageIO.read(stream);
            stream.close();
            if (image == null) {
                Modflared.LOGGER.warn("Modflared: failed to decode indicator.png");
                return;
            }
            // Register directly with the texture manager — no resource pack needed
            Minecraft.getMinecraft().getTextureManager().loadTexture(
                    MODFLARED_INDICATOR_LOC, new DynamicTexture(image)
            );
            Modflared.LOGGER.info("Modflared: indicator texture loaded (" + image.getWidth() + "x" + image.getHeight() + ")");
        } catch (Exception e) {
            Modflared.LOGGER.error("Modflared: could not load indicator texture", e);
        }
    }

    @Inject(method = "drawEntry", at = @At("TAIL"))
    private void modflared$drawTunnelIndicator(int slotIndex, int x, int y, int listWidth,
            int slotHeight, int mouseX, int mouseY, boolean isSelected, float partialTicks,
            CallbackInfo ci) {

        TunnelStatus status = ((IServerData) this.field_148301_e).getTunnelStatus();
        if (status == null || status.getState() != TunnelStatus.State.USE) return;

        // Lazy-load the texture on first render (Minecraft is guaranteed to be ready here)
        modflared$ensureTexture();

        int ix = x + listWidth - MODFLARED_INDICATOR_RIGHT_OFFSET;
        int iy = y + 11;

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        Minecraft.getMinecraft().getTextureManager().bindTexture(MODFLARED_INDICATOR_LOC);
        // textureWidth/Height = INDICATOR_SIZE so UV 0..size maps to the full texture (scales it)
        Gui.drawModalRectWithCustomSizedTexture(
                ix, iy,
                0.0F, 0.0F,
                MODFLARED_INDICATOR_SIZE, MODFLARED_INDICATOR_SIZE,
                MODFLARED_INDICATOR_SIZE, MODFLARED_INDICATOR_SIZE
        );

        if (mouseX >= ix && mouseX <= ix + MODFLARED_INDICATOR_SIZE
                && mouseY >= iy && mouseY <= iy + MODFLARED_INDICATOR_SIZE) {
            this.field_148303_c.setHoveringText(modflared$translate(MODFLARED_TOOLTIP_KEY, MODFLARED_TOOLTIP_FALLBACK));
        }
    }

    @Unique
    private static String modflared$translate(String key, String fallback) {
        String t = I18n.format(key);
        return (t == null || t.equals(key)) ? fallback : t;
    }
}
