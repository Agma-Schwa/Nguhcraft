package org.nguh.nguhcraft.protect

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.util.CommonColors
import net.minecraft.world.phys.shapes.BooleanOp
import net.minecraft.world.phys.shapes.VoxelShape
import net.minecraft.world.phys.shapes.Shapes
import org.nguh.nguhcraft.SmallEnumSet
import org.nguh.nguhcraft.XZRect
import java.util.*
import kotlin.jvm.optionals.getOrNull

/** A protected region. */
open class Region(
    /** Region name. */
    val Name: String,

    /** Region bounds. */
    FromX: Int,
    FromZ: Int,
    ToX: Int,
    ToZ: Int,

    /**
     * Barrier colour override, if any.
     *
     * We’re using a stupid Java optional here because DFU explodes if you
     * try and hand it a 'null' value.
     */
    ColourOverride: Optional<Int> = Optional.empty(),

    /** Parameter used in deserialisation. */
    _Flags: SmallEnumSet<Flags>? = null,
) : XZRect(
    FromX = FromX,
    FromZ = FromZ,
    ToX = ToX,
    ToZ = ToZ
) {
    /**
    * Flags.
    *
    * These dictate what is allowed. Unset means deny.
    */
    enum class Flags {
        /** Allow attacking non-hostile entities. */
        ATTACK_FRIENDLY,

        /** Allow attacking players. */
        ATTACK_PLAYERS,

        /** Allow interacting with buttons. */
        BUTTONS,

        /**
         * Allow breaking and placing blocks.
         *
         * More specific block interaction flags, e.g. opening wooden
         * doors, are separate flags entirely and not affected by this
         * at all.
         */
        CHANGE_BLOCKS,

        /** Allow opening and closing doors. */
        DOORS,

        /**
         * Allow interacting with entities.
         *
         * Setting this to ALLOW will allow ALL entity interactions; to
         * only allow specific ones, use the flags below instead (e.g.
         * USE_VEHICLES).
         */
        ENTITY_INTERACT,

        /**
         * Allow entities to be affected by the environment.
         *
         * This includes explosions, lightning, etc. and any other
         * environmental hazards and everything that is not caused
         * by a player.
         */
        ENVIRONMENTAL_HAZARDS,

        /**
         * Check if this region allows natural spawning of hostile mobs.
         *
         * TODO: Spawn eggs and commands should not be included and can
         *       always be used to summon hostile mobs regardless.
         */
        HOSTILE_MOB_SPAWNING,

        /** Allow attaching and detaching leashes to mobs, fences, etc. */
        LEASHING,

        /** Allow players to enter this region. */
        PLAYER_ENTRY,

        /** Allow players to leave this region. */
        PLAYER_EXIT,

        /**
         * Allow fall damage.
         *
         * This affects only players.
         */
        PLAYER_FALL_DAMAGE,

        /**
         * Enable pressure plates.
         *
         * This allows entities and players to interact with pressure
         * plates; if disables, pressure plates will simply... not work.
         */
        PRESSURE_PLATES,

        /** Whether to render the entry/exit barrier if there is one. */
        RENDER_ENTRY_EXIT_BARRIER,

        /**
         * Allow teleportation.
         *
         * Note that this flag only handles teleporting inside of a region;
         * PLAYER_ENTRY and PLAYER_EXIT override this flag for the purposes
         * of entering and exiting it.
         *
         * This restricts the use of ender pearls and chorus fruit, but NOT
         * the /tp command, command blocks, or other forms of hard-coded
         * teleporting (endermen etc.).
         */
        TELEPORT,

        /** Allow trading with villagers. */
        TRADE,

        /**
         * Allow using and destroying vehicles.
         *
         * This is one permission because e.g. using minecarts without
         * being able to place or destroy them is fairly useless.
         */
        USE_VEHICLES;
    }

    /**
     * Flags that are set for this region.
     *
     * By default, players are allowed to enter and exit a region.
     */
    val RegionFlags = _Flags ?: SmallEnumSet(Flags.PLAYER_ENTRY, Flags.PLAYER_EXIT)

    /** Colour override, if any. */
    var ColourOverride: Int? = ColourOverride.getOrNull()
        protected set

    /** Voxel shape for collisions from the inside. */
    val InsideShape: VoxelShape = Shapes.join(
        Shapes.INFINITY,
        Shapes.box(
            MinX.toDouble(),
            Double.NEGATIVE_INFINITY,
            MinZ.toDouble(),
            OutsideMaxX.toDouble(),
            Double.POSITIVE_INFINITY,
            OutsideMaxZ.toDouble()
        ),
        BooleanOp.ONLY_FIRST
    )

    /** Voxel shape for collisions from the outside. */
    val OutsideShape: VoxelShape = Shapes.box(
        MinX.toDouble(),
        Double.NEGATIVE_INFINITY,
        MinZ.toDouble(),
        OutsideMaxX.toDouble(),
        Double.POSITIVE_INFINITY,
        OutsideMaxZ.toDouble()
    ).optimize()

    /** Check if this region allows players to attack non-hostile mobs. */
    fun AllowsAttackingFriendlyEntities() = Test(Flags.ATTACK_FRIENDLY)

    /** Check if this region allows block breaking. */
    fun AllowsBlockModification() = Test(Flags.CHANGE_BLOCKS)

    /** Check if this region allows interacting with buttons. */
    fun AllowsButtons() = Test(Flags.BUTTONS)

    /** Check if this region allows interacting with doors. */
    fun AllowsDoors() = Test(Flags.DOORS)

    /** Check if this region allows entity interaction. */
    fun AllowsEntityInteraction() = Test(Flags.ENTITY_INTERACT)

    /** Check if this region allows entities to be affected by the environment. */
    fun AllowsEnvironmentalHazards() = Test(Flags.ENVIRONMENTAL_HAZARDS)

    /** Check if this region allows natural spawning of hostile mobs. */
    fun AllowsHostileMobSpawning() = Test(Flags.HOSTILE_MOB_SPAWNING)

    /** Check if this region allows leashes to be used. */
    fun AllowsLeashing() = Test(Flags.LEASHING)

    /** Check if this region allows players to enter. */
    fun AllowsPlayerEntry() = Test(Flags.PLAYER_ENTRY)

    /** Check if this region allows players to exit. */
    fun AllowsPlayerExit() = Test(Flags.PLAYER_EXIT)

    /** Check if entities should be affected by fall damage. */
    fun AllowsPlayerFallDamage() = Test(Flags.PLAYER_FALL_DAMAGE)

    /** Check if this region allows pressure plates. */
    fun AllowsPressurePlates() = Test(Flags.PRESSURE_PLATES)

    /** Check if this region allows players to be attacked. */
    fun AllowsPvP() = Test(Flags.ATTACK_PLAYERS)

    /** Check if this region allows teleportation. */
    fun AllowsTeleportation() = Test(Flags.TELEPORT)

    /** Check if this region allows vehicle use. */
    fun AllowsVehicleUse() = Test(Flags.ENTITY_INTERACT) || Test(Flags.USE_VEHICLES)

    /** Check if this region allows trading with villagers. */
    fun AllowsVillagerTrading() = Test(Flags.ENTITY_INTERACT) || Test(Flags.TRADE)

    /** Get the colour to use for this region’s barrier, or null if we shouldn’t render the barrier. */
    fun BarrierColor(): Int? = when {
        !Test(Flags.RENDER_ENTRY_EXIT_BARRIER) -> null
        ColourOverride != null -> ColourOverride
        !AllowsPlayerEntry() && !AllowsPlayerExit() -> 0xFFFFAA00.toInt()
        !AllowsPlayerExit() -> CommonColors.SOFT_RED
        !AllowsPlayerEntry() -> CommonColors.HIGH_CONTRAST_DIAMOND
        else -> null
    }

    /** Whether we should render the entry/exit barrier. */
    fun ShouldRenderEntryExitBarrier() = BarrierColor() != null


    /** Helper to simplify testing flags. */
    protected fun Test(Flag: Flags) = RegionFlags.IsSet(Flag)

    /** Get a string representation of this region. */
    override fun toString(): String {
        return "Region($Name, [$MinX, $MinZ] -> [$MaxX, $MaxZ]): $RegionFlags"
    }

    companion object {
        val CODEC: Codec<Region> = RecordCodecBuilder.create {
            it.group(
                Codec.STRING.fieldOf("Name").forGetter(Region::Name),
                Codec.INT.fieldOf("MinX").forGetter(Region::MinX),
                Codec.INT.fieldOf("MinZ").forGetter(Region::MinZ),
                Codec.INT.fieldOf("MaxX").forGetter(Region::MaxX),
                Codec.INT.fieldOf("MaxZ").forGetter(Region::MaxZ),
                Codec.INT.optionalFieldOf("ColourOverride").forGetter({ Optional.ofNullable(it.ColourOverride) }),
                SmallEnumSet.CreateCodec(Flags.entries).fieldOf("RegionFlags").forGetter(Region::RegionFlags),
            ).apply(it, ::Region)
        }

        val PACKET_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, Region::Name,
            ByteBufCodecs.INT, Region::MinX,
            ByteBufCodecs.INT, Region::MinZ,
            ByteBufCodecs.INT, Region::MaxX,
            ByteBufCodecs.INT, Region::MaxZ,
            ByteBufCodecs.optional(ByteBufCodecs.INT), { Optional.ofNullable(it.ColourOverride) },
            SmallEnumSet.CreatePacketCodec<Flags>(), Region::RegionFlags,
            ::Region
        )
    }
}