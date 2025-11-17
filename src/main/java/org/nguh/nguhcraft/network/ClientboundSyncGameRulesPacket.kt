package org.nguh.nguhcraft.network

import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import org.nguh.nguhcraft.Utils

data class ClientboundSyncGameRulesPacket (
    var Flags: Long
) : CustomPacketPayload {
    override fun type() = ID
    companion object {
        val ID = Utils.PacketId<ClientboundSyncGameRulesPacket>("clientbound/sync_game_rules")
        val CODEC: StreamCodec<ByteBuf, ClientboundSyncGameRulesPacket> = ByteBufCodecs.VAR_LONG.map(
            ::ClientboundSyncGameRulesPacket,
            ClientboundSyncGameRulesPacket::Flags
        )
    }

}