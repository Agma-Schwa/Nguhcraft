package org.nguh.nguhcraft.block

import com.mojang.serialization.MapCodec
import net.minecraft.core.BlockPos
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.*
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.CropBlock
import net.minecraft.world.level.block.PotatoBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.IntegerProperty
import net.minecraft.world.level.gameevent.GameEvent
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape
import org.nguh.nguhcraft.item.NguhItems

class GrapeCropBlock(Settings: Properties) : CropBlock(Settings) {
    init {
        registerDefaultState(defaultBlockState().setValue(STICK_LOGGED, true))
    }

    override fun codec(): MapCodec<GrapeCropBlock> {
        return CODEC
    }

    override fun getBaseSeedId(): ItemLike {
        return NguhItems.GRAPE_SEEDS
    }

    override fun getAgeProperty(): IntegerProperty {
        return AGE
    }

    override fun getMaxAge(): Int {
        return MAX_AGE
    }

    override fun getShape(BlockState: BlockState, World: BlockGetter, BlockPos: BlockPos, Context: CollisionContext): VoxelShape {
        return when {
            !IsStickLogged(BlockState) -> FLAT_SHAPE!!
            getAge(BlockState) == 0 -> SMALL_SHAPE!!
            else -> BIG_SHAPE!!
        }
    }

    override fun isRandomlyTicking(BlockState: BlockState): Boolean {
        return super.isRandomlyTicking(BlockState) && IsStickLogged(BlockState)
    }

    override fun isValidBonemealTarget(LevelReader: LevelReader, BlockPos: BlockPos, BlockState: BlockState): Boolean {
        return super.isValidBonemealTarget(LevelReader, BlockPos, BlockState) && IsStickLogged(BlockState)
    }

    override fun useItemOn(
        ItemStack: ItemStack,
        BlockState: BlockState,
        Level: Level,
        BlockPos: BlockPos,
        Player: Player,
        InteractionHand: InteractionHand,
        BlockHitResult: BlockHitResult
    ): InteractionResult? {
        if (ItemStack.`is`(Items.STICK) && !IsStickLogged(BlockState)) {
            Level.setBlockAndUpdate(BlockPos, BlockState.setValue(STICK_LOGGED, true))
            Level.playSound(
                null,
                BlockPos,
                SoundEvents.CROP_PLANTED,
                SoundSource.BLOCKS,
                1f,
                0.8f + Level.random.nextFloat() * 0.4f
            )
            ItemStack.consume(1, Player)
            Level.gameEvent(Player, GameEvent.BLOCK_CHANGE, BlockPos)
            return InteractionResult.SUCCESS
        }
        return super.useItemOn(ItemStack, BlockState, Level, BlockPos, Player, InteractionHand, BlockHitResult)
    }

    override fun useWithoutItem(
        BlockState: BlockState,
        World: Level,
        BlockPos: BlockPos,
        Player: Player,
        BlockHitResult: BlockHitResult
    ): InteractionResult {
        val age = getAge(BlockState)
        if (age != getMaxAge()) {
            return super.useWithoutItem(BlockState, World, BlockPos, Player, BlockHitResult)
        }
        val amount_grapes = 1 + World.random.nextInt(2)
        val amount_seeds = World.random.nextInt(2)
        val amount_leaves = World.random.nextInt(2)

        popResource(World, BlockPos, ItemStack(NguhItems.GRAPES, amount_grapes))
        if (amount_seeds > 0) popResource(World, BlockPos, ItemStack(NguhItems.GRAPE_SEEDS, amount_seeds))
        if (amount_leaves > 0) popResource(World, BlockPos, ItemStack(NguhItems.GRAPE_LEAF, amount_leaves))
        World.playSound(
            null,
            BlockPos,
            SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES,
            SoundSource.BLOCKS,
            1f,
            0.8f + World.random.nextFloat() * 0.4f
        )
        World.setBlock(BlockPos, BlockState.setValue(AGE, 1), UPDATE_CLIENTS)
        World.gameEvent(Player, GameEvent.BLOCK_CHANGE, BlockPos)
        return InteractionResult.SUCCESS
    }

    override fun createBlockStateDefinition(Builder: StateDefinition.Builder<Block?, BlockState?>) {
        Builder.add(AGE, STICK_LOGGED)
    }

    override fun getStateForPlacement(BlockPlaceContext: BlockPlaceContext): BlockState? {
        return super.getStateForPlacement(BlockPlaceContext)?.setValue(STICK_LOGGED, false)
    }

    companion object {
        val CODEC = simpleCodec(::GrapeCropBlock)
        const val MAX_AGE: Int = 4
        val AGE: IntegerProperty = BlockStateProperties.AGE_4
        val STICK_LOGGED: BooleanProperty = BooleanProperty.create("sticklogged")
        private val FLAT_SHAPE: VoxelShape? = column(16.0, 0.0, 2.0)
        private val SMALL_SHAPE: VoxelShape? = cube(9.5, 16.0, 9.5)
        private val BIG_SHAPE: VoxelShape? = cube(16.0)

        fun IsStickLogged(BlockState: BlockState) = BlockState.getValue(STICK_LOGGED)
    }
}

class PeanutCropBlock(settings: Properties) : CropBlock(settings) {
    override fun codec(): MapCodec<PeanutCropBlock> {
        return CODEC
    }

    override fun getBaseSeedId(): ItemLike {
        return NguhItems.PEANUTS
    }

    override fun getShape(BlockState: BlockState, World: BlockGetter, BlockPos: BlockPos, Context: CollisionContext): VoxelShape {
        return SHAPES_BY_AGE[this.getAge(BlockState)]
    }

    companion object {
        val CODEC = simpleCodec(::PeanutCropBlock)
        private val SHAPE_HEIGHTS = arrayOf(2, 4, 5, 9, 11, 14, 14, 14)
        private val SHAPES_BY_AGE =
            boxes(7) { column(16.0, 0.0, SHAPE_HEIGHTS[it].toDouble()) }
    }
}