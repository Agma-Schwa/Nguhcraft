package org.nguh.nguhcraft.server

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtString
import net.minecraft.network.packet.CustomPayload
import net.minecraft.registry.RegistryWrapper
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.nguh.nguhcraft.Nbt
import org.nguh.nguhcraft.NbtListOf
import org.nguh.nguhcraft.network.ClientboundSyncDisplayPacket
import org.nguh.nguhcraft.set
import java.util.*

/** Abstract handle for a display. */
abstract class DisplayHandle(val Id: String) {
    abstract fun Listing(): Text
}

/** Client-side display managed by the server. */
class SyncedDisplay(Id: String): DisplayHandle(Id) {
    /** The text lines that are sent to the client. */
    val Lines = mutableListOf<Text>()

    /** Load from Nbt. */
    constructor(Tag: NbtCompound, WL: RegistryWrapper.WrapperLookup) : this(Tag.getString(TAG_ID)) {
        val List = Tag.getList(TAG_LINES, NbtElement.STRING_TYPE.toInt())
        for (Element in List) {
            val T = Text.Serialization.fromJson(Element.asString(), WL)
            if (T != null) Lines.add(T)
        }
    }

    /** Show all lines in the display. */
    override fun Listing(): Text {
        if (Lines.isEmpty()) return Text.literal("Display '$Id' is empty.").formatted(Formatting.YELLOW)
        val T = Text.empty().append(Text.literal("Display '$Id':").formatted(Formatting.YELLOW))
        for (L in Lines) T.append("\n").append(L)
        return T
    }

    /** Save to Nbt. */
    fun Save(WL: RegistryWrapper.WrapperLookup) = Nbt {
        set(TAG_ID, Id)
        set(TAG_LINES, NbtListOf {
            for (Line in Lines) add(
                NbtString.of(Text.Serialization.toJsonString(Line, WL))
            )
        })
    }

    companion object {
        private const val TAG_LINES = "Lines"
        private const val TAG_ID = "Id"
    }
}

/** Object that manages displays. */
class DisplayManager(private val S: MinecraftServer): Manager("Displays") {
    private val Displays = mutableMapOf<String, SyncedDisplay>()
    private val ActiveDisplays = mutableMapOf<UUID, SyncedDisplay>()

    /** Get a display if it exists. */
    fun GetExisting(Id: String): DisplayHandle? = Displays[Id]

    /** Get the names of all displays. */
    fun GetExistingDisplayNames(): Collection<String> = Displays.keys

    /** List all displays. */
    fun ListAll(): MutableText {
        if (Displays.isEmpty()) return Text.literal("No displays defined.")
        val T = Text.literal("Displays:")
        for (D in Displays.values) T.append("\n  - ${D.Id}")
        return T
    }

    override fun ReadData(Tag: NbtElement) {
        if (Tag !is NbtCompound) return

        // Load displays.
        val TDisplays = Tag.getList(TAG_DISPLAYS, NbtElement.COMPOUND_TYPE.toInt())
        for (Element in TDisplays) {
            val D = SyncedDisplay(Element as NbtCompound, S.registryManager)
            Displays[D.Id] = D
        }

        // Load active displays.
        val TActiveDisplays = Tag.getCompound(TAG_ACTIVE_DISPLAYS)
        for (Key in TActiveDisplays.keys) {
            val Id = UUID.fromString(Key)
            val DisplayId = TActiveDisplays.getString(Key)
            val D = GetExisting(DisplayId) as SyncedDisplay?
            if (D != null) ActiveDisplays[Id] = D
        }
    }

    override fun WriteData() = Nbt {
        set(TAG_DISPLAYS, NbtListOf { for (D in Displays.values) add(D.Save(S.registryManager)) })
        set(TAG_ACTIVE_DISPLAYS, Nbt { for ((Id, Display) in ActiveDisplays) set(Id.toString(), Display.Id) })
    }

    override fun Sync(SP: ServerPlayerEntity) {
        val D = ActiveDisplays[SP.uuid]
        if (D != null) SyncDisplay(SP, D)
    }

    override fun Sync(S: MinecraftServer) {
        // The displays differ by player, so we need to sync each player individually.
        for (SP in S.playerManager.playerList) Sync(SP)
    }

    /** Set the active display for a player. */
    fun SetActiveDisplay(SP: ServerPlayerEntity, D: DisplayHandle?) {
        if (D != null) {
            ActiveDisplays[SP.uuid] = D as SyncedDisplay
            SyncDisplay(SP, D)
        } else {
            ActiveDisplays.remove(SP.uuid)
            SyncDisplay(SP, null)
        }
    }

    /** Sync a display to all clients that care about it. */
    private fun Sync(D: SyncedDisplay) {
        for ((PlayerId, D) in ActiveDisplays.filter { it.value == D }) {
            val SP = S.playerManager.getPlayer(PlayerId)
            if (SP != null) SyncDisplay(SP, D)
        }
    }

    /** Sync a display to a player. */
    private fun SyncDisplay(SP: ServerPlayerEntity, D: SyncedDisplay?) {
        ServerPlayNetworking.send(SP, ClientboundSyncDisplayPacket(D?.Lines ?: listOf()))
    }

    /** Update a display and sync it to the client. Creates the display if it doesn’t exist. */
    fun UpdateDisplay(Id: String, Callback: (D: SyncedDisplay) -> Unit) {
        val D = Displays.getOrPut(Id) { SyncedDisplay(Id) }
        Callback(D)
        Sync(D)
    }

    companion object {
        private const val TAG_DISPLAYS = "Displays"
        private const val TAG_ACTIVE_DISPLAYS = "ActiveDisplays"
    }
}

val MinecraftServer.DisplayManager get() = Manager.Get<DisplayManager>(this)
