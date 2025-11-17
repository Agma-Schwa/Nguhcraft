package org.nguh.nguhcraft.network

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization
import org.nguh.nguhcraft.Utils

/**
* Chat message sent from the server to the client.
* <p>
* This is used for chat messages and DMs. The message content must be
* a literal string and will be rendered as Markdown on the client.
*/
data class ClientboundChatPacket (
    /**
    * The name to display for the sender/receiver of this message.
    * <p>
    * If this is a public or incoming message, this will be the sender’s
    * name, otherwise, the receiver’s name.
    */
    var PlayerName: Component,

    /** The message content. */
    var Content: String,

    /** Whether this is a private message. */
    var MessageKind: Byte
) : CustomPacketPayload {
    override fun type() = ID
    companion object {
        const val MK_PUBLIC: Byte = 0
        const val MK_OUTGOING_DM: Byte = 1
        const val MK_INCOMING_DM: Byte = 2

        val ID = Utils.PacketId<ClientboundChatPacket>("clientbound/chat")
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, ClientboundChatPacket> = StreamCodec.composite(
            ComponentSerialization.TRUSTED_STREAM_CODEC, ClientboundChatPacket::PlayerName,
            ByteBufCodecs.STRING_UTF8, ClientboundChatPacket::Content,
            ByteBufCodecs.BYTE, ClientboundChatPacket::MessageKind,
            ::ClientboundChatPacket
        )
    }
}