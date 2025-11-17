package org.nguh.nguhcraft.server

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.Context
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.entity.player.ChatVisiblity
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket
import net.minecraft.server.network.ServerLoginPacketListenerImpl
import net.minecraft.server.network.ServerGamePacketListenerImpl
import net.minecraft.server.level.ServerPlayer
import net.minecraft.network.chat.Component
import net.minecraft.ChatFormatting
import net.minecraft.util.StringUtil
import org.nguh.nguhcraft.network.ServerboundChatPacket
import org.nguh.nguhcraft.network.VersionCheck

/** This runs on the network thread. */
object ServerNetworkHandler {
    private val ERR_ILLEGAL_CHARS: Component = Component.translatable("multiplayer.disconnect.illegal_characters")
    private val ERR_CHAT_DISABLED: Component = Component.translatable("chat.disabled.options").withStyle(ChatFormatting.RED)
    private val ERR_EMPTY_MESSAGE: Component = Component.literal("Client attempted to send an empty message.")
    private val ERR_EMPTY_COMMAND: Component = Component.literal("Command is empty!").withStyle(ChatFormatting.RED)
    private val ERR_NEEDS_CLIENT_MOD: Component = Component.literal("Sorry, the Nguhcraft client-side mod is required\nto play on the server!")

    @JvmStatic fun HandleChatMessage(Message: String, Context: Context) {
        if (!ValidateIncomingMessage(Message, Context.player(), false)) return
        Context.server().execute { Chat.ProcessChatMessage(Message, Context) }
    }

    @JvmStatic fun HandleCommand(Handler: ServerGamePacketListenerImpl, Command: String) {
        if (!ValidateIncomingMessage(Command, Handler.player, true)) return
        Handler.player.Server.execute { Chat.ProcessCommand(Handler, Command) }
    }

    private fun HandleVersionCheck(
        Handler: ServerLoginPacketListenerImpl,
        Understood: Boolean,
        Buf: FriendlyByteBuf,
    ) {
        // Client doesn’t have the mod.
        if (!Understood) {
            Handler.disconnect(ERR_NEEDS_CLIENT_MOD)
            return
        }

        // Client mod version is out of date.
        val V = Buf.readInt()
        if (V != VersionCheck.NGUHCRAFT_VERSION) {
            Handler.disconnect(Component.literal("""
                Sorry, your Nguhcraft client mod is out of date!
                Yours: $V vs Server: ${VersionCheck.NGUHCRAFT_VERSION}

                Please update it to play on the server.
                If you’re using Prism Launcher, just 
                restart the game.""".trimIndent()
            ))
        }
    }

    fun Init() {
        ServerLoginConnectionEvents.QUERY_START.register(ServerLoginConnectionEvents.QueryStart { _, _, Sender, Syn ->
            Sender.sendPacket(VersionCheck.ID, PacketByteBufs.empty())
        })

        ServerLoginNetworking.registerGlobalReceiver(VersionCheck.ID) lambda@{ _, H, Understood, Buf, _, _ ->
            HandleVersionCheck(H, Understood, Buf)
        }

        ServerPlayNetworking.registerGlobalReceiver(ServerboundChatPacket.ID) { Packet, Context ->
            HandleChatMessage(Packet.Message, Context)
        }
    }

    /** Validate an incoming chat message or command. */
    private fun ValidateIncomingMessage(
        Incoming: String,
        SP: ServerPlayer,
        IsCommand: Boolean
    ): Boolean {
        val S = SP.Server

        // Server is shutting down.
        if (S.isStopped) return false

        // Player has disconnected.
        if (SP.hasDisconnected()) return false

        // Check for illegal characters.
        if (Incoming.any { !StringUtil.isAllowedChatCharacter(it) }) {
            SP.connection.disconnect(ERR_ILLEGAL_CHARS)
            return false
        }

        // Disallow empty messages.
        //
        // This can actually happen if a user just sends '/', which gets processed
        // as an empty command since the '/' is not included in the command text.
        // Don’t kick them in that case.
        if (Incoming.isEmpty()) {
            if (IsCommand) SP.connection.send(ClientboundSystemChatPacket(ERR_EMPTY_COMMAND, false))
            else SP.connection.disconnect(ERR_EMPTY_MESSAGE)
            return false
        }

        // Player has disabled chat.
        if (SP.chatVisibility == ChatVisiblity.HIDDEN) {
            SP.connection.send(ClientboundSystemChatPacket(ERR_CHAT_DISABLED, false))
            return false
        }

        // Ok.
        //
        // Unlinked players are allowed to run exactly one command: /discord link, so
        // we can’t check for that here. Our caller needs to do that.
        SP.resetLastActionTime()
        return true
    }
}