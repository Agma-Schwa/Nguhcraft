package org.nguh.nguhcraft.network

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.level.ServerPlayer
import org.nguh.nguhcraft.Utils
import org.nguh.nguhcraft.server.Data
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
) : CustomPacketPayload {
    override fun type() = ID

    private constructor(buf: RegistryFriendlyByteBuf) : this(
        buf.readUUID(),
        buf.readUtf(),
        buf.readInt(),
        buf.readUtf(),
        buf.readBoolean(),
    )

    @Environment(EnvType.SERVER)
    constructor(SP: ServerPlayer) : this(
        SP.uuid,
        SP.scoreboardName,
        SP.Data.DiscordColour,
        SP.Data.DiscordName,
        SP.Data.IsLinked
    )

    private fun Write(buf: RegistryFriendlyByteBuf) {
        buf.writeUUID(PlayerId)
        buf.writeUtf(MinecraftName)
        buf.writeInt(DiscordColour)
        buf.writeUtf(DiscordName)
        buf.writeBoolean(Linked)
    }

    companion object {
        val ID = Utils.PacketId<ClientboundLinkUpdatePacket>("clientbound/link_update")
        val CODEC = MakeCodec(ClientboundLinkUpdatePacket::Write, ::ClientboundLinkUpdatePacket)
    }
}
