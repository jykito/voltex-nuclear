package com.jykito.voltexnuclear.compat.jade;

import com.jykito.voltexnuclear.cascade.CascadeCoreBlockEntity;
import com.jykito.voltexnuclear.column.ColumnCoreBlockEntity;
import com.jykito.voltexnuclear.core.ModInfo;
import com.jykito.voltexnuclear.fusion.TorusCoreBlockEntity;
import com.jykito.voltexnuclear.machine.MachineEntity;
import com.jykito.voltexnuclear.radiochem.RadiochemCoreBlockEntity;
import com.jykito.voltexnuclear.starforge.StarForgeCoreBlockEntity;
import net.minecraft.world.level.block.Block;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Jade entry point. Jade finds the class by its annotation when it starts; without Jade nobody looks here, so the code of
 * the add-on must never reference this package (the same rule as the Voltex plugin).
 */
@WailaPlugin(ModInfo.MOD_ID)
public class NuclearJadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        // the machines whose window numbers Jade shows; the one-block machines share MachineEntity
        registration.registerBlockDataProvider(MachineJadeProvider.INSTANCE, CascadeCoreBlockEntity.class);
        registration.registerBlockDataProvider(MachineJadeProvider.INSTANCE, ColumnCoreBlockEntity.class);
        registration.registerBlockDataProvider(MachineJadeProvider.INSTANCE, RadiochemCoreBlockEntity.class);
        registration.registerBlockDataProvider(MachineJadeProvider.INSTANCE, TorusCoreBlockEntity.class);
        registration.registerBlockDataProvider(MachineJadeProvider.INSTANCE, StarForgeCoreBlockEntity.class);
        registration.registerBlockDataProvider(MachineJadeProvider.INSTANCE, MachineEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(ReactorJadeProvider.INSTANCE, Block.class);
        registration.registerBlockComponent(MachineJadeProvider.INSTANCE, Block.class);
    }
}
