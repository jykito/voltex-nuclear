package com.jykito.voltexnuclear.compat.jade;

import com.jykito.voltexnuclear.VoltexNuclear;
import com.jykito.voltexnuclear.cascade.CascadeCoreBlockEntity;
import com.jykito.voltexnuclear.client.CascadeScreen;
import com.jykito.voltexnuclear.client.ColumnScreen;
import com.jykito.voltexnuclear.client.RadiochemScreen;
import com.jykito.voltexnuclear.client.StarForgeScreen;
import com.jykito.voltexnuclear.client.StatusLine;
import com.jykito.voltexnuclear.client.TorusScreen;
import com.jykito.voltexnuclear.client.VoltexBars;
import com.jykito.voltexnuclear.column.ColumnCoreBlockEntity;
import com.jykito.voltexnuclear.core.column.HeavyWaterColumn;
import com.jykito.voltexnuclear.core.starforge.StarForge;
import com.jykito.voltexnuclear.fusion.TorusCoreBlockEntity;
import com.jykito.voltexnuclear.machine.MachineEntity;
import com.jykito.voltexnuclear.machine.WindowNumbers;
import com.jykito.voltexnuclear.radiochem.RadiochemCoreBlockEntity;
import com.jykito.voltexnuclear.starforge.StarForgeCoreBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.function.IntUnaryOperator;

/**
 * The add-on's machines in Jade (PLAN stage 12, step 2), aimed at the core of a multiblock or at a one-block machine:
 * the state line of its window, the energy a tick it takes or gives, the progress of its work and, for the torus and the
 * star forge, the heat. The server sends the numbers the window gets as they are ({@link WindowNumbers}); the client
 * reads them with the window's own code, so Jade says what the window says. The reactor has its own provider.
 */
public enum MachineJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final ResourceLocation UID = VoltexNuclear.id("machine");
    private static final String KEY = "voltex_nuclear_window";
    private static final int C_ENERGY = 0x00FFCC, C_LABEL = 0x9AA0A6, C_HEAT = 0xFF9A3C;

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof WindowNumbers machine)) return;
        ContainerData d = machine.windowNumbers();
        int[] numbers = new int[d.getCount()];
        for (int i = 0; i < numbers.length; i++) numbers[i] = d.get(i);
        data.putIntArray(KEY, numbers);
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains(KEY)) return;
        int[] numbers = data.getIntArray(KEY);
        IntUnaryOperator d = i -> i >= 0 && i < numbers.length ? numbers[i] : 0;
        BlockEntity be = accessor.getBlockEntity();
        if (be instanceof CascadeCoreBlockEntity) {
            status(tooltip, CascadeScreen.status(d));
            energy(tooltip, d.applyAsInt(CascadeCoreBlockEntity.D_FE));
            if (d.applyAsInt(CascadeCoreBlockEntity.D_WORKING) == 1) progress(tooltip, d.applyAsInt(CascadeCoreBlockEntity.D_PROGRESS), d.applyAsInt(CascadeCoreBlockEntity.D_WORK));
        } else if (be instanceof ColumnCoreBlockEntity) {
            status(tooltip, ColumnScreen.status(d));
            if (d.applyAsInt(ColumnCoreBlockEntity.D_WORKING) == 1) energy(tooltip, HeavyWaterColumn.energyPerTick(d.applyAsInt(ColumnCoreBlockEntity.D_TIERS)));
        } else if (be instanceof RadiochemCoreBlockEntity) {
            status(tooltip, RadiochemScreen.status(d));
            energy(tooltip, d.applyAsInt(RadiochemCoreBlockEntity.D_FE));
            if (d.applyAsInt(RadiochemCoreBlockEntity.D_WORKING) == 1) progress(tooltip, d.applyAsInt(RadiochemCoreBlockEntity.D_PROGRESS), d.applyAsInt(RadiochemCoreBlockEntity.D_WORK));
        } else if (be instanceof TorusCoreBlockEntity) {
            status(tooltip, TorusScreen.status(d));
            energy(tooltip, StatusLine.wide(d, TorusCoreBlockEntity.D_FE));
            int heat = StatusLine.wide(d, TorusCoreBlockEntity.D_HEAT);
            if (heat > 0) tooltip.add(Component.translatable("gui.voltex_nuclear.reactor.heat", VoltexBars.big(heat)).withStyle(s -> s.withColor(C_HEAT)));
        } else if (be instanceof StarForgeCoreBlockEntity) {
            status(tooltip, StarForgeScreen.status(d));
            int heat = StatusLine.wide(d, StarForgeCoreBlockEntity.D_HEAT);
            tooltip.add(Component.translatable("gui.voltex_nuclear.star_forge.heat").append(": " + VoltexBars.decimal(heat / 1000.0, 1) + "k/"
                    + VoltexBars.decimal(StarForge.STAR_HEAT / 1000.0, 1) + "k").withStyle(s -> s.withColor(heat >= StarForge.STAR_HEAT ? C_HEAT : C_LABEL)));
            int done = d.applyAsInt(StarForgeCoreBlockEntity.D_DONE);
            if (done > 0) progress(tooltip, done, 1000);
        } else if (be instanceof MachineEntity && d.applyAsInt(MachineEntity.D_WORKING) == 1) {
            progress(tooltip, d.applyAsInt(MachineEntity.D_PROGRESS), d.applyAsInt(MachineEntity.D_WORK));
        }
    }

    private static void status(ITooltip tooltip, StatusLine line) {
        tooltip.add(line.text().copy().withStyle(s -> s.withColor(line.color() & 0xFFFFFF)));
    }

    private static void energy(ITooltip tooltip, int fe) {
        tooltip.add(Component.literal(VoltexBars.big(fe) + " FE/t").withStyle(s -> s.withColor(C_ENERGY)));
    }

    /** The Voltex line, "Progress: n%". */
    private static void progress(ITooltip tooltip, int progress, int work) {
        if (work > 0) tooltip.add(Component.translatable("jade.industrial_core.progress", Math.min(100, progress * 100 / work)));
    }
}
