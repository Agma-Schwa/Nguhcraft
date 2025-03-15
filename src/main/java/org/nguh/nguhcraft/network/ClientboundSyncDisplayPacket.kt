package org.nguh.nguhcraft.network

import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.codec.PacketCodecs
import net.minecraft.network.packet.CustomPayload
import net.minecraft.text.Text
import net.minecraft.text.TextCodecs
import org.nguh.nguhcraft.Utils

data class ClientboundSyncDisplayPacket(var Lines: List<Text>): CustomPayload {
    override fun getId() = ID
    companion object {
        val ID = Utils.PacketId<ClientboundSyncDisplayPacket>("clientbound/sync_display")
        val CODEC: PacketCodec<RegistryByteBuf, ClientboundSyncDisplayPacket> = PacketCodec.tuple(
            PacketCodecs.collection(::ArrayList, TextCodecs.UNLIMITED_REGISTRY_PACKET_CODEC), ClientboundSyncDisplayPacket::Lines,
            ::ClientboundSyncDisplayPacket
        )
    }
}