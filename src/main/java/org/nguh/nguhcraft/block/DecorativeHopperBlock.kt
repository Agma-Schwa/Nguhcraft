package org.nguh.nguhcraft.block

import com.mojang.serialization.MapCodec
import net.minecraft.world.level.block.*
import net.minecraft.world.level.pathfinder.PathComputationType
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.Mirror
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.phys.shapes.BooleanOp
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.VoxelShape
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.shapes.CollisionContext
import org.nguh.nguhcraft.mixin.common.HopperBlockAcessor
import java.util.function.Function


/**
* A block that looks like a hopper, but does nothing.
*
* Useful because a certain *someone* likes to use these for
* decoration, but the block entities tend to be rather laggy,
* which is why this exists.
*
* All of the logic is basically inherited from HopperBlock.
*/
class DecorativeHopperBlock(settings: Properties) : Block(settings) {
    public override fun codec() = CODEC
    init {
        registerDefaultState(
            stateDefinition.any()
                .setValue(HopperBlock.FACING, Direction.DOWN)
        )
    }

    /** Copy-pasted from HopperBlock because it has a block state that we don’t. */
    val ShapeFunction: Function<BlockState, VoxelShape> = run {
        val shape = column(12.0, 11.0, 16.0)
        val voxelShape = Shapes.or(column(16.0, 10.0, 16.0), column(8.0, 4.0, 10.0))
        val voxelShape2 = Shapes.join(voxelShape, shape, BooleanOp.ONLY_FIRST)
        val map = Shapes.rotateAll(
            boxZ(4.0, 4.0, 8.0, 0.0, 8.0), Vec3(8.0, 6.0, 8.0).scale(0.0625)
        )

        getShapeForEachState { state: BlockState ->
            Shapes.or(
                voxelShape2,
                Shapes.join(
                    map[state.getValue(HopperBlock.FACING)]!!,
                    Shapes.block(),
                    BooleanOp.AND
                )
            )
        }
    }

    override fun getShape(
        St: BlockState,
        W: BlockGetter,
        Pos: BlockPos,
        Ctx: CollisionContext
    ) = ShapeFunction.apply(St)

    override fun getInteractionShape(
        St: BlockState,
        W: BlockGetter,
        Pos: BlockPos
    ) = Hopper.`Nguhcraft$GetRaycastShape`(St, W, Pos)

    override fun getStateForPlacement(ctx: BlockPlaceContext): BlockState {
        val Dir = ctx.clickedFace.opposite
        return defaultBlockState().setValue(
            HopperBlock.FACING,
            if (Dir.axis === Direction.Axis.Y) Direction.DOWN else Dir
        )
    }

    override fun rotate(state: BlockState, rotation: Rotation) =
        Hopper.`Nguhcraft$Rotate`(state, rotation)

    override fun mirror(state: BlockState, mirror: Mirror) =
        Hopper.`Nguhcraft$Mirror`(state, mirror)

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(HopperBlock.FACING)
    }

    override fun isPathfindable(state: BlockState, type: PathComputationType) = false

    companion object {
        val CODEC: MapCodec<DecorativeHopperBlock> = simpleCodec(::DecorativeHopperBlock)
        private val Hopper get() = Blocks.HOPPER as HopperBlockAcessor
    }
}
