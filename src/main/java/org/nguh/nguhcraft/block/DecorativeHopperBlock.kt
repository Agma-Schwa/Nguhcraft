package org.nguh.nguhcraft.block

import com.mojang.serialization.MapCodec
import net.minecraft.block.*
import net.minecraft.entity.ai.pathing.NavigationType
import net.minecraft.item.ItemPlacementContext
import net.minecraft.state.StateManager
import net.minecraft.state.property.Property
import net.minecraft.util.BlockMirror
import net.minecraft.util.BlockRotation
import net.minecraft.util.function.BooleanBiFunction
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import net.minecraft.util.shape.VoxelShape
import net.minecraft.util.shape.VoxelShapes
import net.minecraft.world.BlockView
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
class DecorativeHopperBlock(settings: Settings) : Block(settings) {
    public override fun getCodec() = CODEC
    init {
        defaultState = stateManager.defaultState
            .with(HopperBlock.FACING, Direction.DOWN)
    }

    /** Copy-pasted from HopperBlock because it has a block state that we don’t. */
    val ShapeFunction: Function<BlockState, VoxelShape> = run {
        val shape = createColumnShape(12.0, 11.0, 16.0)
        val voxelShape = VoxelShapes.union(createColumnShape(16.0, 10.0, 16.0), createColumnShape(8.0, 4.0, 10.0))
        val voxelShape2 = VoxelShapes.combineAndSimplify(voxelShape, shape, BooleanBiFunction.ONLY_FIRST)
        val map = VoxelShapes.createFacingShapeMap(
            createCuboidZShape(4.0, 4.0, 8.0, 0.0, 8.0), Vec3d(8.0, 6.0, 8.0).multiply(0.0625)
        )

        createShapeFunction { state: BlockState ->
            VoxelShapes.union(
                voxelShape2,
                VoxelShapes.combineAndSimplify(
                    map[state.get(HopperBlock.FACING)],
                    VoxelShapes.fullCube(),
                    BooleanBiFunction.AND
                )
            )
        }
    }

    override fun getOutlineShape(
        St: BlockState,
        W: BlockView,
        Pos: BlockPos,
        Ctx: ShapeContext
    ) = ShapeFunction.apply(St)

    override fun getRaycastShape(
        St: BlockState,
        W: BlockView,
        Pos: BlockPos
    ) = Hopper.`Nguhcraft$GetRaycastShape`(St, W, Pos)

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState {
        val Dir = ctx.side.opposite
        return defaultState.with(
            HopperBlock.FACING,
            if (Dir.axis === Direction.Axis.Y) Direction.DOWN else Dir
        )
    }

    override fun rotate(state: BlockState, rotation: BlockRotation) =
        Hopper.`Nguhcraft$Rotate`(state, rotation)

    override fun mirror(state: BlockState, mirror: BlockMirror) =
        Hopper.`Nguhcraft$Mirror`(state, mirror)

    override fun appendProperties(builder: StateManager.Builder<Block, BlockState>) {
        builder.add(HopperBlock.FACING)
    }

    override fun canPathfindThrough(state: BlockState, type: NavigationType) = false

    companion object {
        val CODEC: MapCodec<DecorativeHopperBlock> = createCodec(::DecorativeHopperBlock)
        private val Hopper get() = Blocks.HOPPER as HopperBlockAcessor
    }
}
