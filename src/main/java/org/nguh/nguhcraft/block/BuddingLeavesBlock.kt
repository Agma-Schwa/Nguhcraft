package org.nguh.nguhcraft.block

import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ColorParticleOption
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.util.ExtraCodecs
import net.minecraft.util.ParticleUtils
import net.minecraft.util.RandomSource
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.LeavesBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.IntegerProperty
import net.minecraft.world.level.gameevent.GameEvent
import net.minecraft.world.phys.BlockHitResult

open class BuddingLeavesBlock(
    ParticleChance: Float,
    private val ParticleEffect: ParticleOptions?,
    Settings: Properties,
) : LeavesBlock(ParticleChance, Settings) {
    init { registerDefaultState(defaultBlockState().setValue(AGE, 0)) }

    override fun spawnFallingLeavesParticle(L: Level, Pos: BlockPos, Random: RandomSource) = ParticleUtils.spawnParticleBelow(
        L,
        Pos,
        Random,
        ParticleEffect ?: ColorParticleOption.create(ParticleTypes.TINTED_LEAVES, L.getClientLeafTintColor(Pos))
    )

    override fun isRandomlyTicking(St: BlockState) =
        (St.getValue(AGE) < MAX_AGE || St.getValue(DISTANCE) == 7) &&
        !St.getValue(PERSISTENT)

    override fun randomTick(St: BlockState, SL: ServerLevel, Pos: BlockPos, Random: RandomSource) {
        super.randomTick(St, SL, Pos, Random)
        val Age = St.getValue(AGE)
        if (
            Age < MAX_AGE &&
            !St.getValue(PERSISTENT) &&
            Random.nextInt(25 / (8 - St.getValue(DISTANCE))) == 0
        ) SL.setBlock(Pos, St.setValue(AGE, Age + 1), UPDATE_CLIENTS)
    }

    open val Fruit: Item get() = Items.APPLE

    public override fun useWithoutItem(
        St: BlockState,
        L: Level,
        Pos: BlockPos,
        PE: Player,
        BHR: BlockHitResult
    ) = UseImpl(St, L, Pos, PE, BHR, Fruit, 1, 3)

    private fun UseImpl(
        St: BlockState,
        L: Level,
        Pos: BlockPos,
        PE: Player,
        BHR: BlockHitResult,
        Loot: Item,
        Min: Int,
        Max: Int
    ): InteractionResult {
        val Age = St.getValue(AGE)
        if (Age != MAX_AGE) return super.useWithoutItem(St, L, Pos, PE, BHR)
        val Amount = Min + (if (Max > Min) L.random.nextInt(Max - Min) else 0)
        if (Amount > 0) popResource(L, Pos, ItemStack(Loot, Amount))
        L.playSound(
            null,
            Pos,
            SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES,
            SoundSource.BLOCKS,
            1f,
            0.8f + L.random.nextFloat() * 0.4f
        )
        val NewState = St.setValue(AGE, 0)
        L.setBlock(Pos, NewState, UPDATE_CLIENTS)
        L.gameEvent(GameEvent.BLOCK_CHANGE, Pos, GameEvent.Context.of(PE, NewState))
        return InteractionResult.SUCCESS
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        builder.add(AGE)
    }

    override fun codec() = CODEC
    companion object {
        val CODEC: MapCodec<BuddingLeavesBlock> = RecordCodecBuilder.mapCodec {
            it.group(
                ExtraCodecs.floatRange(0.0f, 1.0f)
                    .fieldOf("leaf_particle_chance")
                    .forGetter(BuddingLeavesBlock::leafParticleChance),
                ParticleTypes.CODEC.fieldOf("leaf_particle")
                    .forGetter(BuddingLeavesBlock::ParticleEffect),
                propertiesCodec()
            ).apply(it, ::BuddingLeavesBlock)
        }

        const val MAX_AGE: Int = 4
        val AGE: IntegerProperty = BlockStateProperties.AGE_4
        val FruitDropChances = floatArrayOf(0.005f, 0.0055555557f, 0.00625f, 0.008333334f, 0.025f)

        fun MakeInstance(
            ParticleChance: Float,
            ParticleEffect: ParticleOptions?,
            Settings: Properties,
            BaseBlock: Block,
            FruitItem: Item? = null
        ): BuddingLeavesBlock {
            return object : BuddingLeavesBlock(ParticleChance, ParticleEffect, Settings) {
                override fun asItem() = BaseBlock.asItem()
                override val Fruit get() = FruitItem ?: super.Fruit
            }
        }
    }
}