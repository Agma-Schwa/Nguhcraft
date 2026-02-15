package org.nguh.nguhcraft.block

import com.mojang.serialization.MapCodec
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape

class TrophyBlock(S: Properties) : HorizontalDirectionalBlock(S) {
    override fun codec(): MapCodec<TrophyBlock> = CODEC

    override fun getShape(
        St: BlockState,
        W: BlockGetter,
        Pos: BlockPos,
        Ctx: CollisionContext
    ): VoxelShape = SHAPE

    init {
        registerDefaultState(
            stateDefinition.any().setValue(FACING, Direction.NORTH)
        )
    }

    override fun getStateForPlacement(blockPlaceContext: BlockPlaceContext): BlockState {
        return defaultBlockState().setValue(
            FACING,
            blockPlaceContext.horizontalDirection.opposite
        )
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) { builder.add(FACING) }

    companion object{
        val CODEC: MapCodec<TrophyBlock> = simpleCodec(::TrophyBlock)

        val SHAPE = Shapes.or(
            column(12.0, 0.0, 1.0),
            column(10.0, 1.0, 2.0),
            column(8.0, 2.0, 3.0),
            column(6.0, 3.0, 23.0)
        )
    }
}