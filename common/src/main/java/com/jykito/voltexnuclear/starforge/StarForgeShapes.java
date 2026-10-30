package com.jykito.voltexnuclear.starforge;

import com.jykito.voltexnuclear.machine.PieceShapes;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The shapes of a formed star forge's blocks for aim and collision (DESIGN step 16), as the torus's: each block takes the
 * model cut to its own cell, so one walks round the dome and up to the stands. They come from {@link StarForgeShapeData}
 * (written by the model export) and are made once for every place and facing: getShape is asked very often and only hands
 * out what is ready.
 */
public final class StarForgeShapes {

    private StarForgeShapes() {}

    private static final VoxelShape[][] PLACES = new VoxelShape[StarForgeShapeData.PLACES.length][4];

    static {
        for (Direction f : Direction.Plane.HORIZONTAL)
            for (int i = 0; i < PLACES.length; i++) PLACES[i][f.get2DDataValue()] = PieceShapes.parse(StarForgeShapeData.PLACES[i], f);
    }

    /** The shape of the block in place {@code index} of a forge looking {@code facing}. */
    public static VoxelShape place(int index, Direction facing) {
        return PLACES[index][facing.get2DDataValue()];
    }
}
