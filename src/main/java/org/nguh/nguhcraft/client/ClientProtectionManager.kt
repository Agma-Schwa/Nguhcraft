package org.nguh.nguhcraft.client

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.RegistryByteBuf
import net.minecraft.registry.RegistryKey
import net.minecraft.world.World
import org.nguh.nguhcraft.network.ClientboundSyncProtectionMgrPacket
import org.nguh.nguhcraft.protect.ProtectionManager
import org.nguh.nguhcraft.protect.Region

@Environment(EnvType.CLIENT)
class ClientRegion(B: RegistryByteBuf, W: RegistryKey<World>): Region(
    Name = B.readString(),
    World = W,
    FromX = B.readInt(),
    FromZ = B.readInt(),
    ToX = B.readInt(),
    ToZ = B.readInt()
) {
    init {
        RegionFlags = B.readLong()
        ColourOverride = when (val c = B.readInt()) {
            COLOUR_OVERRIDE_NONE_ENC -> null
            else -> c
        }
    }
}

@Environment(EnvType.CLIENT)
class ClientProtectionManager(
    Packet: ClientboundSyncProtectionMgrPacket
) : ProtectionManager(
    OverworldRegions = Packet.OverworldRegions,
    NetherRegions = Packet.NetherRegions,
    EndRegions = Packet.EndRegions
) {
    override fun _BypassesRegionProtection(PE: PlayerEntity) =
        if (PE is ClientPlayerEntity) NguhcraftClient.BypassesRegionProtection
        else false

    override fun IsLinked(PE: PlayerEntity) = true

    companion object {
        /** Empty manager used on the client to ensure it’s never null. */
        @JvmField val EMPTY = ClientProtectionManager(ClientboundSyncProtectionMgrPacket(
            listOf(),
            listOf(),
            listOf()
        ))
    }
}