package org.nguh.nguhcraft.server

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.network.RegistryByteBuf
import net.minecraft.registry.RegistryKey
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.profiler.Profilers
import net.minecraft.world.World
import org.nguh.nguhcraft.Constants
import org.nguh.nguhcraft.server.MCBASIC
import org.nguh.nguhcraft.network.ClientboundSyncProtectionMgrPacket
import org.nguh.nguhcraft.protect.ProtectionManager
import org.nguh.nguhcraft.protect.Region
import org.nguh.nguhcraft.server.accessors.ServerPlayerAccessor
import java.util.UUID

/** Used to signal that a region’s properties are invalid. */
data class MalformedRegionException(val Msg: Text) : Exception()

/** Server-side region. */
class ServerRegion(
    S: MinecraftServer,
    Name: String,
    World: RegistryKey<World>,
    FromX: Int,
    FromZ: Int,
    ToX: Int,
    ToZ: Int
): Region(Name, World, FromX, FromZ, ToX, ToZ) {
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

    /** Deserialise a region. */
    constructor(S: MinecraftServer, Tag: NbtCompound, W: RegistryKey<World>) : this(
        S,
        Tag.getString(TAG_NAME),
        W,
        FromX = Tag.getInt(TAG_MIN_X),
        FromZ = Tag.getInt(TAG_MIN_Z),
        ToX = Tag.getInt(TAG_MAX_X),
        ToZ = Tag.getInt(TAG_MAX_Z)
    ) {
        if (Name.isEmpty()) throw IllegalArgumentException("Region name cannot be empty!")

        // Read flags.
        val FlagsTag = Tag.getCompound(TAG_FLAGS)
        RegionFlags = Flags.entries.fold(0L) { Acc, Flag ->
            if (FlagsTag.getBoolean(Flag.name.lowercase())) Acc or Flag.Bit() else Acc
        }
    }


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
            SP.serverWorld,
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
            Path.append("\n    Invoked by player '").append(SP.displayName)
                .append("':\n    ").append(E.message ?: "Unknown error")
            S.sendError(Path)
            SP.server.BroadcastToOperators(Path.formatted(Formatting.RED))
        }
    }

    /** Save this region. */
    fun Save(): NbtCompound {
        val Tag = NbtCompound()
        Tag.putString(TAG_NAME, Name)
        Tag.putInt(TAG_MIN_X, MinX)
        Tag.putInt(TAG_MIN_Z, MinZ)
        Tag.putInt(TAG_MAX_X, MaxX)
        Tag.putInt(TAG_MAX_Z, MaxZ)

        // Store flags as strings for robustness.
        val FlagsTag = NbtCompound()
        Flags.entries.forEach { FlagsTag.putBoolean(it.name.lowercase(), Test(it)) }
        Tag.put(TAG_FLAGS, FlagsTag)

        return Tag
    }

    /** Set a region flag. */
    fun SetFlag(S: MinecraftServer, Flag: Flags, Allow: Boolean) {
        val OldFlags = RegionFlags
        RegionFlags = if (Allow) OldFlags or Flag.Bit() else OldFlags and Flag.Bit().inv()
        if (OldFlags != RegionFlags) S.ProtectionManager.Sync(S)
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
        TickPlayer(SP, Contains(SP.blockPos))
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

    /** Write this region to a packet. */
    fun Write(buf: RegistryByteBuf) {
        buf.writeString(Name)
        buf.writeInt(MinX)
        buf.writeInt(MinZ)
        buf.writeInt(MaxX)
        buf.writeInt(MaxZ)
        buf.writeLong(RegionFlags)
    }

    companion object {
        private val REGION_TRIGGER_TEXT: Text = Text.of("Region trigger")
        private const val TAG_MIN_X = "MinX"
        private const val TAG_MIN_Z = "MinZ"
        private const val TAG_MAX_X = "MaxX"
        private const val TAG_MAX_Z = "MaxZ"
        private const val TAG_FLAGS = "RegionFlags"
        private const val TAG_NAME = "Name"
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
    Parent: Region,
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
        if (Intersecting != null && R.Contains(Intersecting)) {
            val I = Data.indexOf(Intersecting)
            var J = I + 1
            while (J < Data.size && R.Contains(Data[J])) J++
            Intersecting = if (J == Data.size) null else Data[J]
        }

        // Case 1: There is no region that intersects with R and which R does not
        // fully contain. Simply add R to the end of the list and we’re done.
        if (Intersecting == null) Data.add(R)

        // Case 2: The Intersecting region fully contains R, and it is the first
        // region to do so. Insert R directly before it.
        else if (Intersecting.Contains(R)) Data.add(Data.indexOf(Intersecting), R)

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
class ServerProtectionManager : ProtectionManager(
    OverworldRegions = ServerRegionList(ServerWorld.OVERWORLD),
    NetherRegions = ServerRegionList(ServerWorld.NETHER),
    EndRegions = ServerRegionList(ServerWorld.END)
) {
    companion object {
        private const val TAG_REGIONS = "Regions"
    }

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
        (PE as ServerPlayerAccessor).bypassesRegionProtection

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
    fun LoadRegions(SW: ServerWorld, Tag: NbtCompound) {
        val RegionsTag = Tag.getList(TAG_REGIONS, NbtElement.COMPOUND_TYPE.toInt())
        val Regions = RegionListFor(SW)
        RegionsTag.forEach {
            val R = ServerRegion(SW.server, it as NbtCompound, SW.registryKey)
            Regions.Add(R)
        }
    }

    /** Save regions to a tag. */
    fun SaveRegions(W: ServerWorld, Tag: NbtCompound) {
        val RegionsTag = Tag.getList(TAG_REGIONS, NbtElement.COMPOUND_TYPE.toInt())
        RegionListFor(W).forEach { RegionsTag.add(it.Save()) }
        Tag.put(TAG_REGIONS, RegionsTag)
    }

    /** Get the region list for a world. */
    fun RegionListFor(SW: ServerWorld) = (super.RegionListFor(SW) as ServerRegionList)
    fun ServerRegionListFor(SW: RegistryKey<World>) = (super.RegionListFor(SW) as ServerRegionList)

    /** Send data to the client. */
    fun Send(SP: ServerPlayerEntity) {
        ServerPlayNetworking.send(SP, Serialise())
    }

    /** Write the manager state to a packet. */
    fun Serialise() = ClientboundSyncProtectionMgrPacket(
        OverworldRegions = RegionListFor(ServerWorld.OVERWORLD),
        NetherRegions = RegionListFor(ServerWorld.NETHER),
        EndRegions = RegionListFor(ServerWorld.END)
    )

    /** Sync regions to the clients. */
    fun Sync(Server: MinecraftServer) {
        Server.Broadcast(Serialise())
    }

    /** Fire events that need to happen when a player leaves the server. */
    fun TickPlayerQuit(SP: ServerPlayerEntity) {
        Profilers.get().push("Nguhcraft: Region tick")
        for (R in RegionListFor(SP.serverWorld)) R.TickPlayer(SP, InRegion = false)
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
        for (R in RegionListFor(SP.serverWorld)) R.TickPlayer(SP)

        Profilers.get().pop()
    }
}