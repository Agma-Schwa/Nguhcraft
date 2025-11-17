package org.nguh.nguhcraft.client

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.network.chat.Component
import net.minecraft.ChatFormatting
import org.nguh.nguhcraft.Constants
import org.nguh.nguhcraft.SyncedGameRule
import org.nguh.nguhcraft.Utils
import org.nguh.nguhcraft.Utils.LBRACK_COMPONENT
import org.nguh.nguhcraft.Utils.RBRACK_COMPONENT
import org.nguh.nguhcraft.client.ClientUtils.Client
import org.nguh.nguhcraft.client.accessors.ClientPlayerListEntryAccessor
import org.nguh.nguhcraft.client.accessors.DisplayData
import org.nguh.nguhcraft.client.render.WorldRendering
import org.nguh.nguhcraft.network.*
import org.nguh.nguhcraft.protect.ProtectionManagerAccess
import java.util.concurrent.CompletableFuture

/** This runs on the network thread. */
@Environment(EnvType.CLIENT)
object ClientNetworkHandler {
    /** Coloured components used in chat messages. */
    private val ARROW_COMPONENT: Component = Component.literal(" → ").withColor(Constants.DeepKoamaru)
    private val ME_COMPONENT = Component.literal("me").withColor(Constants.Lavender)
    private val SPACE_COMPONENT = Component.literal(" ")

    private fun Execute(CB: () -> Unit) = Client().execute(CB)

    /** Incoming chat message. */
    private fun HandleChatPacket(Packet: ClientboundChatPacket) {
        // Render content as Markdown.
        var Message = MarkdownParser.Render(Packet.Content).withStyle(ChatFormatting.WHITE)
        Message = when (Packet.MessageKind) {
            // Player: Message content
            ClientboundChatPacket.MK_PUBLIC -> Packet.PlayerName.copy()
                .append(SPACE_COMPONENT)
                .append(Message)

            // [Player → me] Message content
            ClientboundChatPacket.MK_INCOMING_DM -> LBRACK_COMPONENT.copy()
                .append(Packet.PlayerName)
                .append(ARROW_COMPONENT)
                .append(ME_COMPONENT)
                .append(RBRACK_COMPONENT)
                .append(SPACE_COMPONENT)
                .append(Message)

            // [me → Player] Message content
            ClientboundChatPacket.MK_OUTGOING_DM -> LBRACK_COMPONENT.copy()
                .append(ME_COMPONENT)
                .append(ARROW_COMPONENT)
                .append(Packet.PlayerName)
                .append(RBRACK_COMPONENT)
                .append(SPACE_COMPONENT)
                .append(Message)

            // Should never happen, but render the message anyway.
            else -> Component.literal("<ERROR: Invalid Message Format>\n").withStyle(ChatFormatting.RED)
                .append(Packet.PlayerName)
                .append(SPACE_COMPONENT)
                .append(Message)
        }

        Execute { Client().chatListener.handleSystemMessage(Message, false) }
    }

    /**
    * Notification to update a player’s Discord name.
    *
    * This is only ever sent if we’re connected to a dedicated server.
    */
    private fun HandleLinkUpdatePacket(Packet: ClientboundLinkUpdatePacket) {
        val C = Client()
        val NW = C.connection ?: return
        Execute {
            NW.onlinePlayers.firstOrNull { it.profile.id == Packet.PlayerId }?.let {
                it as ClientPlayerListEntryAccessor
                it.isLinked = Packet.Linked

                // Player is not linked.
                if (!Packet.Linked) {
                    it.nameAboveHead = Component.literal(Packet.MinecraftName)
                    it.tabListDisplayName = Component.literal(Packet.MinecraftName).withStyle(ChatFormatting.GRAY)
                }

                // If the player is linked, the player list shows the Minecraft name as well.
                else {
                    val DiscordName = Component.literal(Packet.DiscordName).withColor(Packet.DiscordColour)
                    it.nameAboveHead = DiscordName
                    it.tabListDisplayName = DiscordName.copy()
                        .append(Component.literal(" "))
                        .append(Utils.BracketedLiteralComponent(Packet.MinecraftName))
                }
            }
        }
    }

    /** Sync display data. */
    private fun HandleSyncDisplayPacket(Packet: ClientboundSyncDisplayPacket) {
        Execute {
            val D = Client().DisplayData ?: return@Execute
            D.Lines = Packet.Lines
        }
    }

    /** Update the game rules. */
    private fun HandleSyncGameRulesPacket(Packet: ClientboundSyncGameRulesPacket) = SyncedGameRule.Update(Packet)

    /** Sync protection bypass state. */
    private fun HandleSyncProtectionBypassPacket(Packet: ClientboundSyncFlagPacket) {
        when (Packet.Flag) {
            ClientFlags.BYPASSES_REGION_PROTECTION -> NguhcraftClient.BypassesRegionProtection = Packet.Value
            ClientFlags.IN_HYPERSHOT_CONTEXT -> NguhcraftClient.InHypershotContext = Packet.Value
            ClientFlags.VANISHED -> NguhcraftClient.Vanished = Packet.Value
        }
    }

    /** Sync protection manager state. */
    private fun HandleSyncProtectionMgrPacket(Packet: ClientboundSyncProtectionMgrPacket) {
        Execute {
            val A = (Client().connection as? ProtectionManagerAccess)
            A?.`Nguhcraft$SetProtectionManager`(ClientProtectionManager(Packet))
        }
    }

    /** Sync spawn positions. */
    private fun HandleSyncSpawnsPacket(Packet: ClientboundSyncSpawnsPacket) {
        WorldRendering.Spawns = Packet.Spawns
    }

    /** Initialise packet handlers. */
    fun Init() {
        ClientLoginNetworking.registerGlobalReceiver(VersionCheck.ID) { _, _, _, _ ->
            CompletableFuture.completedFuture(VersionCheck.Packet)
        }

        Register(ClientboundChatPacket.ID, ::HandleChatPacket)
        Register(ClientboundLinkUpdatePacket.ID, ::HandleLinkUpdatePacket)
        Register(ClientboundSyncGameRulesPacket.ID, ::HandleSyncGameRulesPacket)
        Register(ClientboundSyncFlagPacket.ID, ::HandleSyncProtectionBypassPacket)
        Register(ClientboundSyncProtectionMgrPacket.ID, ::HandleSyncProtectionMgrPacket)
        Register(ClientboundSyncDisplayPacket.ID, ::HandleSyncDisplayPacket)
        Register(ClientboundSyncSpawnsPacket.ID, ::HandleSyncSpawnsPacket)
    }

    /** Register a packet handler. */
    private fun <T : CustomPacketPayload?> Register(ID: CustomPacketPayload.Type<T>, Handler: (T) -> Unit) {
        ClientPlayNetworking.registerGlobalReceiver(ID) { Payload, _ -> Handler(Payload) }
    }
}