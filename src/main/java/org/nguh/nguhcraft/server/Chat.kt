package org.nguh.nguhcraft.server

import com.mojang.logging.LogUtils
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.Context
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket
import net.minecraft.network.chat.CommonComponents
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerGamePacketListenerImpl
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.level.ServerLevel
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Component
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import org.nguh.nguhcraft.Constants
import org.nguh.nguhcraft.Utils
import org.nguh.nguhcraft.network.ClientboundChatPacket
import org.nguh.nguhcraft.server.ServerUtils.IsIntegratedServer
import org.nguh.nguhcraft.server.ServerUtils.IsLinkedOrOperator
import org.nguh.nguhcraft.server.ServerUtils.Multicast
import org.nguh.nguhcraft.server.dedicated.Discord

/** Get a player’s name. */
val ServerPlayer.Name get(): Component = displayName ?: Component.literal(scoreboardName).withStyle(ChatFormatting.GRAY)

/** This handles everything related to chat and messages */
object Chat {
    private val LOGGER = LogUtils.getLogger()
    private val ERR_NEEDS_LINK_TO_CHAT: Component = Component.literal("You must link your account to send messages in chat or run commands (other than /discord link)").withStyle(ChatFormatting.RED)
    private val ERR_MUTED: Component = Component.literal("You are muted and cannot send messages in chat").withStyle(ChatFormatting.RED)

    /** Components used in sender names. */
    val SERVER_COMPONENT: Component = Utils.BracketedLiteralComponent("Server")
    val RCON_COMPONENT: Component = Utils.BracketedLiteralComponent("RCon")
    private val SRV_LIT_COMPONENT: Component = Component.literal("Server").withColor(Constants.Lavender)
    private val COLON_COMPONENT: Component = Component.literal(":")
    private val COMMA_COMPONENT = Component.literal(", ").withColor(Constants.DeepKoamaru)
    private val DISCORD_COMPONENT: Component = Utils.BracketedLiteralComponent("Discord").append(CommonComponents.SPACE)
    private val REPLY_COMPONENT: Component = CommonComponents.space().append(Utils.BracketedLiteralComponent("Reply"))
    private val IMAGE_COMPONENT: Component = CommonComponents.space().append(Utils.BracketedLiteralComponent("Image"))


    /** Broadcast a command to subscribed operators. */
    private fun BroadcastCommand(
        S: MinecraftServer,
        Source: MutableComponent,
        Command: String,
        SP: ServerPlayer? = null
    ) {
        Source.append(Component.literal(" issued command\n    /$Command").withStyle(ChatFormatting.GRAY))
        S.BroadcastToOperators(Source, SP)
    }

    /** Actually send a message. */
    fun DispatchMessage(S: MinecraftServer, Sender: ServerPlayer?, Message: String) {
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
            else Component.empty()
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
            Context.responseSender().sendPacket(ClientboundSystemChatPacket(ERR_NEEDS_LINK_TO_CHAT, false))
            return false
        }

        // Or muted.
        if (Discord.IsMuted(SP)) {
            Context.responseSender().sendPacket(ClientboundSystemChatPacket(ERR_MUTED, false))
            return false
        }

        return true
    }

    /** Log a chat message. */
    fun LogChat(SP: ServerPlayer, Message: String, IsCommand: Boolean) {
        val Linked = IsLinkedOrOperator(SP)
        if (IsCommand) BroadcastCommand(
            SP.Server,
            SP.Name.copy() ?: Component.literal(SP.scoreboardName),
            Message,
            SP
        )

        LOGGER.info(
            "[CHAT] {}{}{}: {}{}",
            SP.Name.string,
            if (Linked) " [${SP.scoreboardName}]" else "",
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
    fun LogCommandBlock(S: String, SW: ServerLevel, Pos: BlockPos) {
        val WorldKey = if (SW.dimension() == Level.OVERWORLD) "" else "${SW.dimension().location().path}:"
        BroadcastCommand(SW.server, Component.literal("Command block at $WorldKey[${Pos.toShortString()}]"), S)
        LOGGER.info(
            "[CMD] Command block at {}[{}] issued command /{}",
            WorldKey,
            Pos.toShortString(),
            S
        )
    }

    /** Log RCon command execution. */
    @JvmStatic
    fun LogRConCommand(S: MinecraftServer, Command: String) {
        BroadcastCommand(S, Component.literal("Remote console"), Command)
        LOGGER.info(
            "[RCon] Remote console issued command /{}",
            if (Command.startsWith('/')) Command.substring(1) else Command
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
    fun ProcessCommand(Handler: ServerGamePacketListenerImpl, Command: String) {
        val SP = Handler.player
        LogChat(SP, Command, true)

        // An unlinked player can only run /discord link.
        if (!IsLinkedOrOperator(SP) && !Command.startsWith("discord")) {
            Handler.send(ClientboundSystemChatPacket(ERR_NEEDS_LINK_TO_CHAT, false))
            return
        }

        // Dew it.
        val S = SP.Server
        val ParsedCommand = S.commands.dispatcher.parse(Command, SP.createCommandSourceStack())
        S.commands.performCommand(ParsedCommand, Command)
    }

    /** Process a message sent from Discord. */
    fun ProcessDiscordMessage(
        S: MinecraftServer,
        Content: String,
        MemberName: String,
        Colour: Int,
        HasReference: Boolean,
        HasAttachments: Boolean
    ) {
        val Comp = DISCORD_COMPONENT.copy().append(Component.literal(MemberName).append(":").withColor(Colour))
        if (HasReference) Comp.append(REPLY_COMPONENT)
        if (HasAttachments) Comp.append(IMAGE_COMPONENT)
        S.Broadcast(ClientboundChatPacket(Comp, Content, ClientboundChatPacket.MK_PUBLIC))
        LOGGER.info(
            "[Discord] {} says: {}{}{}",
            MemberName,
            if (HasReference) "[Reply] " else "",
            if (HasAttachments) "[Image] " else "",
            Content,
        )
    }

    /** Send a private message to players. */
    fun SendPrivateMessage(From: ServerPlayer?, Players: Collection<ServerPlayer>, Message: String) {
        if (From != null && !IsIntegratedServer()) {
            if (Discord.IsMuted(From)) {
                From.displayClientMessage(ERR_MUTED, false)
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
        val AllReceivers = Component.empty()
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