package org.nguh.nguhcraft.server

import com.mojang.logging.LogUtils
import net.minecraft.world.entity.player.Player
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.resources.ResourceKey
import net.minecraft.server.MinecraftServer
import net.minecraft.commands.CommandSourceStack
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Component
import net.minecraft.ChatFormatting
import net.minecraft.util.profiling.Profiler
import net.minecraft.world.level.Level
import org.nguh.nguhcraft.*
import org.nguh.nguhcraft.network.ClientboundSyncProtectionMgrPacket
import org.nguh.nguhcraft.protect.ProtectionManager
import org.nguh.nguhcraft.protect.Region
import java.util.*

/** Used to signal that a region’s properties are invalid. */
data class MalformedRegionException(val Msg: Component) : Exception()

/** Server-side region. */
class ServerRegion(
    S: MinecraftServer,

    /** The world that this region belongs to. */
    val World: ResourceKey<Level>,

    RegionData: Region,
): Region(
    Name = RegionData.Name,
    FromX = RegionData.MinX,
    FromZ = RegionData.MinZ,
    ToX = RegionData.MaxX,
    ToZ = RegionData.MaxZ,
    ColourOverride = Optional.ofNullable(RegionData.ColourOverride),
    _Flags = RegionData.RegionFlags,
) {
    /** Make sure that the name is valid . */
    init {
        if (Name.trim().isEmpty() || Name.contains("/") || Name.contains(".."))
            throw MalformedRegionException(Component.nullToEmpty("Invalid region name '$Name'"))
    }

    /** Command that is run when a player enters the region. */
    val PlayerEntryTrigger = RegionTrigger(S, this, "player_entry")

    /** Command that is run when a player leaves the region. */
    val PlayerLeaveTrigger = RegionTrigger(S, this, "player_leave")

    /**
     * Players that are in this region.
     *
     * This is used to implement leave triggers and other functionality
     * that relies on tracking what players are in a region. It is not
     * persisted across server restarts, so e.g. join triggers will simply
     * fire again once a player logs back in (even in the same session,
     * actually).
     */
    private var PlayersInRegion = mutableSetOf<UUID>()

    /** Display the region’s bounds. */
    fun AppendBounds(MT: MutableComponent): MutableComponent = MT.append(Component.literal(" ["))
        .append(Component.literal("$MinX").withStyle(ChatFormatting.GRAY))
        .append(", ")
        .append(Component.literal("$MinZ").withStyle(ChatFormatting.GRAY))
        .append("] → [")
        .append(Component.literal("$MaxX").withStyle(ChatFormatting.GRAY))
        .append(", ")
        .append(Component.literal("$MaxZ").withStyle(ChatFormatting.GRAY))
        .append("]")


    /** Append the world and name of this region. */
    fun AppendWorldAndName(MT: MutableComponent): MutableComponent = MT
        .append(Component.literal(World.location().path.toString()).withColor(Constants.Lavender))
        .append(":")
        .append(Component.literal(Name).withStyle(ChatFormatting.AQUA))


    /** Run a player trigger. */
    fun InvokePlayerTrigger(SP: ServerPlayer, T: RegionTrigger) {
        if (T.Proc.IsEmpty()) return
        val S = CommandSourceStack(
            SP.server!!,
            SP.position(),
            SP.rotationVector,
            SP.level(),
            RegionTrigger.PERMISSION_LEVEL,
            "Region Trigger",
            REGION_TRIGGER_TEXT,
            SP.server!!,
            null
        )

        try {
            T.Proc.ExecuteAndThrow(S)
        } catch (E: Exception) {
            val Path = Component.literal("Error\n    In trigger ")
            T.AppendName(AppendWorldAndName(Path).append(":"))
            Path.append("\n    Invoked by player '").append(SP.Name)
                .append("':\n    ").append(E.message ?: "Unknown error")
            S.sendFailure(Path)
            S.server?.BroadcastToOperators(Path.withStyle(ChatFormatting.RED))
        }
    }

    /** Set the region colour. */
    fun SetColour(S: MinecraftServer, Colour: Int) {
        if (Colour == ColourOverride) return
        ColourOverride = Colour
        S.ProtectionManager.Sync(S)
    }

    /** Set a region flag. */
    fun SetFlag(S: MinecraftServer, Flag: Flags, Allow: Boolean) {
        if (RegionFlags.IsSet(Flag, Allow))
            return

        RegionFlags.Set(Flag, Allow)
        S.ProtectionManager.Sync(S)
    }

    /** Display this region’s stats. */
    val Stats: Component get() {
        val S = Component.empty()
        Flags.entries.forEach {
            val Status = if (Test(it)) Component.literal("allow").withStyle(ChatFormatting.GREEN)
            else Component.literal("deny").withStyle(ChatFormatting.RED)
            S.append("\n - ")
                .append(Component.literal(it.name.lowercase()).withColor(Constants.Orange))
                .append(": ")
                .append(Status)
        }

        fun Display(T: RegionTrigger) {
            S.append("\n - ")
            T.AppendName(S)
            S.append(":\n")
            T.AppendCommands(S, 4)
        }

        Display(PlayerEntryTrigger)
        Display(PlayerLeaveTrigger)
        return S
    }

    /** Tick this region. */
    fun TickPlayer(SP: ServerPlayer) {
        TickPlayer(SP, SP.blockPosition() in this)
    }

    /**
     * Overload of TickPlayer() used when the position of a player cannot
     * be used to accurately determine whether they are in the region.
     */
    fun TickPlayer(SP: ServerPlayer, InRegion: Boolean) {
        if (InRegion) {
            if (PlayersInRegion.add(SP.uuid)) TickPlayerEntered(SP)
        } else {
            if (PlayersInRegion.remove(SP.uuid)) TickPlayerLeft(SP)
        }
    }

    private fun TickPlayerEntered(SP: ServerPlayer) {
        InvokePlayerTrigger(SP, PlayerEntryTrigger)
    }

    private fun TickPlayerLeft(SP: ServerPlayer) {
        InvokePlayerTrigger(SP, PlayerLeaveTrigger)
    }

    companion object {
        private val REGION_TRIGGER_TEXT: Component = Component.nullToEmpty("Region trigger")
    }
}

/**
 * Trigger that runs when an event happens in a region.
 *
 * These are only present on the server; attempting to access one
 * on the client will always return null.
 *
 * The order in which region triggers are fired if a player enters
 * or leaves multiple regions in a single tick is unspecified.
 */
class RegionTrigger(
    S: MinecraftServer,
    Parent: ServerRegion,
    TriggerName: String,
) {
    /** The trigger’s procedure. */
    val Proc = S.ProcedureManager.GetOrCreateManaged("regions/${Parent.World.location().path}/${Parent.Name}/$TriggerName")

    /** Append a region name to a text element. */
    fun AppendName(MT: MutableComponent): MutableComponent
            = MT.append(Component.literal("${Proc.Name}${Proc.DisplayIndicator()}").withColor(Constants.Orange))

    /** Print this trigger. */
    fun AppendCommands(MT: MutableComponent, Indent: Int): MutableComponent {
        return Proc.DisplaySource(MT, Indent)
    }

    companion object {
        const val PERMISSION_LEVEL = 2
    }
}


/**
 * List of protected regions that supports modification.
 *
 * This class exists solely to maintain the following invariant: For
 * any regions A, B, where A comes immediately before B in the region
 * list, either A and B do not overlap, or A is fully contained within B.
 *
 * This property is important since, if this invariant holds, then for
 * any point P, that is contained within a region, a linear search of
 * the region list will always find the innermost region that contains
 * that point, which is also the region whose permission we want to
 * apply.
 *
 * In other words, this enables us to support nested regions, without
 * having to do any special handling during permission checking, since
 * we’ll always automatically find the innermost one due to the way in
 * which the regions are ordered.
 */
class ServerRegionList(
    /** The world that this region list belongs to. */
    val World: ResourceKey<Level>
) : Collection<ServerRegion> {
    /** The ordered list of regions. */
    val Data = mutableListOf<ServerRegion>()

    /** Get all regions in this list. */
    val Regions get(): List<ServerRegion> = Data

    /** Add a region to this list while maintaining the invariant. */
    @Throws(MalformedRegionException::class)
    fun Add(R: ServerRegion) {
        assert(R.World == World) { "Region is not in this world" }

        // We cannot have two regions with the same name or the exact
        // same bounds in the same world.
        Data.find {
            it.Name == R.Name || (
                it.MinX == R.MinX &&
                it.MinZ == R.MinZ &&
                it.MaxX == R.MaxX &&
                it.MaxZ == R.MaxZ
            )
        }?.let {
            // Display which properties are the same.
            val Msg = if (it.Name != R.Name) R.AppendBounds(Component.literal("Region with bounds "))
            else Component.literal("Region with name ")
                .append(Component.literal(R.Name).withStyle(ChatFormatting.AQUA))

            // And the world it’s in.
            Msg.append(" already exists in world ")
                .append(Component.literal(R.World.location().path.toString())
                    .withColor(Constants.Lavender))
            throw MalformedRegionException(Msg)
        }

        // Check if the region intersects an existing one.
        //
        // Let `R` be the new region, and `Intersecting` the first existing
        // region that intersects R, if it exists. Since we’ve already checked
        // that the bounds are not identical, this leaves us with 4 cases we
        // need to handle here:
        //
        //   1. There is no intersecting region.
        //   2. R is fully contained within Intersecting.
        //   3. Intersecting is fully contained within R.
        //   4. The regions intersect, but neither fully contains the other.
        //
        var Intersecting = Data.find { it.Intersects(R) }

        // Case 3: It turns out that the easiest solution to this is to reduce this
        // case to the other three cases first by skipping over any regions that R
        // fully contains, since we can’t insert R before any of them anyway.
        //
        // After this statement is executed, either Intersecting is null (if it was
        // already null or all remaining regions are fully contained in R), or it
        // is set to the first region that intersects R but is not fully contained
        // in R.
        if (Intersecting != null && Intersecting in R) {
            val I = Data.indexOf(Intersecting)
            var J = I + 1
            while (J < Data.size && Data[J] in R) J++
            Intersecting = if (J == Data.size) null else Data[J]
        }

        // Case 1: There is no region that intersects with R and which R does not
        // fully contain. Simply add R to the end of the list and we’re done.
        if (Intersecting == null) Data.add(R)

        // Case 2: The Intersecting region fully contains R, and it is the first
        // region to do so. Insert R directly before it.
        else if (R in Intersecting) Data.add(Data.indexOf(Intersecting), R)

        // Case 4: This is always invalid, since neither region can reasonably be
        // given priority over any blocks that are in contained by both since there
        // is no parent-child relationship here.
        else throw MalformedRegionException(Component.literal("Region ")
            .append(Component.literal(R.Name).withStyle(ChatFormatting.AQUA))
            .append(" intersects region ")
            .append(Component.literal(Intersecting.Name).withStyle(ChatFormatting.AQUA))
            .append(", but neither fully contains the other")
        )
    }

    /**
     * Remove a region from this list, if it exists.
     *
     * @return Whether the region was present in the list.
     */
    fun Remove(R: ServerRegion): Boolean {
        // No special checking is required here.
        //
        // Removing a region does not invalidate the invariant since it
        // the invariant is transitive: for sequential A, B, C, removing
        // either A or C is irrelevant since A, B or B, C will still be
        // ordered correctly, and removing B maintains the invariant since
        // it already holds for A, C, again by transitivity. By induction,
        // this maintains the invariant for the entire list.
        return Data.remove(R)
    }

    /** Collection interface. */
    override val size: Int get() = Data.size
    override fun contains(element: ServerRegion): Boolean = Data.contains(element)
    override fun containsAll(elements: Collection<ServerRegion>): Boolean = Data.containsAll(elements)
    override fun isEmpty(): Boolean = Data.isEmpty()
    override operator fun iterator(): Iterator<ServerRegion> = Data.iterator()
}

/**
 * Server-side manager state.
 *
 * This manages adding, removing, saving, loading, and syncing regions;
 * code that checks whether something is allowed is in the base class
 * instead.
 */
class ServerProtectionManager(private val S: MinecraftServer) : ProtectionManager(
    mapOf(
        ServerLevel.OVERWORLD to ServerRegionList(ServerLevel.OVERWORLD),
        ServerLevel.NETHER to ServerRegionList(ServerLevel.NETHER),
        ServerLevel.END to ServerRegionList(ServerLevel.END),
    )
) {
    /**
     * This function is the intended way to add a region to a world.
     *
     * This can throw just to ensure we never end up in a situation where we cannot
     * reasonably determine which region a block should belong to.
     *
     * @throws MalformedRegionException If the region name is already taken, or the
     * region bounds are identical to that of another region, or intersect another
     * region without either fully containing the other.
     */
    @Throws(MalformedRegionException::class)
    fun AddRegion(S: MinecraftServer, R: ServerRegion) {
        ServerRegionListFor(R.World).Add(R)
        Sync(S)
    }

    /** Check if this player bypasses region protection. */
    override fun _BypassesRegionProtection(PE: Player) =
        (PE as ServerPlayer).Data.BypassesRegionProtection

    /**
     * This function is the intended way to delete a region from a world.
     *
     * @returns Whether a region was successfully deleted. This can fail
     * if the region does not exist or is not in this world, somehow.
     */
    fun DeleteRegion(S: MinecraftServer, R: ServerRegion) : Boolean {
        if (!ServerRegionListFor(R.World).Remove(R)) return false
        Sync(S)
        return true
    }

    /** Check if a player is linked. */
    override fun IsLinked(PE: Player) =
        ServerUtils.IsLinkedOrOperator(PE as ServerPlayer)

    /**
     * Load regions from a tag.
     *
     * The existing list of regions is cleared.
     */
    override fun ReadData(RV: ValueInput) = RV.With(KEY) {
        for (SW in S.allLevels) {
            val Key = SW.dimension()
            val List = ServerRegionListFor(Key)
            read(Utils.SerialiseWorldToString(Key), LIST_CODEC).ifPresent { it.forEach {
                List.Add(ServerRegion(S, Key, it))
            } }
        }
    }

    /** Save regions to a tag. */
    override fun WriteData(WV: ValueOutput) = WV.With(KEY) {
        for (W in S.allLevels) store(
            Utils.SerialiseWorldToString(W.dimension()),
            LIST_CODEC,
            ServerRegionListFor(W.dimension()).Regions
        )
    }

    /** Get the region list for a world. */
    fun RegionListFor(SW: ServerLevel) = (super.RegionListFor(SW) as ServerRegionList)
    fun ServerRegionListFor(SW: ResourceKey<Level>) = (super.RegionListFor(SW) as ServerRegionList)

    /** Write the manager state to a packet. */
    override fun ToPacket(SP: ServerPlayer) = ClientboundSyncProtectionMgrPacket(Regions)

    /** Fire events that need to happen when a player leaves the server. */
    fun TickPlayerQuit(SP: ServerPlayer) {
        Profiler.get().push("Nguhcraft: Region tick")
        for (R in RegionListFor(SP.level())) R.TickPlayer(SP, InRegion = false)
    }

    /**
     * Fire region-based triggers for this player.
     *
     * We walk the region list for each player because we will never have
     * so many regions that doing that would end up being slower than doing
     * entity lookups for each region.
     */
    fun TickRegionsForPlayer(SP: ServerPlayer) {
        Profiler.get().push("Nguhcraft: Region tick")

        // Tick all regions.
        for (R in RegionListFor(SP.level())) R.TickPlayer(SP)

        Profiler.get().pop()
    }

    companion object {
        val LOGGER = LogUtils.getLogger()
        val KEY = "Regions"
        val LIST_CODEC = Region.CODEC.listOf()
    }
}