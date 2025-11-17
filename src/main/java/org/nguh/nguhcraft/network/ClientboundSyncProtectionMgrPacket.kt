package org.nguh.nguhcraft.network

import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceKey
import net.minecraft.core.registries.Registries
import org.nguh.nguhcraft.Utils
import org.nguh.nguhcraft.protect.Region
import org.nguh.nguhcraft.protect.RegionLists

/** Packet used to update the ProtectionManager state on the client. */
class ClientboundSyncProtectionMgrPacket(val Regions: RegionLists) : CustomPacketPayload {
    override fun type() = ID
    companion object {
        val ID = Utils.PacketId<ClientboundSyncProtectionMgrPacket>("clientbound/sync_protection_mgr")

        // Type inference is somehow broken and doesn’t work unless we specify a type here
        // and make this a separate variable.
        private val REGIONS_CODEC: StreamCodec<ByteBuf, RegionLists> = ByteBufCodecs.map(
            { mutableMapOf() },
            ResourceKey.streamCodec(Registries.DIMENSION),
            Region.PACKET_CODEC.apply(ByteBufCodecs.collection { mutableListOf() })
        )

        val CODEC = REGIONS_CODEC.map(
            ::ClientboundSyncProtectionMgrPacket,
            ClientboundSyncProtectionMgrPacket::Regions
        )
    }
}