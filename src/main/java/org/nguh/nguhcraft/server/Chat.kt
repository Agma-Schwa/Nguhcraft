package org.nguh.nguhcraft.server

import com.mojang.logging.LogUtils
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.Context
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayNetworkHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import org.nguh.nguhcraft.Constants
import org.nguh.nguhcraft.Utils
import org.nguh.nguhcraft.network.ClientboundChatPacket
import org.nguh.nguhcraft.server.ServerUtils.IsIntegratedServer
import org.nguh.nguhcraft.server.ServerUtils.IsLinkedOrOperator
import org.nguh.nguhcraft.server.ServerUtils.Multicast
import org.nguh.nguhcraft.server.dedicated.Discord

/** Get a player’s name. */
val ServerPlayerEntity.Name get(): Text = displayName ?: Text.literal(nameForScoreboard).formatted(Formatting.GRAY)

/** This handles everything related to chat and messages */
object Chat {
    private val LOGGER = LogUtils.getLogger()
    private val ERR_NEEDS_LINK_TO_CHAT: Text = Text.literal("You must link your account to send messages in chat or run commands (other than /discord link)").formatted(Formatting.RED)
    private val ERR_MUTED: Text = Text.literal("You are muted and cannot send messages in chat").formatted(Formatting.RED)

    /** Components used in sender names. */
    val SERVER_COMPONENT: Text = Utils.BracketedLiteralComponent("Server")
    private val SRV_LIT_COMPONENT: Text = Text.literal("Server").withColor(Constants.Lavender)
    private val COLON_COMPONENT: Text = Text.literal(":")
    private val COMMA_COMPONENT = Text.literal(", ").withColor(Constants.DeepKoamaru)

    /** Broadcast a command to subscribed operators. */
    private fun BroadcastCommand(
        S: MinecraftServer,
        Source: MutableText,
        Command: String,
        SP: ServerPlayerEntity? = null
    ) {
        Source.append(Text.literal(" issued command\n    /$Command").formatted(Formatting.GRAY))
        S.BroadcastToOperators(Source, SP)
    }

    /** Actually send a message. */
    fun DispatchMessage(S: MinecraftServer, Sender: ServerPlayerEntity?, Message: String) {
        // On the integrated server, don’t bother with the linking.
        if (IsIntegratedServer()) {
            S.Broadcast(ClientboundChatPacket(
                Sender?.Name ?: SERVER_COMPONENT,
                Message,
                ClientboundChatPacket.MK_PUBLIC
            ))
            return
        }

        // On the dedicated server, actually do everything properly.
        val Name = (
            if (Sender == null) SERVER_COMPONENT
            else Text.empty()
                .append(Sender.Name)
                .append(COLON_COMPONENT.copy().withColor(
                    Sender.Data.DiscordColour)
                )
        )

        S.Broadcast(ClientboundChatPacket(Name, Message, ClientboundChatPacket.MK_PUBLIC))
        Discord.ForwardChatMessage(Sender, Message)
    }

    /** Check if a player can send a chat message and issue an error if they can’t. */
    private fun CanPlayerChat(Context: Context): Boolean {
        if (IsIntegratedServer()) return true

        // On the dedicated server, check if the player is linked.
        val SP = Context.player()
        if (!IsLinkedOrOperator(SP)) {
            Context.responseSender().sendPacket(GameMessageS2CPacket(ERR_NEEDS_LINK_TO_CHAT, false))
            return false
        }

        // Or muted.
        if (Discord.IsMuted(SP)) {
            Context.responseSender().sendPacket(GameMessageS2CPacket(ERR_MUTED, false))
            return false
        }

        return true
    }

    /** Log a chat message. */
    fun LogChat(SP: ServerPlayerEntity, Message: String, IsCommand: Boolean) {
        val Linked = IsLinkedOrOperator(SP)
        if (IsCommand) BroadcastCommand(
            SP.Server,
            SP.Name.copy() ?: Text.literal(SP.nameForScoreboard),
            Message,
            SP
        )

        LOGGER.info(
            "[CHAT] {}{}{}: {}{}",
            SP.Name.string,
            if (Linked) " [${SP.nameForScoreboard}]" else "",
            if (IsCommand) " issued command" else " says",
            if (IsCommand) "/" else "",
            Message
        )
    }

    /**
    * Log a command block execution.
    *
    * Rather counterintuitively, it is easier to hook into every place
    * that issues commands rather than into CommandManager::execute
    * directly as by the time we get there, we no longer know where
    * the command originally came from.
    */
    @JvmStatic
    fun LogCommandBlock(S: String, SW: ServerWorld, Pos: BlockPos) {
        val WorldKey = if (SW.registryKey == World.OVERWORLD) "" else "${SW.registryKey.value.path}:"
        BroadcastCommand(SW.server, Text.literal("Command block at $WorldKey[${Pos.toShortString()}]"), S)
        LOGGER.info(
            "[CMD] Command block at {}[{}] issued command /{}",
            WorldKey,
            Pos.toShortString(),
            S
        )
    }

    /** Handle an incoming chat message. */
    fun ProcessChatMessage(Message: String, Context: Context) {
        val SP = Context.player()
        LogChat(SP, Message, false)

        // Unlinked and muted players cannot chat.
        if (!CanPlayerChat(Context)) return

        // Dew it.
        DispatchMessage(Context.server(), SP, Message)
    }

    /** Process an incoming command. */
    fun ProcessCommand(Handler: ServerPlayNetworkHandler, Command: String) {
        val SP = Handler.player
        LogChat(SP, Command, true)

        // An unlinked player can only run /discord link.
        if (!IsLinkedOrOperator(SP) && !Command.startsWith("discord")) {
            Handler.sendPacket(GameMessageS2CPacket(ERR_NEEDS_LINK_TO_CHAT, false))
            return
        }

        // Dew it.
        val S = SP.Server
        val ParsedCommand = S.commandManager.dispatcher.parse(Command, SP.commandSource)
        S.commandManager.execute(ParsedCommand, Command)
    }

    /** Send a private message to players. */
    fun SendPrivateMessage(From: ServerPlayerEntity?, Players: Collection<ServerPlayerEntity>, Message: String) {
        if (From != null && !IsIntegratedServer()) {
            if (Discord.IsMuted(From)) {
                From.sendMessage(ERR_MUTED, false)
                return
            }
        }

        // Send an incoming message to all players in the list.
        val SenderName = From?.Name ?: SRV_LIT_COMPONENT
        Multicast(Players, ClientboundChatPacket(
            SenderName,
            Message,
            ClientboundChatPacket.MK_INCOMING_DM
        ))

        // And the outgoing message back to the sender. We don’t need to log
        // anything if the console is the sender because the command will have
        // already been logged anyway.
        if (From == null) return
        val AllReceivers = Text.empty()
        var First = true
        for (P in Players) {
            if (First) First = false
            else AllReceivers.append(COMMA_COMPONENT)
            AllReceivers.append(P.Name)
        }

        ServerPlayNetworking.send(From, ClientboundChatPacket(
            AllReceivers,
            Message,
            ClientboundChatPacket.MK_OUTGOING_DM
        ))
    }

    // TODO: Use colours when serialising components for the console.

    /** Send a message from the console. */
    @JvmStatic
    fun SendServerMessage(S: MinecraftServer, Message: String) {
        LOGGER.info("[Server] {}", Message)
        DispatchMessage(S, null, Message)
    }
}