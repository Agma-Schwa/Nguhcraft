package org.nguh.nguhcraft.block

import com.mojang.serialization.MapCodec
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.level.block.SimpleWaterloggedBlock
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.pathfinder.PathComputationType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.material.Fluid
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.tags.FluidTags
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.EnumProperty
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.util.StringRepresentable
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.phys.Vec3
import net.minecraft.util.RandomSource
import net.minecraft.world.phys.shapes.VoxelShape
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.ScheduledTickAccess
import org.nguh.nguhcraft.minus

class VerticalSlabBlock(S: Properties) : Block(S), SimpleWaterloggedBlock {
    enum class Type(val Name: String) : StringRepresentable {
        NORTH("north"),
        SOUTH("south"),
        WEST("west"),
        EAST("east"),
        DOUBLE("double");

        override fun getSerializedName() = Name
        companion object {
            fun From(D: Direction) = when (D) {
                Direction.SOUTH -> SOUTH
                Direction.WEST -> WEST
                Direction.EAST -> EAST
                else -> NORTH
            }
        }
    }

    init {
        registerDefaultState(
            defaultBlockState()
                .setValue(TYPE, Type.NORTH)
                .setValue(WATERLOGGED, false)
        )
    }

    override fun createBlockStateDefinition(B: StateDefinition.Builder<Block, BlockState>) {
        B.add(TYPE, WATERLOGGED)
    }

    override fun codec(): MapCodec<VerticalSlabBlock> = CODEC
    override fun useShapeForLightOcclusion(St : BlockState) =
        St.getValue(TYPE) != Type.DOUBLE

    override fun getShape(
        St: BlockState,
        W: BlockGetter,
        Pos: BlockPos,
        Ctx: CollisionContext
    ): VoxelShape = when (St.getValue(TYPE)) {
        Type.NORTH -> NORTH_SHAPE
        Type.SOUTH -> SOUTH_SHAPE
        Type.WEST -> WEST_SHAPE
        Type.EAST -> EAST_SHAPE
        Type.DOUBLE -> Shapes.block()
    }

    override fun getStateForPlacement(Ctx: BlockPlaceContext): BlockState {
        val Pos = Ctx.clickedPos
        val Waterlogged = Ctx.level.getFluidState(Pos) == Fluids.WATER
        return defaultBlockState().setValue(TYPE, GetPlacementType(Ctx)).setValue(WATERLOGGED, Waterlogged)
    }

    override fun canBeReplaced(St: BlockState, Ctx: BlockPlaceContext): Boolean {
        val Ty = St.getValue(TYPE)
        if (Ty == Type.DOUBLE || !Ctx.itemInHand.`is`(this.asItem())) return false
        if (Ctx.replacingClickedOnBlock()) return Ty == Type.From(Ctx.clickedFace.opposite)
        return true
    }

    override fun getFluidState(St: BlockState): FluidState =
        if (St.getValue(WATERLOGGED)) Fluids.WATER.getSource(false)
        else super.getFluidState(St)

    override fun placeLiquid(
        W: LevelAccessor,
        Pos: BlockPos,
        St: BlockState,
        FS: FluidState
    ) = St.getValue(TYPE) != Type.DOUBLE && super.placeLiquid(W, Pos, St, FS)

    override fun canPlaceLiquid(
        PE: LivingEntity?,
        W: BlockGetter,
        Pos: BlockPos,
        St: BlockState,
        FS: Fluid
    ) = St.getValue(TYPE) != Type.DOUBLE && super.canPlaceLiquid(PE, W, Pos, St, FS)

    override fun updateShape(
        St: BlockState,
        W: LevelReader,
        TV: ScheduledTickAccess,
        Pos: BlockPos,
        D: Direction,
        NPos: BlockPos,
        NSt: BlockState,
        R: RandomSource
    ): BlockState {
        if (St.getValue(WATERLOGGED)) TV.scheduleTick(
            Pos,
            Fluids.WATER,
            Fluids.WATER.getTickDelay(W)
        )

        return super.updateShape(St, W, TV, Pos, D, NPos, NSt, R)
    }

    override fun isPathfindable(St: BlockState, Ty: PathComputationType) =
        Ty == PathComputationType.WATER && St.fluidState.`is`(FluidTags.WATER)


    companion object {
        val CODEC: MapCodec<VerticalSlabBlock> = simpleCodec(::VerticalSlabBlock)
        val WATERLOGGED = BlockStateProperties.WATERLOGGED
        val TYPE = EnumProperty.create(
            "type",
            Type.NORTH.javaClass,
            Type.entries
        )

        val NORTH_SHAPE = box(0.0, 0.0, 0.0, 16.0, 16.0, 8.0)
        val SOUTH_SHAPE = box(0.0, 0.0, 8.0, 16.0, 16.0, 16.0)
        val EAST_SHAPE = box(8.0, 0.0, 0.0, 16.0, 16.0, 16.0)
        val WEST_SHAPE = box(0.0, 0.0, 0.0, 8.0, 16.0, 16.0)

        private fun GetPlacementType(Ctx: BlockPlaceContext): Type {
            // If there is already a slab in this position, merge them.
            val Pos = Ctx.clickedPos
            if (Ctx.level.getBlockState(Pos).block is VerticalSlabBlock)
                return Type.DOUBLE

            // If the user clicked on an existing block, place the slab up against it.
            if (!Ctx.clickedFace.axis.isVertical)
                return Type.From(Ctx.clickedFace.opposite)

            // Map the click position to a quadrant within the XZ plane of the block.
            val Quad = Ctx.clickLocation - Vec3.atCenterOf(Pos)
            return Type.From(Direction.getApproximateNearest(Quad.with(Direction.Axis.Y, 0.0)))
        }
    }
}
