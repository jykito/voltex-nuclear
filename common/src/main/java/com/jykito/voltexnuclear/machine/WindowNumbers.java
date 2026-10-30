package com.jykito.voltexnuclear.machine;

import net.minecraft.world.inventory.ContainerData;

/**
 * A machine whose window numbers Jade shows too (PLAN stage 12, step 2): the server sends them to the client as they are,
 * and the client reads them with the window's own code. The add-on's code never reads them through this; only the Jade
 * plugin does, where Jade is installed.
 */
public interface WindowNumbers {

    /** The numbers the machine's window gets. */
    ContainerData windowNumbers();
}
