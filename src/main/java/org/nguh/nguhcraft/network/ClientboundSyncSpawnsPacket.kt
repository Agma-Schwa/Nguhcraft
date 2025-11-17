package org.nguh.nguhcraft.network

import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import org.nguh.nguhcraft.Utils
import org.nguh.nguhcraft.entity.EntitySpawnManager

class ClientboundSyncSpawnsPacket(
    val Spawns: List<EntitySpawnManager.Spawn>
) : CustomPacketPayload {
    override fun type() = ID
    companion object {
        val ID = Utils.PacketId<ClientboundSyncSpawnsPacket>("clientbound/sync_spawns")
        val CODEC = EntitySpawnManager.Spawn.PACKET_CODEC
            .apply(ByteBufCodecs.list())
            .map(::ClientboundSyncSpawnsPacket, ClientboundSyncSpawnsPacket::Spawns)
    }
}