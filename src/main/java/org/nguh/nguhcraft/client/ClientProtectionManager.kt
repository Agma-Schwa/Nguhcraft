package org.nguh.nguhcraft.client

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.player.LocalPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.world.level.Level
import org.nguh.nguhcraft.network.ClientboundSyncProtectionMgrPacket
import org.nguh.nguhcraft.protect.ProtectionManager

@Environment(EnvType.CLIENT)
class ClientProtectionManager(
    Packet: ClientboundSyncProtectionMgrPacket
) : ProtectionManager(
    Packet.Regions
) {
    override fun _BypassesRegionProtection(PE: Player) =
        if (PE is LocalPlayer) NguhcraftClient.BypassesRegionProtection
        else false

    override fun IsLinked(PE: Player) = true
    override fun ReadData(RV: ValueInput) = throw UnsupportedOperationException()
    override fun WriteData(WV: ValueOutput) = throw UnsupportedOperationException()

    companion object {
        /** Empty manager used on the client to ensure it’s never null. */
        @JvmField val EMPTY = ClientProtectionManager(ClientboundSyncProtectionMgrPacket(mapOf(
            Level.OVERWORLD to listOf(),
            Level.NETHER to listOf(),
            Level.END to listOf(),
        )))
    }
}