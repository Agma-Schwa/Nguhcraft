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
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.LeavesBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.IntegerProperty
import net.minecraft.world.level.gameevent.GameEvent
import net.minecraft.world.phys.BlockHitResult
import org.nguh.nguhcraft.item.NguhItems
import java.util.*
import java.util.function.Function

open class GrowingLeavesBlock : LeavesBlock {
    private val leafParticleEffect: ParticleOptions?

    private constructor(leafParticleChance: Float, leafParticleEffect: ParticleOptions?, settings: Properties) : super(
        leafParticleChance,
        settings
    ) {
        this.leafParticleEffect = leafParticleEffect
    }

    private constructor(leafParticleChance: Float, settings: Properties) : super(leafParticleChance, settings) {
        this.leafParticleEffect = null
    }

    override fun spawnFallingLeavesParticle(world: Level, pos: BlockPos, random: RandomSource) {
        if (Objects.isNull(this.leafParticleEffect)) {
            val tintedParticleEffect =
                ColorParticleOption.create(ParticleTypes.TINTED_LEAVES, world.getClientLeafTintColor(pos))
            ParticleUtils.spawnParticleBelow(world, pos, random, tintedParticleEffect)
        } else {
            ParticleUtils.spawnParticleBelow(world, pos, random, this.leafParticleEffect)
        }
    }

    private fun getLeafParticleChance(): Float {
        return this.leafParticleChance
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
            world.setBlock(pos, block.defaultBlockState()
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

    open val block: Block
        get() = NguhBlocks.BUDDING_OAK_LEAVES

    override fun codec(): MapCodec<out LeavesBlock?> {
        return CODEC
    }

    companion object {
        val CODEC: MapCodec<GrowingLeavesBlock?> =
            RecordCodecBuilder.mapCodec<GrowingLeavesBlock?>(
                Function { instance: RecordCodecBuilder.Instance<GrowingLeavesBlock?>? ->
                    instance!!.group(
                        ExtraCodecs.floatRange(0.0f, 1.0f)
                            .fieldOf("leaf_particle_chance")
                            .forGetter<GrowingLeavesBlock?> { GrowingLeavesBlock: GrowingLeavesBlock? -> GrowingLeavesBlock?.getLeafParticleChance() },
                        ParticleTypes.CODEC.fieldOf("leaf_particle")
                            .forGetter<GrowingLeavesBlock?> { GrowingLeavesBlock: GrowingLeavesBlock? -> GrowingLeavesBlock!!.leafParticleEffect },
                        propertiesCodec<GrowingLeavesBlock?>()
                    )
                        .apply<GrowingLeavesBlock?>(
                            instance
                        ) { leafParticleChance: Float?, leafParticleEffect: ParticleOptions?, settings: Properties? ->
                            GrowingLeavesBlock(
                                leafParticleChance!!,
                                leafParticleEffect,
                                settings!!
                            )
                        }
                }
            )

        fun CHERRY_LEAVES(settings: Properties): GrowingLeavesBlock {
            return object : GrowingLeavesBlock(0.1f, ParticleTypes.CHERRY_LEAVES, settings) {
                override val block: Block
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
                override val block: Block
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
    init { registerDefaultState(defaultBlockState().setValue(getAgeProperty(), 0)) }

    override fun spawnFallingLeavesParticle(world: Level, pos: BlockPos, random: RandomSource) {
        if (Objects.isNull(this.leafParticleEffect)) {
            val tintedParticleEffect =
                ColorParticleOption.create(ParticleTypes.TINTED_LEAVES, world.getClientLeafTintColor(pos))
            ParticleUtils.spawnParticleBelow(world, pos, random, tintedParticleEffect)
        } else {
            ParticleUtils.spawnParticleBelow(world, pos, random, this.leafParticleEffect)
        }
    }

    private fun getLeafParticleChance(): Float {
        return this.leafParticleChance
    }

    private fun getAgeProperty(): IntegerProperty {
        return AGE
    }

    private fun getMaxAge(): Int {
        return MAX_AGE
    }

    fun getAge(state: BlockState): Int {
        return state.getValue(getAgeProperty()) as Int
    }

    fun withAge(age: Int, state: BlockState): BlockState {
        return state.setValue(getAgeProperty(), age)
    }

    fun isMature(state: BlockState): Boolean {
        return getAge(state) >= getMaxAge()
    }

    override fun isRandomlyTicking(state: BlockState): Boolean {
        return (!this.isMature(state) || state.getValue(DISTANCE) == 7) && !(state.getValue(PERSISTENT))
    }

    override fun randomTick(state: BlockState, world: ServerLevel, pos: BlockPos, random: RandomSource) {
        super.randomTick(state, world, pos, random)
        val i = getAge(state)
        if (i < getMaxAge() && !state.getValue(PERSISTENT)) {
            if (random.nextInt(25.0f.toInt() / (8 - state.getValue(DISTANCE))) == 0) {
                world.setBlock(pos, withAge(i + 1, state), UPDATE_CLIENTS)
            }
        }
    }

    open val fruit: Item
       get() = Items.APPLE

    val fruitChances: FloatArray
        get() = floatArrayOf(0.005f, 0.0055555557f, 0.00625f, 0.008333334f, 0.025f)
    // used for loot table generation (when adding custom leaves blocks later)

    public override fun useWithoutItem(
        state: BlockState,
        world: Level,
        pos: BlockPos,
        player: Player,
        hit: BlockHitResult
    ): InteractionResult {
        return onUse(state, world, pos, player, hit, this.fruit, 1, 3)
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
        if (age == getMaxAge()) {
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
            val blockState = state.setValue(getAgeProperty(), 0)
            world.setBlock(pos, blockState, UPDATE_CLIENTS)
            world.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, blockState))
            return InteractionResult.SUCCESS
        } else {
            return super.useWithoutItem(state, world, pos, player, hit)
        }
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block?, BlockState?>) {
        super.createBlockStateDefinition(builder)
        builder.add(getAgeProperty())
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        return Objects.requireNonNull<BlockState>(super.getStateForPlacement(context))
    }

    override fun codec(): MapCodec<BuddingLeavesBlock?> {
        return CODEC
    }

    companion object {
        val CODEC: MapCodec<BuddingLeavesBlock?> = RecordCodecBuilder.mapCodec<BuddingLeavesBlock?>(
            Function { instance: RecordCodecBuilder.Instance<BuddingLeavesBlock?>? ->
                instance!!.group(
                    ExtraCodecs.floatRange(0.0f, 1.0f)
                        .fieldOf("leaf_particle_chance")
                        .forGetter<BuddingLeavesBlock?> { BuddingLeavesBlock: BuddingLeavesBlock? -> BuddingLeavesBlock?.getLeafParticleChance() },
                    ParticleTypes.CODEC.fieldOf("leaf_particle")
                        .forGetter<BuddingLeavesBlock?> { BuddingLeavesBlock: BuddingLeavesBlock? -> BuddingLeavesBlock!!.leafParticleEffect },
                    propertiesCodec<BuddingLeavesBlock?>()
                )
                    .apply<BuddingLeavesBlock?>(
                        instance
                    ) { leafParticleChance: Float?, leafParticleEffect: ParticleOptions?, settings: Properties? ->
                        BuddingLeavesBlock(
                            leafParticleChance!!,
                            leafParticleEffect,
                            settings!!
                        )
                    }
            }
        )
        const val MAX_AGE: Int = 4
        val AGE: IntegerProperty = BlockStateProperties.AGE_4

        fun CHERRY_LEAVES(settings: Properties): BuddingLeavesBlock {
            return object : BuddingLeavesBlock(0.1f, ParticleTypes.CHERRY_LEAVES, settings) {
                override val fruit: Item
                    get() {
                        return NguhItems.CHERRY
                    }
                override fun asItem(): Item { return Blocks.CHERRY_LEAVES.asItem() }
            }
        }

        fun OAK_LEAVES(settings: Properties): BuddingLeavesBlock {
            return object : BuddingLeavesBlock(0.01f, settings) {
                override fun asItem(): Item { return Blocks.OAK_LEAVES.asItem() }
            }
        }

        fun DARK_OAK_LEAVES(settings: Properties): BuddingLeavesBlock {
            return object : BuddingLeavesBlock(0.01f, settings) {
                override fun asItem(): Item { return Blocks.DARK_OAK_LEAVES.asItem() }
            }
        }
    }
}