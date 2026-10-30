package com.jykito.voltexnuclear.forge.client;

import com.jykito.voltexnuclear.client.CascadeRenderer;
import com.jykito.voltexnuclear.client.ColumnRenderer;
import com.jykito.voltexnuclear.client.StarForgeRenderer;
import com.jykito.voltexnuclear.client.StellarFlight;
import com.jykito.voltexnuclear.client.StellarHud;
import com.jykito.voltexnuclear.client.TorusRenderer;
import com.jykito.voltexnuclear.client.RadiochemRenderer;
import com.jykito.voltexnuclear.client.GeigerClient;
import com.jykito.voltexnuclear.client.GeigerHud;
import com.jykito.voltexnuclear.client.NuclearGuideModels;
import com.jykito.voltexnuclear.client.NuclearTooltips;
import com.jykito.voltexnuclear.client.Quads;
import com.jykito.voltexnuclear.client.ReactorOutline;
import com.jykito.voltexnuclear.client.ReactorRenderer;
import com.jykito.voltexnuclear.content.NuclearBlockEntities;
import com.jykito.voltexnuclear.content.NuclearMenus;
import com.jykito.voltexnuclear.core.ModInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Client hooks on Forge 1.20.1; the work itself is in the shared client classes. */
public final class ForgeClientEvents {

    private ForgeClientEvents() {}

    /** Mod bus: the Geiger scale as an overlay right above the hotbar, the cascade and reactor renderers, the windows, the vertex spelling. */
    @Mod.EventBusSubscriber(modid = ModInfo.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        private ModBus() {}

        @SubscribeEvent
        public static void overlays(RegisterGuiOverlaysEvent event) {
            event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "geiger", (gui, g, partialTick, width, height) -> GeigerHud.render(g, width, height));
            event.registerAboveAll("stellar_pellet", (gui, g, pt, w, h) -> StellarHud.render(g, w, h));
        }

        @SubscribeEvent
        public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(NuclearBlockEntities.CASCADE_CORE.get(), CascadeRenderer::new);
            event.registerBlockEntityRenderer(NuclearBlockEntities.REACTOR_CORE.get(), ReactorRenderer::new);
            event.registerBlockEntityRenderer(NuclearBlockEntities.RADIOCHEM_CORE.get(), RadiochemRenderer::new);
            event.registerBlockEntityRenderer(NuclearBlockEntities.COLUMN_CORE.get(), ColumnRenderer::new);
            event.registerBlockEntityRenderer(NuclearBlockEntities.TORUS_CORE.get(), TorusRenderer::new);
            event.registerBlockEntityRenderer(NuclearBlockEntities.STAR_FORGE_CORE.get(), StarForgeRenderer::new);
        }

        @SubscribeEvent
        public static void setup(FMLClientSetupEvent event) {
            Quads.sink = (vc, pose, x, y, z, shade, u, v, overlay, light, nx, ny, nz) -> vc.vertex(pose.pose(), x, y, z)
                    .color(shade, shade, shade, 1f).uv(u, v).overlayCoords(overlay).uv2(light).normal(pose.normal(), nx, ny, nz).endVertex();
            Quads.lines = (vc, pose, x, y, z, r, g, b, a, nx, ny, nz) -> vc.vertex(pose.pose(), x, y, z)
                    .color(r, g, b, a).normal(pose.normal(), nx, ny, nz).endVertex();
            NuclearGuideModels.register();
            event.enqueueWork(() -> {
                MenuScreens.register(NuclearMenus.CASCADE.get(), ForgeCascadeScreen::new);
                MenuScreens.register(NuclearMenus.PELLET_PRESS.get(), ForgePelletPressScreen::new);
                MenuScreens.register(NuclearMenus.ELECTROLYZER.get(), ForgeElectrolyzerScreen::new);
                MenuScreens.register(NuclearMenus.REACTOR.get(), ForgeReactorScreen::new);
                MenuScreens.register(NuclearMenus.RADIOCHEM.get(), ForgeRadiochemScreen::new);
                MenuScreens.register(NuclearMenus.COLUMN.get(), ForgeColumnScreen::new);
                MenuScreens.register(NuclearMenus.TORUS.get(), ForgeTorusScreen::new);
                MenuScreens.register(NuclearMenus.STAR_FORGE.get(), ForgeStarForgeScreen::new);
            });
        }
    }

    /** Game bus: the counter and the suit's damping tick with the client, the tooltips get their lines, the reactor its outline. */
    @Mod.EventBusSubscriber(modid = ModInfo.MOD_ID, value = Dist.CLIENT)
    public static final class GameBus {
        private GameBus() {}

        @SubscribeEvent
        public static void tick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            GeigerClient.tick(mc);
            StellarFlight.tick(mc.player);
        }

        @SubscribeEvent
        public static void tooltip(ItemTooltipEvent event) {
            NuclearTooltips.append(event.getItemStack(), event.getToolTip());
        }

        /** Aimed at a formed reactor, one outline round the whole model instead of the block's box (DESIGN step 16). */
        @SubscribeEvent
        public static void highlight(RenderHighlightEvent.Block event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null && ReactorOutline.draw(mc.level, event.getTarget().getBlockPos(), event.getPoseStack(),
                    event.getMultiBufferSource(), event.getCamera().getPosition())) event.setCanceled(true);
        }
    }
}
