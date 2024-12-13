package org.nguh.nguhcraft.network

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.packet.CustomPayload
import net.minecraft.server.network.ServerPlayerEntity
import org.nguh.nguhcraft.Utils
import org.nguh.nguhcraft.server.accessors.ServerPlayerDiscordAccessor
import java.util.*

data class ClientboundLinkUpdatePacket(
    /** The player.  */
    val PlayerId: UUID,

    /** The player’s Minecraft name. */
    val MinecraftName: String,

    /** The discord colour of the player.  */
    val DiscordColour: Int,

    /** The discord display name, if any.  */
    val DiscordName: String,

    /** Whether the player is linked.  */
    val Linked: Boolean
) : CustomPayload {
    override fun getId() = ID

    private constructor(buf: RegistryByteBuf) : this(
        buf.readUuid(),
        buf.readString(),
        buf.readInt(),
        buf.readString(),
        buf.readBoolean(),
    )

    @Environment(EnvType.SERVER)
    constructor(SP: ServerPlayerEntity) : this(
        SP.uuid,
        SP.nameForScoreboard,
        (SP as ServerPlayerDiscordAccessor).discordColour,
        SP.discordName ?: "",
        SP.isLinked
    )

    private fun Write(buf: RegistryByteBuf) {
        buf.writeUuid(PlayerId)
        buf.writeString(MinecraftName)
        buf.writeInt(DiscordColour)
        buf.writeString(DiscordName)
        buf.writeBoolean(Linked)
    }

    companion object {
        val ID = Utils.PacketId<ClientboundLinkUpdatePacket>("clientbound/link_update")
        val CODEC = MakeCodec(ClientboundLinkUpdatePacket::Write, ::ClientboundLinkUpdatePacket)
    }
}
