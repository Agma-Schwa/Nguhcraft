package org.nguh.nguhcraft.network

import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import org.nguh.nguhcraft.Utils

/**
* This is a chat message packet. It contains a chat message. That’s
* it. No signing, no encryption, no nonsense size limits; just a blob
* of text; that’s all it needs to be. If you want privacy, go talk
* somewhere that isn’t a goddamn Minecraft server ffs.
*/
data class ServerboundChatPacket(
    var Message: String
) : CustomPacketPayload {
    override fun type() = ID
    companion object {
        val ID = Utils.PacketId<ServerboundChatPacket>("serverbound/chat")
        val CODEC: StreamCodec<ByteBuf, ServerboundChatPacket>
            = ByteBufCodecs.STRING_UTF8.map(::ServerboundChatPacket, ServerboundChatPacket::Message)
    }
}
