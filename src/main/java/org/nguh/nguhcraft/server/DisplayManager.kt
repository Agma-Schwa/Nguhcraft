package org.nguh.nguhcraft.server

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.ChatFormatting
import net.minecraft.core.UUIDUtil
import org.nguh.nguhcraft.Named
import org.nguh.nguhcraft.Read
import org.nguh.nguhcraft.With
import org.nguh.nguhcraft.Write
import org.nguh.nguhcraft.network.ClientboundSyncDisplayPacket
import java.util.*

/** Abstract handle for a display. */
abstract class DisplayHandle(val Id: String) {
    abstract fun Listing(): Component
}

/** Client-side display managed by the server. */
class SyncedDisplay(Id: String): DisplayHandle(Id) {
    /** The text lines that are sent to the client. */
    val Lines = mutableListOf<Component>()

    /** Show all lines in the display. */
    override fun Listing(): Component {
        if (Lines.isEmpty()) return Component.literal("Display '$Id' is empty.").withStyle(ChatFormatting.YELLOW)
        val T = Component.empty().append(Component.literal("Display '$Id':").withStyle(ChatFormatting.YELLOW))
        for (L in Lines) T.append("\n").append(L)
        return T
    }

    companion object {
        val CODEC: Codec<SyncedDisplay> = RecordCodecBuilder.create {
            it.group(
                Codec.STRING.fieldOf("Id").forGetter(SyncedDisplay::Id),
                ComponentSerialization.CODEC.listOf().fieldOf("Lines").forGetter(SyncedDisplay::Lines)
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
    fun ListAll(): MutableComponent {
        if (Displays.isEmpty()) return Component.literal("No displays defined.")
        val T = Component.literal("Displays:")
        for (D in Displays.values) T.append("\n  - ${D.Id}")
        return T
    }

    override fun ReadData(RV: ValueInput) = RV.With(KEY) {
        val D = Read(DISPLAYS_CODEC)
        D.ifPresent {
            for (El in it) Displays[El.Id] = El
            Read(ACTIVE_DISPLAYS_CODEC).ifPresent { ActiveDisplays.putAll(it) }
        }
    }

    override fun WriteData(WV: ValueOutput) = WV.With(KEY) {
        Write(DISPLAYS_CODEC, Displays.values.toList())
        Write(ACTIVE_DISPLAYS_CODEC, ActiveDisplays)
    }

    override fun ToPacket(SP: ServerPlayer): CustomPacketPayload {
        return ClientboundSyncDisplayPacket(
            ActiveDisplays[SP.uuid]?.let { Displays[it]?.Lines }
                ?: listOf()
        )
    }

    /** Set the active display for a player. */
    fun SetActiveDisplay(SP: ServerPlayer, D: DisplayHandle?) {
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
            val SP = S.playerList.getPlayer(PlayerId)
            if (SP != null) Sync(SP)
        }
    }

    companion object {
        private const val KEY = "Displays"
        private val DISPLAYS_CODEC = SyncedDisplay.CODEC.listOf().Named("Displays")
        private val ACTIVE_DISPLAYS_CODEC = Codec.unboundedMap(UUIDUtil.AUTHLIB_CODEC, Codec.STRING).Named("ActiveDisplays")
    }
}

val MinecraftServer.DisplayManager get() = Manager.Get<DisplayManager>(this)
