package com.jykito.voltexnuclear.compat.jade;

import com.jykito.voltexnuclear.VoltexNuclear;
import com.jykito.voltexnuclear.client.VoltexBars;
import com.jykito.voltexnuclear.reactor.ReactorCoreBlockEntity;
import com.jykito.voltexnuclear.reactor.ReactorStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * The fission reactor in Jade (PLAN stage 6, step 4): aimed at any block of a formed reactor (the frame, the core, a
 * chamber, a hidden port), its state, power, output and hull. The frame blocks have no block entity, so Jade has no server
 * data for them; the lines come from the client's copy of the core, which the core keeps by its update packets.
 */
public enum ReactorJadeProvider implements IBlockComponentProvider {
    INSTANCE;

    private static final ResourceLocation UID = VoltexNuclear.id("reactor");
    private static final int C_OK = 0x6BE06B, C_BAD = 0xFF5A4A, C_LABEL = 0x9AA0A6, C_ENERGY = 0x00FFCC;

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        BlockPos core = ReactorStructure.formedCore(accessor.getLevel(), accessor.getPosition(), accessor.getBlockState());
        if (core == null || !(accessor.getLevel().getBlockEntity(core) instanceof ReactorCoreBlockEntity be)) return;
        ReactorCoreBlockEntity.Status st = be.clientStatus;
        int color = switch (st) { case RUNNING -> C_OK; case SCRAM, HELD -> C_BAD; default -> C_LABEL; };
        tooltip.add(Component.translatable("gui.voltex_nuclear.reactor.status." + st.name().toLowerCase()).withStyle(s -> s.withColor(color)));
        tooltip.add(Component.translatable("gui.voltex_nuclear.reactor.power", Math.round(be.clientPower * 100)));
        tooltip.add(Component.literal(VoltexBars.big(be.clientFe) + " FE/t").withStyle(s -> s.withColor(C_ENERGY)));
        tooltip.add(Component.translatable("gui.voltex_nuclear.reactor.hull", be.clientHull).withStyle(s -> s.withColor(be.clientHull > 80 ? C_BAD : C_LABEL)));
    }
}
