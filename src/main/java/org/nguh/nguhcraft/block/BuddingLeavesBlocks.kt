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
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.LeavesBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.IntegerProperty
import net.minecraft.world.level.gameevent.GameEvent
import net.minecraft.world.phys.BlockHitResult
import java.util.*

open class GrowingLeavesBlock : LeavesBlock {
    private val LeafParticleEffect: ParticleOptions?

    private constructor(leafParticleChance: Float, leafParticleEffect: ParticleOptions?, settings: Properties) : super(
        leafParticleChance,
        settings
    ) {
        this.LeafParticleEffect = leafParticleEffect
    }

    private constructor(leafParticleChance: Float, settings: Properties) : super(leafParticleChance, settings) {
        LeafParticleEffect = null
    }

    override fun spawnFallingLeavesParticle(world: Level, pos: BlockPos, random: RandomSource) {
        if (LeafParticleEffect != null) {
            val tintedParticleEffect =
                ColorParticleOption.create(ParticleTypes.TINTED_LEAVES, world.getClientLeafTintColor(pos))
            ParticleUtils.spawnParticleBelow(world, pos, random, tintedParticleEffect)
        } else {
            ParticleUtils.spawnParticleBelow(world, pos, random, LeafParticleEffect)
        }
    }

    override fun isRandomlyTicking(state: BlockState): Boolean {
        return !state.getValue(PERSISTENT)
    }

    override fun randomTick(
        state: BlockState,
        world: ServerLevel,
        pos: BlockPos,
        random: RandomSource
    ) {
        super.randomTick(state, world, pos, random)
        val neighbors = countBuddingNeighbors(world, pos)
        if (neighbors < random.nextIntBetweenInclusive(1, 4)) {
            world.setBlock(pos, BaseBlock.defaultBlockState()
                .setValue(PERSISTENT, state.getValue(PERSISTENT))
                .setValue(DISTANCE, state.getValue(DISTANCE))
                .setValue(WATERLOGGED, state.getValue(WATERLOGGED)),
                UPDATE_CLIENTS
            )
        }
    }

    private fun countBuddingNeighbors(
        world: ServerLevel,
        pos: BlockPos
    ): Int {
        var i = 0
        for (x in -1..1) {
            for (z in -1..1) {
                for (y in -1..1) {
                    if (x == 0 && z == 0 && y == 0) { continue }
                    val state = world.getBlockState(pos.offset(x, y, z))
                    if (state.block is BuddingLeavesBlock) { i++ }
                }
            }
        }
        return i
    }

    open val BaseBlock: Block
        get() = NguhBlocks.BUDDING_OAK_LEAVES

    override fun codec(): MapCodec<out LeavesBlock?> {
        return CODEC
    }

    companion object {
        val CODEC: MapCodec<GrowingLeavesBlock> =
            RecordCodecBuilder.mapCodec()
                { instance: RecordCodecBuilder.Instance<GrowingLeavesBlock> ->
                    instance.group(
                        ExtraCodecs.floatRange(0.0f, 1.0f)
                            .fieldOf("leaf_particle_chance")
                            .forGetter { GrowingLeavesBlock: GrowingLeavesBlock -> GrowingLeavesBlock.leafParticleChance },
                        ParticleTypes.CODEC.fieldOf("leaf_particle")
                            .forGetter { GrowingLeavesBlock: GrowingLeavesBlock -> GrowingLeavesBlock.LeafParticleEffect },
                        propertiesCodec()
                    )
                        .apply(
                            instance
                        ) { leafParticleChance: Float, leafParticleEffect: ParticleOptions, settings: Properties ->
                            GrowingLeavesBlock(
                                leafParticleChance,
                                leafParticleEffect,
                                settings
                            )
                        }
                }

        fun CHERRY_LEAVES(settings: Properties): GrowingLeavesBlock {
            return object : GrowingLeavesBlock(0.1f, ParticleTypes.CHERRY_LEAVES, settings) {
                override val BaseBlock: Block
                    get() {
                        return NguhBlocks.BUDDING_CHERRY_LEAVES
                    }
            }
        }

        fun OAK_LEAVES(settings: Properties): GrowingLeavesBlock {
            return object : GrowingLeavesBlock(0.01f, settings) {
            }
        }

        fun DARK_OAK_LEAVES(settings: Properties): GrowingLeavesBlock {
            return object : GrowingLeavesBlock(0.01f, settings) {
                override val BaseBlock: Block
                    get() {
                        return NguhBlocks.BUDDING_DARK_OAK_LEAVES
                    }
            }
        }
    }
}

open class BuddingLeavesBlock(
    ParticleChance: Float,
    private val ParticleEffect: ParticleOptions?,
    Settings: Properties,
) : LeavesBlock(ParticleChance, Settings) {
    init { registerDefaultState(defaultBlockState().setValue(AGE, 0)) }

    override fun spawnFallingLeavesParticle(world: Level, pos: BlockPos, random: RandomSource) {
        if (ParticleEffect != null) {
            val tintedParticleEffect =
                ColorParticleOption.create(ParticleTypes.TINTED_LEAVES, world.getClientLeafTintColor(pos))
            ParticleUtils.spawnParticleBelow(world, pos, random, tintedParticleEffect)
        } else ParticleUtils.spawnParticleBelow(world, pos, random, ParticleEffect)
    }

    fun getAge(state: BlockState): Int {
        return state.getValue(AGE) as Int
    }

    fun withAge(age: Int, state: BlockState): BlockState {
        return state.setValue(AGE, age)
    }

    fun isMature(state: BlockState): Boolean {
        return getAge(state) >= MAX_AGE
    }

    override fun isRandomlyTicking(state: BlockState): Boolean {
        return (!isMature(state) || state.getValue(DISTANCE) == 7) && !(state.getValue(PERSISTENT))
    }

    override fun randomTick(state: BlockState, world: ServerLevel, pos: BlockPos, random: RandomSource) {
        super.randomTick(state, world, pos, random)
        val i = getAge(state)
        if (i < MAX_AGE && !state.getValue(PERSISTENT)) {
            if (random.nextInt(25.0f.toInt() / (8 - state.getValue(DISTANCE))) == 0) {
                world.setBlock(pos, withAge(i + 1, state), UPDATE_CLIENTS)
            }
        }
    }

    open val Fruit: Item
       get() = Items.APPLE

    val fruitChances: FloatArray
        get() = floatArrayOf(0.005f, 0.0055555557f, 0.00625f, 0.008333334f, 0.025f)

    public override fun useWithoutItem(
        state: BlockState,
        world: Level,
        pos: BlockPos,
        player: Player,
        hit: BlockHitResult
    ): InteractionResult {
        return onUse(state, world, pos, player, hit, Fruit, 1, 3)
    }

    private fun onUse(
        state: BlockState,
        world: Level,
        pos: BlockPos,
        player: Player,
        hit: BlockHitResult,
        loot: Item,
        min: Int,
        max: Int
    ): InteractionResult {
        val age = getAge(state)
        if (age == MAX_AGE) {
            val amount = min + (if (max > min) world.random.nextInt(max - min) else 0)
            if (amount > 0) {
                popResource(world, pos, ItemStack(loot, amount))
            }
            world.playSound(
                null,
                pos,
                SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES,
                SoundSource.BLOCKS,
                1f,
                0.8f + world.random.nextFloat() * 0.4f
            )
            val blockState = state.setValue(AGE, 0)
            world.setBlock(pos, blockState, UPDATE_CLIENTS)
            world.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, blockState))
            return InteractionResult.SUCCESS
        } else return super.useWithoutItem(state, world, pos, player, hit)
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block?, BlockState?>) {
        super.createBlockStateDefinition(builder)
        builder.add(AGE)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        return Objects.requireNonNull<BlockState>(super.getStateForPlacement(context))
    }

    override fun codec(): MapCodec<BuddingLeavesBlock> {
        return CODEC
    }

    companion object {
        val CODEC: MapCodec<BuddingLeavesBlock> = RecordCodecBuilder.mapCodec()
            { instance: RecordCodecBuilder.Instance<BuddingLeavesBlock> ->
                instance.group(
                    ExtraCodecs.floatRange(0.0f, 1.0f)
                        .fieldOf("leaf_particle_chance")
                        .forGetter { BuddingLeavesBlock: BuddingLeavesBlock -> BuddingLeavesBlock.leafParticleChance },
                    ParticleTypes.CODEC.fieldOf("leaf_particle")
                        .forGetter { BuddingLeavesBlock: BuddingLeavesBlock -> BuddingLeavesBlock.ParticleEffect },
                    propertiesCodec()
                )
                    .apply(
                        instance
                    ) { leafParticleChance: Float, leafParticleEffect: ParticleOptions, settings: Properties ->
                        BuddingLeavesBlock(
                            leafParticleChance,
                            leafParticleEffect,
                            settings
                        )
                    }
            }
        const val MAX_AGE: Int = 4
        val AGE: IntegerProperty = BlockStateProperties.AGE_4

        fun MakeInstance(
            ParticleChance: Float,
            ParticleEffect: ParticleOptions?,
            Settings: Properties,
            BaseBlock: Block,
            FruitItem: Item?
        ): BuddingLeavesBlock {
            return object : BuddingLeavesBlock(ParticleChance, ParticleEffect, Settings) {
                override fun asItem(): Item { return BaseBlock.asItem() }
                override val Fruit: Item
                    get() = FruitItem ?: super.asItem()
            }
        }
    }
}