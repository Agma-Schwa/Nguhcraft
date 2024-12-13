package org.nguh.nguhcraft.network

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.registry.RegistryKey
import net.minecraft.world.World
import org.nguh.nguhcraft.Utils
import org.nguh.nguhcraft.client.ClientRegion
import org.nguh.nguhcraft.protect.Region
import org.nguh.nguhcraft.server.ServerRegion

/** Packet used to update the ProtectionManager state on the client. */
class ClientboundSyncProtectionMgrPacket(
    val OverworldRegions: Collection<Region>,
    val NetherRegions: Collection<Region>,
    val EndRegions: Collection<Region>
) : CustomPayload {
    override fun getId() = ID

    /** Deserialise the state from a packet. */
    private constructor(B: RegistryByteBuf) : this(
        ReadRegionList(B, World.OVERWORLD),
        ReadRegionList(B, World.NETHER),
        ReadRegionList(B, World.END)
    )

    /** Serialise the state to a packet. */
    private fun Write(B: RegistryByteBuf) {
        WriteRegionList(B, OverworldRegions)
        WriteRegionList(B, NetherRegions)
        WriteRegionList(B, EndRegions)
    }

    /** Write a list of regions to a packet. */
    private fun WriteRegionList(B: RegistryByteBuf, List: Collection<Region>) {
        B.writeInt(List.size)
        List.forEach { (it as ServerRegion).Write(B) }
    }

    companion object {
        val ID = Utils.PacketId<ClientboundSyncProtectionMgrPacket>("clientbound/sync_protection_mgr")
        val CODEC = MakeCodec(ClientboundSyncProtectionMgrPacket::Write, ::ClientboundSyncProtectionMgrPacket)

        /** Read a list of regions from a packet. */
        @Environment(EnvType.CLIENT)
        private fun ReadRegionList(B: RegistryByteBuf, W: RegistryKey<World>): List<Region> {
            val L = mutableListOf<ClientRegion>()
            val Count = B.readInt()
            for (I in 0 until Count) L.add(ClientRegion(B, W))
            return L
        }
    }
}