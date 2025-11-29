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
    init { registerDefaultState(defaultBlockState().setValue(STICK_LOGGED, true)) }

    override fun codec() = CODEC
    override fun getBaseSeedId() = NguhItems.GRAPE_SEEDS
    override fun getAgeProperty() = AGE
    override fun getMaxAge() = MAX_AGE

    override fun getShape(St: BlockState, L: BlockGetter, Pos: BlockPos, Ctx: CollisionContext) = when {
        !IsStickLogged(St) -> FLAT_SHAPE
        getAge(St) == 0 -> SMALL_SHAPE
        else -> BIG_SHAPE
    }

    override fun isRandomlyTicking(St: BlockState) =
        super.isRandomlyTicking(St) && IsStickLogged(St)

    override fun isValidBonemealTarget(L: LevelReader, Pos: BlockPos, St: BlockState) =
        super.isValidBonemealTarget(L, Pos, St) && IsStickLogged(St)

    override fun useItemOn(
        S: ItemStack,
        St: BlockState,
        L: Level,
        Pos: BlockPos,
        PE: Player,
        Hand: InteractionHand,
        BHR: BlockHitResult
    ): InteractionResult? {
        if (S.`is`(Items.STICK) && !IsStickLogged(St)) {
            L.setBlockAndUpdate(Pos, St.setValue(STICK_LOGGED, true))
            L.playSound(
                null,
                Pos,
                SoundEvents.CROP_PLANTED,
                SoundSource.BLOCKS,
                1f,
                0.8f + L.random.nextFloat() * 0.4f
            )
            S.consume(1, PE)
            L.gameEvent(PE, GameEvent.BLOCK_CHANGE, Pos)
            return InteractionResult.SUCCESS
        }
        return super.useItemOn(S, St, L, Pos, PE, Hand, BHR)
    }

    override fun useWithoutItem(
        St: BlockState,
        L: Level,
        Pos: BlockPos,
        PE: Player,
        BHR: BlockHitResult
    ): InteractionResult {
        val age = getAge(St)
        if (age != getMaxAge()) return super.useWithoutItem(St, L, Pos, PE, BHR)

        val amount_grapes = 1 + L.random.nextInt(2)
        val amount_seeds = L.random.nextInt(2)
        val amount_leaves = L.random.nextInt(2)
        popResource(L, Pos, ItemStack(NguhItems.GRAPES, amount_grapes))
        if (amount_seeds > 0) popResource(L, Pos, ItemStack(NguhItems.GRAPE_SEEDS, amount_seeds))
        if (amount_leaves > 0) popResource(L, Pos, ItemStack(NguhItems.GRAPE_LEAF, amount_leaves))

        L.playSound(
            null,
            Pos,
            SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES,
            SoundSource.BLOCKS,
            1f,
            0.8f + L.random.nextFloat() * 0.4f
        )

        L.setBlock(Pos, St.setValue(AGE, 1), UPDATE_CLIENTS)
        L.gameEvent(PE, GameEvent.BLOCK_CHANGE, Pos)
        return InteractionResult.SUCCESS
    }

    override fun createBlockStateDefinition(B: StateDefinition.Builder<Block, BlockState>) {
        B.add(AGE, STICK_LOGGED)
    }

    override fun getStateForPlacement(Ctx: BlockPlaceContext) =
        super.getStateForPlacement(Ctx)?.setValue(STICK_LOGGED, false)

    companion object {
        const val MAX_AGE: Int = 4
        val CODEC: MapCodec<GrapeCropBlock> = simpleCodec(::GrapeCropBlock)
        val AGE: IntegerProperty = BlockStateProperties.AGE_4
        val STICK_LOGGED: BooleanProperty = BooleanProperty.create("sticklogged")
        private val FLAT_SHAPE: VoxelShape? = column(16.0, 0.0, 2.0)
        private val SMALL_SHAPE: VoxelShape? = cube(9.5, 16.0, 9.5)
        private val BIG_SHAPE: VoxelShape? = cube(16.0)

        fun IsStickLogged(St: BlockState): Boolean = St.getValue(STICK_LOGGED)
    }
}

class PeanutCropBlock(settings: Properties) : CropBlock(settings) {
    override fun codec() = CODEC
    override fun getBaseSeedId() = NguhItems.PEANUTS
    override fun getShape(St: BlockState, L: BlockGetter, Pos: BlockPos, Ctx: CollisionContext) =
        SHAPES_BY_AGE[this.getAge(St)]

    companion object {
        val CODEC: MapCodec<PeanutCropBlock> = simpleCodec(::PeanutCropBlock)
        private val SHAPE_HEIGHTS = arrayOf(2, 4, 5, 9, 11, 14, 14, 14)
        private val SHAPES_BY_AGE = boxes(7) { column(16.0, 0.0, SHAPE_HEIGHTS[it].toDouble()) }
    }
}