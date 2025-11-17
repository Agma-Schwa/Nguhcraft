package org.nguh.nguhcraft.server.dedicated

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerCommonPacketListenerImpl
import net.minecraft.server.network.ServerGamePacketListenerImpl
import net.minecraft.server.level.ServerPlayer
import net.minecraft.network.chat.Component
import net.minecraft.ChatFormatting
import org.nguh.nguhcraft.network.ClientFlags
import org.nguh.nguhcraft.server.Broadcast
import org.nguh.nguhcraft.server.Data
import org.nguh.nguhcraft.server.Name
import org.nguh.nguhcraft.server.Server
import org.nguh.nguhcraft.server.SetClientFlag

/**
* Prevents other players from seeing a player.
*
* Note that this is only guaranteed to work properly if two
* conditions are met: First, the vanished player is an operator,
* and second, the vanished player is in creative or spectator
* mode.
*/
@Environment(EnvType.SERVER)
object Vanish {
    /**
    * If this player is not vanished, broadcast it; otherwise, send
    * it to the player itself only.
    *
    * This is a common pattern here because we want to e.g. send player
    * name updates to everyone if the player is shown, and if they’re
    * vanished, we still need to send the name update to that player.
    *
    * We don’t do this for things like join or quit messages though
    * because those aren’t important enough to warrant that; this is
    * mainly for packets that are necessary for gameplay.
    */
    @JvmStatic
    fun BroadcastIfNotVanished(SP: ServerPlayer, Packet: CustomPacketPayload) {
        if (SP.Data.Vanished) ServerPlayNetworking.send(SP, Packet)
        else SP.Server.Broadcast(Packet)
    }

    @JvmStatic
    fun BroadcastIfNotVanished(SP: ServerPlayer, Packet: Packet<*>) {
        if (SP.Data.Vanished) SP.connection.send(Packet)
        else SP.Server.Broadcast(Packet)
    }

    @JvmStatic
    fun FixPlayerListPacket(
        S: MinecraftServer,
        H: ServerCommonPacketListenerImpl,
        PLP: ClientboundPlayerInfoUpdatePacket
    ): ClientboundPlayerInfoUpdatePacket {
        // I don’t think this should happen, but there is not much we can do
        // here if we have no way to get the player from the network handler.
        if (H !is ServerGamePacketListenerImpl) return PLP

        // Filter out vanished players.
        val l = PLP.entries().filter {
            // Always send ourselves.
            //
            // Note that this packet may get send before a player is added to the
            // player manager’s list of all players, so we need to grab the player
            // object from its network handler instead.
            if (H.player.uuid == it.profileId) return@filter true

            // If we can’t get the profile here, then something probably went wrong
            // so send the player anyway.
            val SP = S.playerList.getPlayer(it.profileId)
            SP == null || !SP.Data.Vanished
        }

        // If that had no effect, keep the same packet.
        if (l.size == PLP.entries().size) return PLP

        // Otherwise, build a new one. We can’t simply change the entries in
        // the original packet since the same packet may be broadcast to multiple
        // players, and the resulting packet may have to be different for each
        // player.
        return ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(
            l.map { S.playerList.getPlayer(it.profileId) }
        )
    }

    @JvmStatic
    fun IsVanished(SP: ServerPlayer): Boolean = SP.Data.Vanished

    fun Toggle(SP: ServerPlayer) {
        // Toggle this first since some of the code below depends on it
        // having the correct value.
        SP.Data.Vanished = !SP.Data.Vanished

        // And then broadcast the appropriate packets.
        if (!SP.Data.Vanished) ShowPlayer(SP) else HidePlayer(SP)
        SP.SetClientFlag(ClientFlags.VANISHED, SP.Data.Vanished)
    }

    private fun HidePlayer(SP: ServerPlayer) {
        val P = ClientboundPlayerInfoRemovePacket(listOf(SP.uuid))

        // Detach any vehicles, otherwise, we might get into weird situations
        // where a client receives packets for a player that is to them no longer
        // on the server.
        SP.unRide()

        // Broadcast the packet to all players.
        SP.Server.Broadcast(SP, P)

        // As well as a fake quit message.
        SP.Server.Broadcast(Component.translatable("multiplayer.player.left", SP.Name).withStyle(ChatFormatting.YELLOW))
        Discord.BroadcastJoinQuitMessageImpl(SP, false)
    }

    private fun ShowPlayer(SP: ServerPlayer) {
        val P = ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(listOf(SP))

        // Broadcast the packet to all players.
        SP.Server.Broadcast(SP, P)

        // Also re-send this player’s Discord name to everyone in case this
        // player joined while vanished (or if a player joined while they
        // were vanished).
        //
        // This step also sends a join message to Discord.
        Discord.BroadcastClientStateOnJoin(SP)

        // Also send out a fake join message.
        SP.Server.Broadcast(Component.translatable("multiplayer.player.joined", SP.Name).withStyle(ChatFormatting.YELLOW))
    }
}