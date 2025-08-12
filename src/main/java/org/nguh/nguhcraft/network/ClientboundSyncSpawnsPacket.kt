package org.nguh.nguhcraft.network

import net.minecraft.network.codec.PacketCodecs
import net.minecraft.network.packet.CustomPayload
import org.nguh.nguhcraft.Utils
import org.nguh.nguhcraft.entity.EntitySpawnManager

class ClientboundSyncSpawnsPacket(
    val Spawns: List<EntitySpawnManager.Spawn>
) : CustomPayload {
    override fun getId() = ID
    companion object {
        val ID = Utils.PacketId<ClientboundSyncSpawnsPacket>("clientbound/sync_spawns")
        val CODEC = EntitySpawnManager.Spawn.PACKET_CODEC
            .collect(PacketCodecs.toList())
            .xmap(::ClientboundSyncSpawnsPacket, ClientboundSyncSpawnsPacket::Spawns)
    }
}