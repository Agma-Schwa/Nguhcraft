package org.nguh.nguhcraft.mixin.common;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(HopperBlock.class)
public interface HopperBlockAcessor {
    @Invoker("getShape")
    VoxelShape Nguhcraft$GetOutlineShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context);

    @Invoker("getInteractionShape")
    VoxelShape Nguhcraft$GetRaycastShape(BlockState state, BlockGetter world, BlockPos pos);

    @Invoker("rotate")
    BlockState Nguhcraft$Rotate(BlockState state, Rotation rotation);

    @Invoker("mirror")
    BlockState Nguhcraft$Mirror(BlockState state, Mirror mirror);
}
