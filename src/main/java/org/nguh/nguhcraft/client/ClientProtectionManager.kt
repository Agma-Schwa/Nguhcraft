package org.nguh.nguhcraft.client

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.nbt.NbtElement
import net.minecraft.world.World
import org.nguh.nguhcraft.network.ClientboundSyncProtectionMgrPacket
import org.nguh.nguhcraft.protect.ProtectionManager

@Environment(EnvType.CLIENT)
class ClientProtectionManager(
    Packet: ClientboundSyncProtectionMgrPacket
) : ProtectionManager(
    Packet.Regions
) {
    override fun _BypassesRegionProtection(PE: PlayerEntity) =
        if (PE is ClientPlayerEntity) NguhcraftClient.BypassesRegionProtection
        else false

    override fun IsLinked(PE: PlayerEntity) = true
    override fun ReadData(Tag: NbtElement) = throw UnsupportedOperationException()
    override fun WriteData() = throw UnsupportedOperationException()

    companion object {
        /** Empty manager used on the client to ensure it’s never null. */
        @JvmField val EMPTY = ClientProtectionManager(ClientboundSyncProtectionMgrPacket(mapOf(
            World.OVERWORLD to listOf(),
            World.NETHER to listOf(),
            World.END to listOf(),
        )))
    }
}