package org.nguh.nguhcraft.network

import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.codec.PacketCodecs
import net.minecraft.network.packet.CustomPayload
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import org.nguh.nguhcraft.Utils
import org.nguh.nguhcraft.protect.Region
import org.nguh.nguhcraft.protect.RegionLists

/** Packet used to update the ProtectionManager state on the client. */
class ClientboundSyncProtectionMgrPacket(val Regions: RegionLists) : CustomPayload {
    override fun getId() = ID
    companion object {
        val ID = Utils.PacketId<ClientboundSyncProtectionMgrPacket>("clientbound/sync_protection_mgr")

        // Type inference is somehow broken and doesn’t work unless we specify a type here
        // and make this a separate variable.
        private val REGIONS_CODEC: PacketCodec<ByteBuf, RegionLists> = PacketCodecs.map(
            { mutableMapOf() },
            RegistryKey.createPacketCodec(RegistryKeys.WORLD),
            Region.PACKET_CODEC.collect(PacketCodecs.toCollection { mutableListOf() })
        )

        val CODEC = REGIONS_CODEC.xmap(
            ::ClientboundSyncProtectionMgrPacket,
            ClientboundSyncProtectionMgrPacket::Regions
        )
    }
}