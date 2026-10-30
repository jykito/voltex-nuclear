package com.jykito.voltexnuclear.neoforge.client;

import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.model.HumanoidModel;
import com.jykito.industrialcore.client.armor.ArmorIconRenderer;
import com.jykito.industrialcore.client.armor.PowerSuitModel;
import com.jykito.voltexnuclear.content.NuclearItems;
import com.jykito.voltexnuclear.client.StellarFlight;
import com.jykito.voltexnuclear.client.StellarHud;
import com.jykito.voltexnuclear.VoltexNuclear;
import com.jykito.voltexnuclear.client.CascadeRenderer;
import com.jykito.voltexnuclear.client.ColumnRenderer;
import com.jykito.voltexnuclear.client.StarForgeRenderer;
import com.jykito.voltexnuclear.client.TorusRenderer;
import com.jykito.voltexnuclear.client.RadiochemRenderer;
import com.jykito.voltexnuclear.client.CascadeScreen;
import com.jykito.voltexnuclear.client.ColumnScreen;
import com.jykito.voltexnuclear.client.TorusScreen;
import com.jykito.voltexnuclear.client.ElectrolyzerScreen;
import com.jykito.voltexnuclear.client.RadiochemScreen;
import com.jykito.voltexnuclear.client.GeigerClient;
import com.jykito.voltexnuclear.client.GeigerHud;
import com.jykito.voltexnuclear.client.NuclearGuideModels;
import com.jykito.voltexnuclear.client.NuclearTooltips;
import com.jykito.voltexnuclear.client.PelletPressScreen;
import com.jykito.voltexnuclear.client.Quads;
import com.jykito.voltexnuclear.client.ReactorOutline;
import com.jykito.voltexnuclear.client.ReactorRenderer;
import com.jykito.voltexnuclear.client.ReactorScreen;
import com.jykito.voltexnuclear.client.StarForgeScreen;
import com.jykito.voltexnuclear.content.NuclearBlockEntities;
import com.jykito.voltexnuclear.content.NuclearFluids;
import com.jykito.voltexnuclear.content.NuclearMenus;
import com.jykito.voltexnuclear.core.ModInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * Client hooks on NeoForge 1.21.1; the work itself is in the shared client classes. NeoForge puts each handler on the
 * mod bus or the game bus by the type of its event.
 */
@EventBusSubscriber(modid = ModInfo.MOD_ID, value = Dist.CLIENT)
public final class NeoForgeClientEvents {

    private NeoForgeClientEvents() {}

    /** The Geiger scale as a layer right above the hotbar. */
    @SubscribeEvent
    public static void layers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, VoltexNuclear.id("geiger"), (g, delta) -> GeigerHud.render(g, g.guiWidth(), g.guiHeight()));
        event.registerAboveAll(VoltexNuclear.id("stellar_pellet"), (g, delta) -> StellarHud.render(g, g.guiWidth(), g.guiHeight()));
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

    /** The add-on's fluids in their textures and tints, as Voltex sets its own up on this version. */
    @SubscribeEvent
    public static void fluids(RegisterClientExtensionsEvent event) {
        for (NuclearFluids.Tinted f : NuclearFluids.all()) {
            event.registerFluidType(new IClientFluidTypeExtensions() {
                @Override public ResourceLocation getStillTexture() { return f.still(); }
                @Override public ResourceLocation getFlowingTexture() { return f.flow(); }
                @Override public int getTintColor() { return f.tint(); }
            }, f.source().get().getFluidType());
        }
    }

    /** The Stellar suit on the Voltex power suit model, with its 3D icon, as Voltex hooks up its own suits on this version. */
    @SubscribeEvent
    public static void suit(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
                return PowerSuitModel.forSlot(slot);
            }

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return ArmorIconRenderer.get();
            }
        }, NuclearItems.stellarPieces());
    }


    @SubscribeEvent
    public static void screens(RegisterMenuScreensEvent event) {
        event.register(NuclearMenus.CASCADE.get(), CascadeScreen::new);
        event.register(NuclearMenus.PELLET_PRESS.get(), PelletPressScreen::new);
        event.register(NuclearMenus.ELECTROLYZER.get(), ElectrolyzerScreen::new);
        event.register(NuclearMenus.REACTOR.get(), ReactorScreen::new);
        event.register(NuclearMenus.RADIOCHEM.get(), RadiochemScreen::new);
        event.register(NuclearMenus.COLUMN.get(), ColumnScreen::new);
        event.register(NuclearMenus.TORUS.get(), TorusScreen::new);
        event.register(NuclearMenus.STAR_FORGE.get(), StarForgeScreen::new);
    }

    /** The vertex spellings of this version for the block entity models and the outlines; the formed models of the guide. */
    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        Quads.sink = (vc, pose, x, y, z, shade, u, v, overlay, light, nx, ny, nz) -> vc.addVertex(pose, x, y, z)
                .setColor(shade, shade, shade, 1f).setUv(u, v).setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
        Quads.lines = (vc, pose, x, y, z, r, g, b, a, nx, ny, nz) -> vc.addVertex(pose, x, y, z)
                .setColor(r, g, b, a).setNormal(pose, nx, ny, nz);
        NuclearGuideModels.register();
    }

    /** Aimed at a formed reactor, one outline round the whole model instead of the block's box (DESIGN step 16). */
    @SubscribeEvent
    public static void highlight(RenderHighlightEvent.Block event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && ReactorOutline.draw(mc.level, event.getTarget().getBlockPos(), event.getPoseStack(),
                event.getMultiBufferSource(), event.getCamera().getPosition())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        GeigerClient.tick(mc);
        StellarFlight.tick(mc.player);
    }

    @SubscribeEvent
    public static void tooltip(ItemTooltipEvent event) {
        NuclearTooltips.append(event.getItemStack(), event.getToolTip());
    }
}
