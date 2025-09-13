package org.nguh.nguhcraft.server

import com.mojang.logging.LogUtils
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.registry.RegistryKey
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.storage.ReadView
import net.minecraft.storage.WriteView
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.profiler.Profilers
import net.minecraft.world.World
import org.nguh.nguhcraft.*
import org.nguh.nguhcraft.network.ClientboundSyncProtectionMgrPacket
import org.nguh.nguhcraft.protect.ProtectionManager
import org.nguh.nguhcraft.protect.Region
import java.util.*

/** Used to signal that a region’s properties are invalid. */
data class MalformedRegionException(val Msg: Text) : Exception()

/** Server-side region. */
class ServerRegion(
    S: MinecraftServer,

    /** The world that this region belongs to. */
    val World: RegistryKey<World>,

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
            throw MalformedRegionException(Text.of("Invalid region name '$Name'"))
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
    fun AppendBounds(MT: MutableText): MutableText = MT.append(Text.literal(" ["))
        .append(Text.literal("$MinX").formatted(Formatting.GRAY))
        .append(", ")
        .append(Text.literal("$MinZ").formatted(Formatting.GRAY))
        .append("] → [")
        .append(Text.literal("$MaxX").formatted(Formatting.GRAY))
        .append(", ")
        .append(Text.literal("$MaxZ").formatted(Formatting.GRAY))
        .append("]")


    /** Append the world and name of this region. */
    fun AppendWorldAndName(MT: MutableText): MutableText = MT
        .append(Text.literal(World.value.path.toString()).withColor(Constants.Lavender))
        .append(":")
        .append(Text.literal(Name).formatted(Formatting.AQUA))


    /** Run a player trigger. */
    fun InvokePlayerTrigger(SP: ServerPlayerEntity, T: RegionTrigger) {
        if (T.Proc.IsEmpty()) return
        val S = ServerCommandSource(
            SP.server,
            SP.pos,
            SP.rotationClient,
            SP.world,
            RegionTrigger.PERMISSION_LEVEL,
            "Region Trigger",
            REGION_TRIGGER_TEXT,
            SP.server,
            null
        )

        try {
            T.Proc.ExecuteAndThrow(S)
        } catch (E: Exception) {
            val Path = Text.literal("Error\n    In trigger ")
            T.AppendName(AppendWorldAndName(Path).append(":"))
            Path.append("\n    Invoked by player '").append(SP.Name)
                .append("':\n    ").append(E.message ?: "Unknown error")
            S.sendError(Path)
            S.server?.BroadcastToOperators(Path.formatted(Formatting.RED))
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
    val Stats: Text get() {
        val S = Text.empty()
        Flags.entries.forEach {
            val Status = if (Test(it)) Text.literal("allow").formatted(Formatting.GREEN)
            else Text.literal("deny").formatted(Formatting.RED)
            S.append("\n - ")
                .append(Text.literal(it.name.lowercase()).withColor(Constants.Orange))
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
    fun TickPlayer(SP: ServerPlayerEntity) {
        TickPlayer(SP, SP.blockPos in this)
    }

    /**
     * Overload of TickPlayer() used when the position of a player cannot
     * be used to accurately determine whether they are in the region.
     */
    fun TickPlayer(SP: ServerPlayerEntity, InRegion: Boolean) {
        if (InRegion) {
            if (PlayersInRegion.add(SP.uuid)) TickPlayerEntered(SP)
        } else {
            if (PlayersInRegion.remove(SP.uuid)) TickPlayerLeft(SP)
        }
    }

    private fun TickPlayerEntered(SP: ServerPlayerEntity) {
        InvokePlayerTrigger(SP, PlayerEntryTrigger)
    }

    private fun TickPlayerLeft(SP: ServerPlayerEntity) {
        InvokePlayerTrigger(SP, PlayerLeaveTrigger)
    }

    companion object {
        private val REGION_TRIGGER_TEXT: Text = Text.of("Region trigger")
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
    val Proc = S.ProcedureManager.GetOrCreateManaged("regions/${Parent.World.value.path}/${Parent.Name}/$TriggerName")

    /** Append a region name to a text element. */
    fun AppendName(MT: MutableText): MutableText
            = MT.append(Text.literal("${Proc.Name}${Proc.DisplayIndicator()}").withColor(Constants.Orange))

    /** Print this trigger. */
    fun AppendCommands(MT: MutableText, Indent: Int): MutableText {
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
    val World: RegistryKey<World>
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
            val Msg = if (it.Name != R.Name) R.AppendBounds(Text.literal("Region with bounds "))
            else Text.literal("Region with name ")
                .append(Text.literal(R.Name).formatted(Formatting.AQUA))

            // And the world it’s in.
            Msg.append(" already exists in world ")
                .append(Text.literal(R.World.value.path.toString())
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
        else throw MalformedRegionException(Text.literal("Region ")
            .append(Text.literal(R.Name).formatted(Formatting.AQUA))
            .append(" intersects region ")
            .append(Text.literal(Intersecting.Name).formatted(Formatting.AQUA))
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
        ServerWorld.OVERWORLD to ServerRegionList(ServerWorld.OVERWORLD),
        ServerWorld.NETHER to ServerRegionList(ServerWorld.NETHER),
        ServerWorld.END to ServerRegionList(ServerWorld.END),
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
    override fun _BypassesRegionProtection(PE: PlayerEntity) =
        (PE as ServerPlayerEntity).Data.BypassesRegionProtection

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
    override fun IsLinked(PE: PlayerEntity) =
        ServerUtils.IsLinkedOrOperator(PE as ServerPlayerEntity)

    /**
     * Load regions from a tag.
     *
     * The existing list of regions is cleared.
     */
    override fun ReadData(RV: ReadView) = RV.With(KEY) {
        for (SW in S.worlds) {
            val Key = SW.registryKey
            val List = ServerRegionListFor(Key)
            read(Utils.SerialiseWorldToString(Key), LIST_CODEC).ifPresent { it.forEach {
                List.Add(ServerRegion(S, Key, it))
            } }
        }
    }

    /** Save regions to a tag. */
    override fun WriteData(WV: WriteView) = WV.With(KEY) {
        for (W in S.worlds) put(
            Utils.SerialiseWorldToString(W.registryKey),
            LIST_CODEC,
            ServerRegionListFor(W.registryKey).Regions
        )
    }

    /** Get the region list for a world. */
    fun RegionListFor(SW: ServerWorld) = (super.RegionListFor(SW) as ServerRegionList)
    fun ServerRegionListFor(SW: RegistryKey<World>) = (super.RegionListFor(SW) as ServerRegionList)

    /** Write the manager state to a packet. */
    override fun ToPacket(SP: ServerPlayerEntity) = ClientboundSyncProtectionMgrPacket(Regions)

    /** Fire events that need to happen when a player leaves the server. */
    fun TickPlayerQuit(SP: ServerPlayerEntity) {
        Profilers.get().push("Nguhcraft: Region tick")
        for (R in RegionListFor(SP.world)) R.TickPlayer(SP, InRegion = false)
    }

    /**
     * Fire region-based triggers for this player.
     *
     * We walk the region list for each player because we will never have
     * so many regions that doing that would end up being slower than doing
     * entity lookups for each region.
     */
    fun TickRegionsForPlayer(SP: ServerPlayerEntity) {
        Profilers.get().push("Nguhcraft: Region tick")

        // Tick all regions.
        for (R in RegionListFor(SP.world)) R.TickPlayer(SP)

        Profilers.get().pop()
    }

    companion object {
        val LOGGER = LogUtils.getLogger()
        val KEY = "Regions"
        val LIST_CODEC = Region.CODEC.listOf()
    }
}