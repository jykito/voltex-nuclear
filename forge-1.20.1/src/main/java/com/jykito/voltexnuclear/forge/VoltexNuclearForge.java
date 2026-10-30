package com.jykito.voltexnuclear.forge;

import com.jykito.voltexnuclear.VoltexNuclear;
import com.jykito.voltexnuclear.core.ModInfo;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** Forge 1.20.1 entry point: hands the shared code its platform, gives the machines their capabilities, hooks up the Stellar suit. */
@Mod(ModInfo.MOD_ID)
public final class VoltexNuclearForge {

    public VoltexNuclearForge() {
        VoltexNuclear.init(new ForgePlatform(FMLJavaModLoadingContext.get().getModEventBus()));
        MinecraftForge.EVENT_BUS.addGenericListener(BlockEntity.class, ForgeMachineCaps::attach);
        MinecraftForge.EVENT_BUS.addListener(ForgeSuitEvents::tick);
        MinecraftForge.EVENT_BUS.addListener(ForgeSuitEvents::attack);
        MinecraftForge.EVENT_BUS.addListener(ForgeSuitEvents::hurt);
    }
}
