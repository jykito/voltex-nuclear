package com.jykito.voltexnuclear.machine;

import com.jykito.industrialcore.block.custom.MultiblockPortBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The hidden ports of a formed machine that checks its shape on events (the reactor, the torus, the star forge). A port
 * is a Voltex block: taken out, it tells the machine nothing, and a stand of two ports (the torus's, the forge's) has no
 * frame beside it to notice. So once a second the core looks at the places of its ports alone, a few blocks, and checks
 * its shape when one is gone (stage 15: the author broke the energy port of a torus stand and the torus burned on).
 */
public final class HiddenPorts {

    private HiddenPorts() {}

    /** Ticks between looks. */
    public static final int EVERY = 20;

    /** Whether a port the last check found is gone, looked at once a second; the cores are spread over the ticks by place. */
    public static boolean gone(Level level, BlockPos core, List<BlockPos> ports) {
        if (ports.isEmpty() || Math.floorMod(level.getGameTime() + core.hashCode(), EVERY) != 0) return false;
        for (BlockPos p : ports)
            if (level.isLoaded(p) && !(level.getBlockState(p).getBlock() instanceof MultiblockPortBlock)) return true;
        return false;
    }
}
