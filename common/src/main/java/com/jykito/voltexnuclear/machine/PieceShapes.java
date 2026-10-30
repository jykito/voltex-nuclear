package com.jykito.voltexnuclear.machine;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The shapes of the pieces of a multiblock of places (the fission reactor, the fusion torus) as their model exports write
 * them: boxes in sixteenths for a machine facing south, "x1,y1,z1,x2,y2,z2" separated by spaces.
 */
public final class PieceShapes {

    private PieceShapes() {}

    /** The boxes turned the way the blockstates turn the models (clockwise seen from above), as one shape. */
    public static VoxelShape parse(String boxes, Direction facing) {
        VoxelShape shape = Shapes.empty();
        if (boxes.isEmpty()) return shape;
        for (String box : boxes.split(" ")) {
            String[] v = box.split(",");
            double x1 = Double.parseDouble(v[0]), y1 = Double.parseDouble(v[1]), z1 = Double.parseDouble(v[2]);
            double x2 = Double.parseDouble(v[3]), y2 = Double.parseDouble(v[4]), z2 = Double.parseDouble(v[5]);
            shape = Shapes.or(shape, switch (facing) {
                case WEST -> Block.box(16 - z2, y1, x1, 16 - z1, y2, x2);
                case NORTH -> Block.box(16 - x2, y1, 16 - z2, 16 - x1, y2, 16 - z1);
                case EAST -> Block.box(z1, y1, 16 - x2, z2, y2, 16 - x1);
                default -> Block.box(x1, y1, z1, x2, y2, z2);
            });
        }
        return shape.optimize();
    }
}
