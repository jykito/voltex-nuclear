package com.jykito.voltexnuclear.cascade;

import net.minecraft.util.StringRepresentable;

/**
 * What a block of the add-on casing is in a formed cascade or radiochemical plant: its block model is that piece of the machine (DESIGN step 18,
 * the still parts are baked into the chunk). {@link #NONE} is a plain casing block.
 */
public enum CasingRole implements StringRepresentable {
    NONE("none"),
    /** The plinth under the row, the cabinet included; a port may stand in its place. */
    PLINTH("plinth"),
    /** The lower part of a column, over the plinth. */
    COLUMN("column"),
    /** The top of a column with the header pipe over it. */
    COLLECTOR("collector"),
    /** The lower block of the cabinet over the plinth; a port may stand in its place. */
    CABINET_LOW("cabinet_low"),
    /** The block of the cabinet under the core, with the lamp; a port may stand in its place. */
    CABINET_MID("cabinet_mid"),
    /** Any block of a formed radiochemical plant; which piece of the plant it shows is its {@code piece}, the cell's number. */
    PLANT("plant"),
    /** Any casing of a formed heavy water column, the tiers and the pillar; which piece it shows is its {@code piece} (ColumnStructure.piece). */
    HEAVY_COLUMN("heavy_column");

    private final String name;

    CasingRole(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
