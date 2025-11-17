package org.nguh.nguhcraft.network

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization
import org.nguh.nguhcraft.Utils

data class ClientboundSyncDisplayPacket(var Lines: List<Component>): CustomPacketPayload {
    override fun type() = ID
    companion object {
        val ID = Utils.PacketId<ClientboundSyncDisplayPacket>("clientbound/sync_display")
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, ClientboundSyncDisplayPacket> = StreamCodec.composite(
            ByteBufCodecs.collection(::ArrayList, ComponentSerialization.TRUSTED_STREAM_CODEC), ClientboundSyncDisplayPacket::Lines,
            ::ClientboundSyncDisplayPacket
        )
    }
}