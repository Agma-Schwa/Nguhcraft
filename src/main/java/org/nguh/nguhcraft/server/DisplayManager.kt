package org.nguh.nguhcraft.server

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.network.packet.CustomPayload
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.storage.ReadView
import net.minecraft.storage.WriteView
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.text.TextCodecs
import net.minecraft.util.Formatting
import net.minecraft.util.Uuids
import org.nguh.nguhcraft.Named
import org.nguh.nguhcraft.Read
import org.nguh.nguhcraft.With
import org.nguh.nguhcraft.Write
import org.nguh.nguhcraft.network.ClientboundSyncDisplayPacket
import java.util.*

/** Abstract handle for a display. */
abstract class DisplayHandle(val Id: String) {
    abstract fun Listing(): Text
}

/** Client-side display managed by the server. */
class SyncedDisplay(Id: String): DisplayHandle(Id) {
    /** The text lines that are sent to the client. */
    val Lines = mutableListOf<Text>()

    /** Show all lines in the display. */
    override fun Listing(): Text {
        if (Lines.isEmpty()) return Text.literal("Display '$Id' is empty.").formatted(Formatting.YELLOW)
        val T = Text.empty().append(Text.literal("Display '$Id':").formatted(Formatting.YELLOW))
        for (L in Lines) T.append("\n").append(L)
        return T
    }

    companion object {
        val CODEC: Codec<SyncedDisplay> = RecordCodecBuilder.create {
            it.group(
                Codec.STRING.fieldOf("Id").forGetter(SyncedDisplay::Id),
                TextCodecs.CODEC.listOf().fieldOf("Lines").forGetter(SyncedDisplay::Lines)
            ).apply(it) { Id, Lines -> SyncedDisplay(Id).also { it.Lines.addAll(Lines) } }
        }
    }
}

/** Object that manages displays. */
class DisplayManager(private val S: MinecraftServer): Manager() {
    private val Displays = mutableMapOf<String, SyncedDisplay>()
    private val ActiveDisplays = mutableMapOf<UUID, String>()

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

    override fun ReadData(RV: ReadView) = RV.With(KEY) {
        val D = Read(DISPLAYS_CODEC)
        D.ifPresent {
            for (El in it) Displays[El.Id] = El
            Read(ACTIVE_DISPLAYS_CODEC).ifPresent { ActiveDisplays.putAll(it) }
        }
    }

    override fun WriteData(WV: WriteView) = WV.With(KEY) {
        Write(DISPLAYS_CODEC, Displays.values.toList())
        Write(ACTIVE_DISPLAYS_CODEC, ActiveDisplays)
    }

    override fun ToPacket(SP: ServerPlayerEntity): CustomPayload? {
        return ClientboundSyncDisplayPacket(
            ActiveDisplays[SP.uuid]?.let { Displays[it]?.Lines }
                ?: listOf()
        )
    }

    /** Set the active display for a player. */
    fun SetActiveDisplay(SP: ServerPlayerEntity, D: DisplayHandle?) {
        if (D != null) {
            ActiveDisplays[SP.uuid] = D.Id
            Sync(SP)
        } else {
            ActiveDisplays.remove(SP.uuid)
            Sync(SP)
        }
    }

    /** Update a display and sync it to the client. Creates the display if it doesn’t exist. */
    fun UpdateDisplay(Id: String, Callback: (D: SyncedDisplay) -> Unit) {
        val D = Displays.getOrPut(Id) { SyncedDisplay(Id) }
        Callback(D)
        for ((PlayerId) in ActiveDisplays.filter { it.value == D.Id }) {
            val SP = S.playerManager.getPlayer(PlayerId)
            if (SP != null) Sync(SP)
        }
    }

    companion object {
        private const val KEY = "Displays"
        private val DISPLAYS_CODEC = SyncedDisplay.CODEC.listOf().Named("Displays")
        private val ACTIVE_DISPLAYS_CODEC = Codec.unboundedMap(Uuids.CODEC, Codec.STRING).Named("ActiveDisplays")
    }
}

val MinecraftServer.DisplayManager get() = Manager.Get<DisplayManager>(this)
