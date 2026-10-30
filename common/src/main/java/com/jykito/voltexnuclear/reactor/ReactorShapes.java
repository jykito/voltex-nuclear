package com.jykito.voltexnuclear.reactor;

import com.jykito.voltexnuclear.machine.PieceShapes;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The shapes of a formed reactor's blocks for aim and collision (DESIGN step 16): each block takes the boxes of its piece
 * of the model cut to its own cell, so the aim goes through the empty corners and one walks up to the body. They come
 * from {@link ReactorShapeData} (written by the model export) and are made once for every place and facing: getShape is
 * asked very often and only hands out what is ready.
 */
public final class ReactorShapes {

    private ReactorShapes() {}

    private static final VoxelShape[][] PLACES = new VoxelShape[ReactorShapeData.PLACES.length][4];
    private static final VoxelShape[] CHAMBER = new VoxelShape[4];

    static {
        for (Direction f : Direction.Plane.HORIZONTAL) {
            for (int i = 0; i < PLACES.length; i++) PLACES[i][f.get2DDataValue()] = PieceShapes.parse(ReactorShapeData.PLACES[i], f);
            CHAMBER[f.get2DDataValue()] = PieceShapes.parse(ReactorShapeData.CHAMBER, f);
        }
    }

    /** The shape of the block in place {@code index} of a reactor looking {@code facing}. */
    public static VoxelShape place(int index, Direction facing) {
        return PLACES[index][facing.get2DDataValue()];
    }

    /** The shape of a formed chamber looking out {@code outward}. */
    public static VoxelShape chamber(Direction outward) {
        return CHAMBER[outward.get2DDataValue()];
    }
}
