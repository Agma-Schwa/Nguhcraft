package org.nguh.nguhcraft.block

import com.mojang.serialization.MapCodec
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.ShapeContext
import net.minecraft.block.Waterloggable
import net.minecraft.entity.ai.pathing.NavigationType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.fluid.Fluid
import net.minecraft.fluid.FluidState
import net.minecraft.fluid.Fluids
import net.minecraft.item.ItemPlacementContext
import net.minecraft.registry.tag.FluidTags
import net.minecraft.state.StateManager
import net.minecraft.state.property.EnumProperty
import net.minecraft.state.property.Properties
import net.minecraft.util.StringIdentifiable
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.random.Random
import net.minecraft.util.shape.VoxelShape
import net.minecraft.util.shape.VoxelShapes
import net.minecraft.world.BlockView
import net.minecraft.world.WorldAccess
import net.minecraft.world.WorldView
import net.minecraft.world.tick.ScheduledTickView
import org.nguh.nguhcraft.minus

class VerticalSlabBlock(S: Settings) : Block(S), Waterloggable {
    enum class Type(val Name: String) : StringIdentifiable {
        NORTH("north"),
        SOUTH("south"),
        WEST("west"),
        EAST("east"),
        DOUBLE("double");

        override fun asString() = Name
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
        defaultState = defaultState
            .with(TYPE, Type.NORTH)
            .with(WATERLOGGED, false)
    }

    override fun appendProperties(B: StateManager.Builder<Block, BlockState>) {
        B.add(TYPE, WATERLOGGED)
    }

    override fun getCodec(): MapCodec<VerticalSlabBlock> = CODEC
    override fun hasSidedTransparency(St : BlockState) =
        St.get(TYPE) != Type.DOUBLE

    override fun getOutlineShape(
        St: BlockState,
        W: BlockView,
        Pos: BlockPos,
        Ctx: ShapeContext
    ): VoxelShape = when (St.get(TYPE)) {
        Type.NORTH -> NORTH_SHAPE
        Type.SOUTH -> SOUTH_SHAPE
        Type.WEST -> WEST_SHAPE
        Type.EAST -> EAST_SHAPE
        Type.DOUBLE -> VoxelShapes.fullCube()
    }

    override fun getPlacementState(Ctx: ItemPlacementContext): BlockState {
        val Pos = Ctx.blockPos
        val Waterlogged = Ctx.world.getFluidState(Pos) == Fluids.WATER
        return defaultState.with(TYPE, GetPlacementType(Ctx)).with(WATERLOGGED, Waterlogged)
    }

    override fun canReplace(St: BlockState, Ctx: ItemPlacementContext): Boolean {
        val Ty = St.get(TYPE)
        if (Ty == Type.DOUBLE || !Ctx.stack.isOf(this.asItem())) return false
        if (Ctx.canReplaceExisting()) return Ty == Type.From(Ctx.side.opposite)
        return true
    }

    override fun getFluidState(St: BlockState): FluidState =
        if (St.get(WATERLOGGED)) Fluids.WATER.getStill(false)
        else super.getFluidState(St)

    override fun tryFillWithFluid(
        W: WorldAccess,
        Pos: BlockPos,
        St: BlockState,
        FS: FluidState
    ) = St.get(TYPE) != Type.DOUBLE && super.tryFillWithFluid(W, Pos, St, FS)

    override fun canFillWithFluid(
        PE: PlayerEntity?,
        W: BlockView,
        Pos: BlockPos,
        St: BlockState,
        FS: Fluid
    ) = St.get(TYPE) != Type.DOUBLE && super.canFillWithFluid(PE, W, Pos, St, FS)

    override fun getStateForNeighborUpdate(
        St: BlockState,
        W: WorldView,
        TV: ScheduledTickView,
        Pos: BlockPos,
        D: Direction,
        NPos: BlockPos,
        NSt: BlockState,
        R: Random
    ): BlockState {
        if (St.get(WATERLOGGED)) TV.scheduleFluidTick(
            Pos,
            Fluids.WATER,
            Fluids.WATER.getTickRate(W)
        )

        return super.getStateForNeighborUpdate(St, W, TV, Pos, D, NPos, NSt, R)
    }

    override fun canPathfindThrough(St: BlockState, Ty: NavigationType) =
        Ty == NavigationType.WATER && St.fluidState.isIn(FluidTags.WATER)


    companion object {
        val CODEC: MapCodec<VerticalSlabBlock> = createCodec(::VerticalSlabBlock)
        val WATERLOGGED = Properties.WATERLOGGED
        val TYPE = EnumProperty.of(
            "type",
            Type.NORTH.javaClass,
            Type.entries
        )

        val NORTH_SHAPE = createCuboidShape(0.0, 0.0, 0.0, 16.0, 16.0, 8.0)
        val SOUTH_SHAPE = createCuboidShape(0.0, 0.0, 8.0, 16.0, 16.0, 16.0)
        val EAST_SHAPE = createCuboidShape(8.0, 0.0, 0.0, 16.0, 16.0, 16.0)
        val WEST_SHAPE = createCuboidShape(0.0, 0.0, 0.0, 8.0, 16.0, 16.0)

        private fun GetPlacementType(Ctx: ItemPlacementContext): Type {
            // If there is already a slab in this position, merge them.
            val Pos = Ctx.blockPos
            if (Ctx.world.getBlockState(Pos).block is VerticalSlabBlock)
                return Type.DOUBLE

            // If the user clicked on an existing block, place the slab up against it.
            if (!Ctx.side.axis.isVertical)
                return Type.From(Ctx.side.opposite)

            // Map the click position to a quadrant within the XZ plane of the block.
            val Quad = Ctx.hitPos - Vec3d.ofCenter(Pos)
            return Type.From(Direction.getFacing(Quad.withAxis(Direction.Axis.Y, 0.0)))
        }
    }
}
