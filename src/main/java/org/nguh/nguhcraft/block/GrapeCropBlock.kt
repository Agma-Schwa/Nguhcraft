package org.nguh.nguhcraft.block

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

class GrapeCropBlock(settings: Properties) : CropBlock(settings) {
    fun init() {
        this.registerDefaultState(super.defaultBlockState().setValue(this.getStickLoggedProperty(), true))
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

    fun getStickLoggedProperty(): BooleanProperty {
        return STICK_LOGGED
    }

    fun getStickLogged(blockState: BlockState): Boolean {
        return blockState.getValue(this.getStickLoggedProperty())
    }

    override fun getShape(state: BlockState, world: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        if (!getStickLogged(state)) {
            return FLAT_SHAPE!!
        } else if (getAge(state) == 0) {
            return SMALL_SHAPE!!
        } else {
            return BIG_SHAPE!!
        }
    }

    override fun isRandomlyTicking(blockState: BlockState): Boolean {
        return super.isRandomlyTicking(blockState) && getStickLogged(blockState)
    }

    override fun isValidBonemealTarget(levelReader: LevelReader, blockPos: BlockPos, blockState: BlockState): Boolean {
        return super.isValidBonemealTarget(levelReader, blockPos, blockState) && getStickLogged(blockState)
    }

    protected override fun useItemOn(
        itemStack: ItemStack,
        blockState: BlockState,
        level: Level,
        blockPos: BlockPos,
        player: Player,
        interactionHand: InteractionHand,
        blockHitResult: BlockHitResult
    ): InteractionResult? {
        if (itemStack.`is`(Items.STICK) && !getStickLogged(blockState)) {
            level.setBlockAndUpdate(blockPos, blockState.setValue(getStickLoggedProperty(), true))
            level.playSound(
                null,
                blockPos,
                SoundEvents.CROP_PLANTED,
                SoundSource.BLOCKS,
                1f,
                0.8f + level.random.nextFloat() * 0.4f
            )
            itemStack.consume(1, player)
            level.gameEvent(player, GameEvent.BLOCK_CHANGE, blockPos)
            return InteractionResult.SUCCESS
        }
        return super.useItemOn(itemStack, blockState, level, blockPos, player, interactionHand, blockHitResult)
    }

    public override fun useWithoutItem(
        state: BlockState,
        world: Level,
        pos: BlockPos,
        player: Player,
        hit: BlockHitResult
    ): InteractionResult {
        val age = getAge(state)
        if (age == getMaxAge()) {
            val amount_grapes = 1 + world.random.nextInt(2)
            val amount_seeds = world.random.nextInt(2)
            val amount_leaves = world.random.nextInt(2)

            popResource(world, pos, ItemStack(NguhItems.GRAPES, amount_grapes))
            if (amount_seeds > 0) {
                popResource(world, pos, ItemStack(NguhItems.GRAPE_SEEDS, amount_seeds))
            }
            if (amount_leaves > 0) {
                popResource(world, pos, ItemStack(NguhItems.GRAPE_LEAF, amount_leaves))
            }
            world.playSound(
                null,
                pos,
                SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES,
                SoundSource.BLOCKS,
                1f,
                0.8f + world.random.nextFloat() * 0.4f
            )
            world.setBlock(pos, state.setValue(AGE, 1), UPDATE_CLIENTS)
            world.gameEvent(player, GameEvent.BLOCK_CHANGE, pos)
            return InteractionResult.SUCCESS
        } else {
            return super.useWithoutItem(state, world, pos, player, hit)
        }
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block?, BlockState?>) {
        builder.add(AGE).add(STICK_LOGGED)
    }

    override fun getStateForPlacement(blockPlaceContext: BlockPlaceContext): BlockState? {
        return super.getStateForPlacement(blockPlaceContext)?.setValue(getStickLoggedProperty(), false)
    }

    companion object {
        const val MAX_AGE: Int = 4
        val AGE: IntegerProperty = BlockStateProperties.AGE_4
        val STICK_LOGGED: BooleanProperty = BooleanProperty.create("sticklogged")
        private val FLAT_SHAPE: VoxelShape? = column(16.0, 0.0, 2.0)
        private val SMALL_SHAPE: VoxelShape? = cube(9.5, 16.0, 9.5)
        private val BIG_SHAPE: VoxelShape? = cube(16.0)
    }
}