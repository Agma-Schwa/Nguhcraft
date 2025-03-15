package org.nguh.nguhcraft.server.dedicated

import com.mojang.authlib.GameProfile
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.mojang.logging.LogUtils
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberUpdateEvent
import net.dv8tion.jda.api.events.guild.update.GuildUpdateIconEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.requests.ErrorResponse
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.MemberCachePolicy
import net.dv8tion.jda.api.utils.cache.CacheFlag
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtIo
import net.minecraft.nbt.NbtSizeTracker
import net.minecraft.screen.ScreenTexts
import net.minecraft.server.BannedPlayerEntry
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.dedicated.MinecraftDedicatedServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.ClickEvent
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.WorldSavePath
import net.minecraft.world.GameMode
import org.jetbrains.annotations.Contract
import org.nguh.nguhcraft.Constants
import org.nguh.nguhcraft.Utils
import org.nguh.nguhcraft.Utils.NormaliseNFKCLower
import org.nguh.nguhcraft.mixin.server.MinecraftServerAccessor
import org.nguh.nguhcraft.network.ClientboundChatPacket
import org.nguh.nguhcraft.network.ClientboundLinkUpdatePacket
import org.nguh.nguhcraft.server.Broadcast
import org.nguh.nguhcraft.server.IsVanished
import org.nguh.nguhcraft.server.PlayerByUUID
import org.nguh.nguhcraft.server.accessors.ServerPlayerAccessor
import org.nguh.nguhcraft.server.accessors.ServerPlayerDiscordAccessor
import org.nguh.nguhcraft.server.command.Commands.Exn
import org.nguh.nguhcraft.server.command.Error
import org.nguh.nguhcraft.server.command.Reply
import org.nguh.nguhcraft.server.dedicated.PlayerList.Companion.UpdateCacheEntry
import org.slf4j.Logger
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.PatternSyntaxException

private val ServerPlayerEntity.isLinked get() = (this as ServerPlayerDiscordAccessor).isLinked
private val ServerPlayerEntity.isOperator get() = hasPermissionLevel(4)
private val ServerPlayerEntity.isLinkedOrOperator get() = isLinked || isOperator

private var ServerPlayerEntity.discordId
    get() = (this as ServerPlayerDiscordAccessor).discordId
    set(value) { (this as ServerPlayerDiscordAccessor).discordId = value }
private var ServerPlayerEntity.discordColour
    get() = (this as ServerPlayerDiscordAccessor).discordColour
    set(value) { (this as ServerPlayerDiscordAccessor).discordColour = value }
private var ServerPlayerEntity.discordName: String
    get() = (this as ServerPlayerDiscordAccessor).discordName
    set(value) { (this as ServerPlayerDiscordAccessor).discordName = value }
private var ServerPlayerEntity.discordAvatarURL: String
    get() = (this as ServerPlayerDiscordAccessor).discordAvatarURL
    set(value) { (this as ServerPlayerDiscordAccessor).discordAvatarURL = value }
private var ServerPlayerEntity.discordDisplayName: Text?
    get() = (this as ServerPlayerDiscordAccessor).nguhcraftDisplayName
    set(value) { (this as ServerPlayerDiscordAccessor).nguhcraftDisplayName = value }
private var ServerPlayerEntity.isMuted
    get() = (this as ServerPlayerDiscordAccessor).muted
    set(value) { (this as ServerPlayerDiscordAccessor).muted = value }

private lateinit var Server: MinecraftDedicatedServer

@Environment(EnvType.SERVER)
internal class Discord : ListenerAdapter() {
    override fun onButtonInteraction(E: ButtonInteractionEvent) {
        if (!Ready) return

        // Ignore any interactions that we don’t recognise.
        val ID = E.componentId
        if (!ID.startsWith(BUTTON_ID_LINK)) return

        // Try to retrieve the player we need to link to.
        Server.execute {
            val SP = Server.PlayerByUUID(ID.substring(BUTTON_ID_LINK.length))
            if (SP == null) {
                E.reply("Invalid player!").setEphemeral(true).queue()
                return@execute
            }

            // Sanity check. Overriding the link would not be the end of the
            // world, so we don’t check for that elsewhere, but we might as
            // well check for it here.
            if (SP.isLinked) {
                E.reply("This account is already linked to a Discord account!")
                    .setEphemeral(true).queue()
                return@execute
            }

            // If the account is not a server member, ignore.
            val UUID = SP.uuid
            AgmaSchwaGuild.retrieveMemberById(E.user.idLong).queue({ M ->
                // I don’t think it’s possible to get here w/ 'M' being null,
                // but I’d rather not take any chances in this particular part
                // of the code base...
                if (M == null) {
                    E.reply("You are not a member of the server!").setEphemeral(true).queue()
                    return@queue
                }

                // Last-minute check because race conditions.
                if (!IsAllowedToLink(M)) {
                    E.reply("You must have at least the @nguh-bruh role to link your account!")
                        .setEphemeral(true).queue()
                    return@queue
                }

                // Link the player. Take care to do this in the main thread and re-fetch
                // the player in case they’ve died in the meantime.
                WithPlayer(UUID) { PerformLink(it, M) }
                E.reply("Done!").setEphemeral(true).queue()
            }, { Error ->
                if (Error is ErrorResponseException && Error.errorResponse == ErrorResponse.UNKNOWN_MEMBER) {
                    E.reply("You are not a member of the server!").setEphemeral(true).queue()
                } else {
                    E.reply("Error: ${Error.message}\n\nAre you a member of the Agma Schwa Discord server?")
                        .setEphemeral(true)
                        .queue()
                }
            })
        }
    }

    override fun onGuildMemberRemove(E: GuildMemberRemoveEvent) {
        if (Ready) HandleMemberRemoved(E.user.idLong)
    }

    override fun onGuildMemberUpdate(E: GuildMemberUpdateEvent) {
        val M = E.member
        UpdateLinkedPlayer(M.idLong) { UpdatePlayer(it, M) }
    }

    override fun onGuildUpdateIcon(E: GuildUpdateIconEvent) {
        ServerAvatarURL = when {
            E.newIconUrl != null -> E.newIconUrl!!
            E.oldIconUrl != null -> E.oldIconUrl!!
            else -> Client.selfUser.effectiveAvatarUrl
        }
    }

    override fun onMessageReceived(E: MessageReceivedEvent) {
        // Prevent infinite loops and only consider messages in the designated channel.
        if (!Ready) return
        val M = E.member ?: return
        if (
            E.isWebhookMessage ||
            E.author.isSystem ||
            E.author.isBot ||
            E.channel.idLong != MessageChannel.idLong
        ) return

        val Name = SanitiseForMinecraft(M.effectiveName, "[Invalid Username]")
        val Colour = M.colorRaw
        val Mess = E.message
        val Content = SanitiseForMinecraft(Mess.contentDisplay)
        val HasAttachments = Mess.attachments.isNotEmpty()
        val HasReference = Mess.messageReference != null
        Server.execute {
            val Comp = DISCORD_COMPONENT.copy().append(Text.literal(Name).append(":").withColor(Colour))
            if (HasReference) Comp.append(REPLY_COMPONENT)
            if (HasAttachments) Comp.append(IMAGE_COMPONENT)
            Server.Broadcast(ClientboundChatPacket(Comp, Content, ClientboundChatPacket.MK_PUBLIC))
        }
    }

    companion object {
        const val INVALID_ID: Long = 0

        /**
         * The '[Discord]' [Text] used in chat messages
         */
        private val DISCORD_COMPONENT: Text = Utils.BracketedLiteralComponent("Discord").append(ScreenTexts.SPACE)
        private val REPLY_COMPONENT: Text = ScreenTexts.space().append(Utils.BracketedLiteralComponent("Reply"))
        private val IMAGE_COMPONENT: Text = ScreenTexts.space().append(Utils.BracketedLiteralComponent("Image"))

        private val INTERNAL_ERROR_PLEASE_RELINK: Text = Text
            .literal("Sorry, we couldn’t fetch your account info from Discord. Please relink your account")
            .formatted(Formatting.RED)

        private val ROLE_REQUIREMENTS_NOT_MET = DynamicCommandExceptionType {
            Text.literal("You need to have at least the @$it role on the Discord server to play on this server!")
        }

        private val MUST_ENABLE_DMS : SimpleCommandExceptionType
            = Exn("You must enable DMs from server members to link your account!")

        private val PLEASE_WAIT : SimpleCommandExceptionType
            = Exn("Bot is still starting; please wait a few seconds and try again.")

        private val LOGGER: Logger = LogUtils.getLogger()
        private const val BUTTON_ID_LINK = "ng_lnk:"
        private val DEFAULT_AVATARS = arrayOf(
            "https://cdn.discordapp.com/embed/avatars/0.png",
            "https://cdn.discordapp.com/embed/avatars/1.png",
            "https://cdn.discordapp.com/embed/avatars/2.png",
            "https://cdn.discordapp.com/embed/avatars/3.png",
            "https://cdn.discordapp.com/embed/avatars/4.png",
            "https://cdn.discordapp.com/embed/avatars/5.png",
        )

        private lateinit var Client: JDA
        private lateinit var MessageWebhook: Webhook
        private lateinit var MessageChannel: TextChannel
        private lateinit var AgmaSchwaGuild: Guild
        private lateinit var NguhcrafterRole: Role
        private lateinit var MutedRole: Role
        private lateinit var RequiredRoles: List<Role>

        @Volatile private var ServerAvatarURL: String = DEFAULT_AVATARS[0]
        @Volatile private var Ready = false

        /// Config file format.
        @Serializable
        data class ConfigFile(
            val token: String,
            val guildId: Long,
            val channelId: Long,
            val webhookId: Long,
            val playerRoleId: Long,
            val mutedRoleId: Long,
            val requiredRoleIds: List<Long>
        )

        @JvmStatic
        fun Start(S: MinecraftDedicatedServer) {
            Server = S

            // Load the bot config.
            val Config = Json.decodeFromString<ConfigFile>(File("discord-bot-config.json").readText())
            val Intents = EnumSet.allOf(GatewayIntent::class.java).also { it.add(GatewayIntent.GUILD_MEMBERS) }
            Client = JDABuilder.createDefault(Config.token)
                .setEnabledIntents(Intents)
                .disableCache(CacheFlag.MEMBER_OVERRIDES, CacheFlag.VOICE_STATE, CacheFlag.EMOJI)
                .setMemberCachePolicy(MemberCachePolicy.ONLINE)
                .setActivity(Activity.playing("Minecraft"))
                .setBulkDeleteSplittingEnabled(false)
                .addEventListeners(Discord())
                .build()
            Client.awaitReady()

            fun <T> Get(Text: String, Callback: () -> T) = Callback() ?: throw Exception("Invalid $Text ID.")
            AgmaSchwaGuild = Get("guild") { Client.getGuildById(Config.guildId) }
            MessageChannel = Get("channel") { Client.getTextChannelById(Config.channelId) }
            MessageWebhook = Get("webhook") { Client.retrieveWebhookById(Config.webhookId).complete() }
            NguhcrafterRole = Get("player role") { AgmaSchwaGuild.getRoleById(Config.playerRoleId) }
            MutedRole = Get("muted role") { AgmaSchwaGuild.getRoleById(Config.mutedRoleId) }
            RequiredRoles = Config.requiredRoleIds.map { Get ("required role") { AgmaSchwaGuild.getRoleById(it) } }
            ServerAvatarURL = AgmaSchwaGuild.iconUrl ?: Client.selfUser.effectiveAvatarUrl
            Ready = true
            SendSimpleEmbed(null, "Starting server...", Constants.Lavender)
        }

        @JvmStatic
        fun Stop() {
            if (!Ready) return
            Ready = false
            SendSimpleEmbed(null, "Shutting down...", Constants.Lavender)
            Client.shutdown()
        }

        @JvmStatic
        fun BroadcastAdvancement(SP: ServerPlayerEntity, AdvancementMessage: Text) {
            if (SP.IsVanished) return
            if (!Ready) return
            try {
                val Text = AdvancementMessage.string
                SendSimpleEmbed(SP, Text, Constants.Lavender)
            } catch (E: Exception) {
                E.printStackTrace()
                LOGGER.error("Failed to send advancement message: {}", E.message)
            }
        }

        /** Sync state w/ the client. Called when a client (re)joins. */
        @JvmStatic
        fun BroadcastClientStateOnJoin(SP: ServerPlayerEntity) {
            BroadcastJoinQuitMessage(SP, true)

            // Broadcast this player’s name to everyone. If this player is vanished,
            // we still need to send the packet to them so that they can see their
            // own name properly.
            Vanish.BroadcastIfNotVanished(SP, ClientboundLinkUpdatePacket(SP))

            // Send all other players’ names to this player.
            for (P in Server.playerManager.playerList)
                if (P != SP && !P.IsVanished)
                    ServerPlayNetworking.send(SP, ClientboundLinkUpdatePacket(P))
        }

        @JvmStatic
        fun BroadcastDeathMessage(SP: ServerPlayerEntity, DeathMessage: Text) {
            if (SP.IsVanished) return
            if (!Ready) return

            // Convoluted setup to both resend an abbreviated message if the actual
            // one ends up being too long (Minecraft does this too) and not let an
            // exception escape in any case.
            try {
                val Text = DeathMessage.string
                SendSimpleEmbed(SP, Text, Constants.Red)
            } catch (E: Exception) {
                if (E is IllegalArgumentException || E is ErrorResponseException) {
                    // Death message was too long.
                    val S = DeathMessage.asTruncatedString(256)
                    val Msg = Text.translatable("death.attack.even_more_magic", SP.displayName)
                    val Abbr = Text.translatable("death.attack.message_too_long", Text.literal(S))
                    SendSimpleEmbed(SP, "$Msg\n\n$Abbr", Constants.Black)
                    return
                }

                E.printStackTrace()
                LOGGER.error("Failed to send death message: {}", E.message)
            }
        }

        @JvmStatic
        fun BroadcastJoinQuitMessage(SP: ServerPlayerEntity, Joined: Boolean) {
            if (SP.IsVanished) return
            BroadcastJoinQuitMessageImpl(SP, Joined)
        }

        fun BroadcastJoinQuitMessageImpl(SP: ServerPlayerEntity, Joined: Boolean) {
            if (!Ready) return
            try {
                val Name = if (SP.isLinked) SP.discordName
                else SP.nameForScoreboard
                val Text = "$Name ${if (Joined) "joined" else "left"} the game"
                SendSimpleEmbed(SP, Text, if (Joined) Constants.Green else Constants.Red)
            } catch (E: Exception) {
                E.printStackTrace()
                LOGGER.error("Failed to send join/quit message: {}", E.message)
            }
        }

        private fun BroadcastPlayerUpdate(SP: ServerPlayerEntity) {
            Vanish.BroadcastIfNotVanished(
                SP,
                ClientboundLinkUpdatePacket(
                    SP.uuid,
                    SP.gameProfile.name,
                    SP.discordColour,
                    SP.discordName,
                    SP.isLinked
                )
            )
        }

        /**
        * Check if a player is allowed to join the server.
        *
        * @return `null` If nothing is preventing them from joining on Discord’s end, or
        * a message explaining why they can’t join.
        */
        @JvmStatic
        fun CheckCanJoin(GP: GameProfile): Text? {
            // If we have no record of that player, we can’t map them to
            // a Discord profile, so give up.
            val Entry = PlayerList.Player(GP) ?: return null

            // Likewise, if the player is not linked, give up.
            if (!Entry.isLinked) return null

            // Get the member, if this fails, the rest of the code will just
            // unlink them and put them in adventure mode on join, so there’s
            // no reason to do that here.
            val M = MemberByID(Entry.DiscordID) ?: return null

            // Finally, check if they have the muted role.
            if (M.roles.contains(MutedRole)) return Text.translatable("multiplayer.disconnect.muted")
            return null
        }

        /** Compute the name of a (linked) player. */
        private fun ComputePlayerName(
            IsLinked: Boolean,
            ScoreboardName: String,
            DiscordName: String,
            DiscordColour: Int
        ): Text = if (!IsLinked) Text.literal(ScoreboardName).formatted(Formatting.GRAY)
                  else Text.literal(DiscordName).withColor(DiscordColour)

        /**
         * Discord requires at least one non-combining non-whitespace character in
         * usernames; this returns true if this character makes a username valid.
         */
        private fun CreatesValidDiscordUserName(C: Char): Boolean {
            return when (Character.getType(C).toByte()) {
                Character.UNASSIGNED,
                Character.MODIFIER_LETTER,
                Character.NON_SPACING_MARK,
                Character.COMBINING_SPACING_MARK,
                Character.SPACE_SEPARATOR,
                Character.LINE_SEPARATOR,
                Character.PARAGRAPH_SEPARATOR,
                Character.CONTROL,
                Character.FORMAT,
                Character.PRIVATE_USE,
                Character.SURROGATE,
                Character.CONNECTOR_PUNCTUATION,
                Character.MODIFIER_SYMBOL -> false
                else -> true
            }
        }

        /**
         * Force a member to link with a player, ignoring the
         * usual permission checks.
         */
        fun ForceLink(S: ServerCommandSource, SP: ServerPlayerEntity, ID: Long) {
            if (!Ready) throw PLEASE_WAIT.create()

            // Fetch the member.
            val Member = MemberByID(S, ID) ?: return

            // Warn if they’re already linked and unlink them.
            if (SP.isLinked) {
                S.Reply("Warning: Member ${Member.effectiveName} is already linked to ${SP.nameForScoreboard}. Unlinking...")
                PerformUnlink(SP)
            }

            // Finally, link them. No need for WithMember() here as this
            // happens synchronously during the server tick.
            PerformLink(SP, Member)
        }

        /**
         * Get a default avatar that is dependent on some positive
         * number 'Which'; if the number is too large, it will be
         * reduced to a valid index.
         */
        private fun DefaultAvatarURL(Which: Int): String = DEFAULT_AVATARS[Which % DEFAULT_AVATARS.size]

        fun ForwardChatMessage(SP: ServerPlayerEntity?, Message: String?) {
            try {
                // Message sent by the server.
                if (SP == null) {
                    SendSimpleEmbed(null, Message, Constants.Lavender)
                    return
                }

                // Message sent by a linked user.
                Message(SP, Message)
            } catch (E: Exception) {
                E.printStackTrace()
                LOGGER.error("Failed to forward chat message to Discord: {}", E.message)
            }
        }

        /** Handle a player no longer being on the Discord server. */
        private fun HandleMemberRemoved(Id: Long) {
            // If there is no player online that is linked with this user, then do nothing;
            // here. This code will trigger again when they next join, and we’ll take care
            // of anything that needs to be done then and there.
            UpdateLinkedPlayer(Id) {
                // Retrieve this now since we won’t be able to find the linked player anymore
                // after unlinking them.
                val GP = it.gameProfile

                // Always unlink the player if they’re not on the server anymore.
                PerformUnlink(it)

                // Try to retrieve the ban list entry for this user; if this succeeds,
                // that means they were actually banned (instead of simply leaving or
                // getting kicked); ban them from the Minecraft server as well.
                AgmaSchwaGuild.retrieveBan(UserSnowflake.fromId(Id)).queue({
                    if (Server.playerManager.userBanList.contains(GP)) return@queue
                    Server.playerManager.userBanList.add(
                        BannedPlayerEntry(
                            GP,
                            null,
                            "Discord Integration",
                            null,
                            "Banned from the Agma Schwa Discord server"
                        )
                    )

                    // Kick the player if they’re online.
                    LOGGER.info("Banned player ${GP.name} since they were banned from the Discord server.")
                    val SP = Server.playerManager.getPlayer(GP.id)
                    SP?.networkHandler?.disconnect(Text.translatable("multiplayer.disconnect.banned"))
                }) { e ->
                    // This is racy; the player may have been unbanned again.
                    if (e !is ErrorResponseException || e.errorResponse != ErrorResponse.UNKNOWN_BAN)
                        LOGGER.error("Failed to check ban status for player $Id: ${e.message}")
                    else
                        LOGGER.info("No Discord ban found for user $Id")
                }
            }
        }

        /** Check if a server member is allowed to link their account at all. */
        private fun IsAllowedToLink(M: Member): Boolean =
            M.roles.any { RequiredRoles.contains(it) }

        /** DO NOT USE. */
        fun __IsLinkedOrOperatorImpl(SP: ServerPlayerEntity): Boolean = SP.isLinkedOrOperator

        /** Check if a player is muted. */
        fun IsMuted(SP: ServerPlayerEntity): Boolean = SP.isMuted

        /** Attempt to initiate a link of a player to a member w/ the given ID */
        @Throws(CommandSyntaxException::class)
        fun Link(Source: ServerCommandSource, SP: ServerPlayerEntity, ID: Long) {
            if (!Ready) return
            val Member = MemberByID(Source, ID) ?: return

            // Check if the player is allowed to link their account.
            if (!IsAllowedToLink(Member)) throw ROLE_REQUIREMENTS_NOT_MET.create(RequiredRoles.first().name)

            // Some geniuses may have decided to make it so people can’t DM them;
            // display an error in that case instead of crashing.
            val Name = SP.nameForScoreboard
            val Content = "Press the button below to link your Minecraft account '$Name' to your Discord account."
            try {
                val Ch = Member.user.openPrivateChannel().complete()
                val Mess = Ch.sendMessage(Content)
                    .setActionRow(Button.success(BUTTON_ID_LINK + SP.uuidAsString, "Confirm"))
                    .complete()

                Source.sendMessage(
                    Text.literal(
                        "Follow this link and press the button to link your Minecraft account '"
                        + Name + "' to your Discord account '" + Member.effectiveName + "': "
                    )
                    .withColor(Constants.Green)
                    .append(Utils.LINK)
                    .styled { style -> style.withClickEvent(ClickEvent(ClickEvent.Action.OPEN_URL, Mess.jumpUrl)) })
            } catch (E: ErrorResponseException) {
                if (E.errorResponse == ErrorResponse.CANNOT_SEND_TO_USER) throw MUST_ENABLE_DMS.create()

                // No idea what this is; rethrow.
                throw E
            }
        }

        private fun LinkedPlayerForMember(ID: Long): ServerPlayerEntity?
            = Server.playerManager.playerList.find { it.discordId == ID }

        private fun MemberByID(ID: Long): Member? {
            return try {
                AgmaSchwaGuild.retrieveMemberById(ID).complete()
            } catch (_: ErrorResponseException) {
                null
            }
        }

        private fun MemberByID(Source: ServerCommandSource, ID: Long): Member? {
            try {
                return AgmaSchwaGuild.retrieveMemberById(ID).complete()
            } catch (E: ErrorResponseException) {
                Source.sendError(
                    when (E.errorResponse) {
                        ErrorResponse.UNKNOWN_USER -> Text.literal("Unknown user: $ID")
                        ErrorResponse.UNKNOWN_MEMBER -> Text.literal("Unknown member: $ID")
                        else -> Text.literal("Internal Error: " + E.message)
                    }
                )
                return null
            }
        }

        /**
         * Get the linked member for a player.
         *
         * @param SP the player
         * @return the linked member, or null if the player is not linked or there is an error.
         */
        private fun MemberForPlayer(SP: ServerPlayerEntity): Member? {
            if (!SP.isLinked) return null
            return MemberByID(SP.discordId)
        }

        /**
         * Send a message to [Discord.MessageChannel] using [Discord.MessageWebhook]
         *
         * @param Username  the username to be used for this message
         * @param AvatarURL the avatar to be used for this message
         * @param Content   the message content
         */
        private fun Message(Username: String, AvatarURL: String?, Content: String?) {
            var Avatar = AvatarURL
            if ("" == Avatar) Avatar = null
            MessageWebhook.sendMessage(S(Content))
                .setUsername(SanitiseUsername(Username))
                .setAvatarUrl(Avatar)
                .queue()
        }

        /**
         * Send a message as a player.
         */
        private fun Message(SP: ServerPlayerEntity, Content: String?) {
            if (SP.isLinked) Message(SP.discordName, SP.discordAvatarURL, Content)
            else {
                val N = SP.nameForScoreboard
                Message("$N (unlinked)", DefaultAvatarURL(N.length), Content)
            }
        }

        private fun PerformUnlink(SP: ServerPlayerEntity) {
            val ID = SP.discordId
            UpdatePlayer(SP, INVALID_ID, null, null, Constants.Grey)

            // Remove the @ŋguhcrafter role. This can fail if the user is no
            // longer a server member, so just ignore that error.
            AgmaSchwaGuild.removeRoleFromMember(UserSnowflake.fromId(ID), NguhcrafterRole).queue({}) { e ->
                if (
                    e !is ErrorResponseException ||
                    e.errorResponse != ErrorResponse.UNKNOWN_MEMBER
                ) throw e
            }
        }

        private fun PerformLink(SP: ServerPlayerEntity, M: Member) {
            val DiscordMsg = "${SP.nameForScoreboard} is now linked to ${M.user.name}"
            UpdatePlayer(SP, M)

            // Move the player out of adventure mode.
            if (SP.interactionManager.gameMode == GameMode.ADVENTURE) SP.changeGameMode(GameMode.SURVIVAL)

            // Give them the @ŋguhcrafter role.
            AgmaSchwaGuild.addRoleToMember(M, NguhcrafterRole).queue()

            // Broadcast the change to all players and send a message to Discord.
            SendSimpleEmbed(null, DiscordMsg, Constants.Green)
            Server.playerManager.broadcast(
                Text.empty()
                    .append(Text.literal(SP.nameForScoreboard).formatted(Formatting.AQUA))
                    .append(Text.literal(" is now linked to ").formatted(Formatting.GREEN))
                    .append(Text.literal(M.effectiveName).withColor(M.colorRaw)),
                false
            )
        }

        /**
         * Strip both Discord mentions and Minecraft colour codes.
         *
         * @param s a string
         * @return The string "null" if `s` is `null` and
         * `SanitiseForDiscord(SanitiseForMinecraft(s))` otherwise.
         *
         * @see .SanitiseForDiscord
         * @see .SanitiseForMinecraft
         */
        private fun S(s: String?) = if (s == null) "null" else SanitiseForDiscord(SanitiseForMinecraft(s))!!

        /**
         * Sanitise mentions by inserting a zero-width space after each @ sign in `s`
         *
         * @param s the String to be sanitised
         * @return a copy of `s`, with all mentions sanitised
         */
        @Contract(value = "null -> null; !null -> !null")
        fun SanitiseForDiscord(s: String?) = s?.replace("@", "@\u200B")

        /**
         * Remove all § colour codes from a String.
         *
         * Minecraft already strips those on input when you send a chat message, so this
         * only needs to be used on strings that originate elsewhere, e.g. anything received
         * from Discord.
         *
         * Note that the Discord display name of a member is sanitised on creation, so we
         * don’t need to do that every time we send a chat message.
         *
         * @param S the string
         * @param IfEmpty a string to be returned if the result is empty
         * @return a copy of `S`, with all § colour codes removed
         */
        @Contract(value = "null -> null; !null -> !null")
        fun SanitiseForMinecraft(S: String, IfEmpty: String = ""): String {
            var Name = Formatting.strip(S) ?: ""
            Name = Name.replace("§", "")
            return Name.ifBlank { IfEmpty }
        }

        /**
         * Sanitise a username for use as the author of a webhook.
         */
        private fun SanitiseUsername(Username: String): String {
            val Sanitised = S(Username)
            return if (Sanitised.any { CreatesValidDiscordUserName(it) }) Sanitised.take(80)
            else "[Invalid Username] $Sanitised".take(80)
        }

        /**
         * Send a message containing text and an embed
         *
         * @param Username  the username to be used for this message
         * @param AvatarURL the avatar to be used for this message
         * @param IconURL   the icon to be used for the embed
         * @param Content   the message content
         * @param Colour    the colour of the embed
         */
        private fun SendEmbed(
            Username: String,
            AvatarURL: String?,
            IconURL: String?,
            Content: String?,
            Colour: Int
        ) {
            val E = EmbedBuilder()
                .setAuthor(S(Content), null, IconURL?.takeIf { it.isNotEmpty() })
                .setColor(Colour)
                .build()

            MessageWebhook.sendMessageEmbeds(E)
                .setUsername(SanitiseUsername(Username))
                .setAvatarUrl(AvatarURL?.takeIf { it.isNotEmpty() })
                .queue()
        }

        /**
         * Send an embed to the server. Used for join, quit, death, and link messages.
         *
         * @param SP     the player whose name should be used
         * @param Text   the message content
         * @param Colour the colour of the embed
         */
        private fun SendSimpleEmbed(SP: ServerPlayerEntity?, Text: String?, Colour: Int) {
            SendEmbed(
                "[Server]",
                ServerAvatarURL,
                SP?.discordAvatarURL,
                Text,
                Colour
            )
        }

        fun Unlink(S: ServerCommandSource, SP: ServerPlayerEntity) {
            if (!Ready) return
            PerformUnlink(SP)
            S.sendMessage(Text.literal("Unlinked ")
                .append(SP.name)
                .append(" from Discord.")
                .formatted(Formatting.YELLOW)
            )

            val DiscordMsg = "${SP.nameForScoreboard} is no longer linked"
            SendSimpleEmbed(SP, DiscordMsg, Constants.Red)
        }

        /**
         * Get the player linked to the Discord account with the given ID, and
         * if it exists, queue F to run in the main thread to update the player.
         */
        fun UpdateLinkedPlayer(ID: Long, F: (ServerPlayerEntity) -> Unit) {
            Server.execute {
                val SP = LinkedPlayerForMember(ID) ?: return@execute
                F(SP)
            }
        }

        /**
         * Refresh a player after (un)linking them.
         *
         * This sets a player’s display name and refreshes their commands.
         */
        private fun UpdatePlayer(
            SP: ServerPlayerEntity,
            ID: Long,
            DisplayName: String?,
            AvatarURL: String?,
            NameColour: Int
        ) {
            // Sanity check.
            assert(Server.isOnThread) { "Must link on tick thread" }

            // Only resend the command tree if this has changed, so compute
            // this before we update the id below.
            val LinkStatusChanged = SP.discordId != ID

            // Dew it.
            SP.discordId = ID
            SP.discordName = SanitiseForMinecraft(DisplayName ?: "", "[Invalid Username]")
            SP.discordColour = NameColour
            SP.discordAvatarURL = AvatarURL ?: ""
            UpdatePlayerName(SP)

            // Broadcast this player’s info to everyone.
            if (!SP.isDisconnected) {
                BroadcastPlayerUpdate(SP)

                // The 'link'/'unlink' options are only available if the player is
                // unlinked/linked, so we need to refresh the player’s commands.
                if (LinkStatusChanged) Server.commandManager.sendCommandTree(SP)
            }
        }

        private fun UpdatePlayer(SP: ServerPlayerEntity, M: Member) {
            // Compute whether they’re muted.
            SP.isMuted = M.roles.contains(MutedRole)

            // Update the name etc.
            UpdatePlayer(
                SP,
                M.idLong,
                M.effectiveName,
                M.effectiveAvatarUrl,
                M.colorRaw
            )

            // Kick them if they are now muted.
            if (SP.isMuted) SP.networkHandler.disconnect(Text.translatable("multiplayer.disconnect.muted"))
        }

        /**
         * Re-fetch a linked player’s info from Discord. Called after a player
         * first joins.
         */
        @JvmStatic
        fun UpdatePlayerOnJoin(SP: ServerPlayerEntity) {
            if (!SP.isLinked) {
                BroadcastPlayerUpdate(SP)
                return
            }

            // Retrieve the member to see if they’re still on the server and because
            // their name, avatar, etc. may have changed since we last saw them.
            val UUID = SP.uuid
            AgmaSchwaGuild.retrieveMemberById(SP.discordId).useCache(false).queue({ M ->
                WithPlayer(UUID) { UpdatePlayer(it, M) }
            }, { failure ->
                WithPlayer(UUID) {
                    // Failure likely indicates that the player is no longer on the
                    // server; unlink them just in case and log the error.
                    HandleMemberRemoved(it.discordId)

                    // The call to HandleMemberRemoved() may have just banned this
                    // player, so do nothing here if that’s the case.
                    if (!it.isDisconnected) {
                        it.sendMessage(INTERNAL_ERROR_PLEASE_RELINK)
                        failure.printStackTrace()
                        LOGGER.error("Failed to fetch Discord info for player '${it.name}': ${failure.message}")
                    }
                }
            })
        }

        /** Recompute a player’s name after something has changed. */
        @JvmStatic
        fun UpdatePlayerName(SP: ServerPlayerEntity) {
            // Set by the old paper server; delete it here.
            SP.customName = null
            SP.isCustomNameVisible = false

            // Save this since we’ll be needing it constantly.
            SP.discordDisplayName = ComputePlayerName(
                SP.isLinked,
                SP.nameForScoreboard,
                SP.discordName,
                SP.discordColour
            )

            UpdateCacheEntry(SP)
        }

        /**
         * Get a player by UUID and run some code on them.
         *
         * Use this instead of reusing ServerPlayerEntity objects across
         * callback boundaries as the player entity may have become invalid
         * by the time we get to the callback (the player may have died or
         * left the server in the meantime).
         */
        private fun WithPlayer(Id: UUID, F: (ServerPlayerEntity) -> Unit) {
            Server.execute {
                val SP = Server.playerManager.getPlayer(Id) ?: return@execute
                F(SP)
            }
        }
    }
}

/**
 * Server player list that can also handle offline players.
 *
 * Prefer to use the normal player list unless you absolutely
 * need access to offline player data.
 */
@Environment(EnvType.SERVER)
class PlayerList private constructor(private val ByID: HashMap<UUID, Entry>) : Iterable<PlayerList.Entry> {
    class Entry(
        val ID: UUID,
        val DiscordID: Long,
        val DiscordColour: Int,
        val MinecraftName: String,
        val DiscordName: String
    ) {
        val NormalisedDiscordName: String = NormaliseNFKCLower(DiscordName)
        val isLinked: Boolean get() = DiscordID != Discord.INVALID_ID
        override fun toString() = MinecraftName.ifEmpty { ID.toString() }
    }

    private val Data = ByID.values.toTypedArray()
    init { Data.sortBy { it.toString().lowercase() } }

    /** Find a player by a condition.  */
    fun find(Pred: (Entry) -> Boolean) = Data.find(Pred)

    /** Find a player by UUID.  */
    fun find(ID: UUID) = ByID.getOrDefault(ID, null)

    override fun iterator() = Data.iterator()
    companion object {
        private val LOGGER = LogUtils.getLogger()

        /**
         * Cache for offline player data; this is not used if the player is online.
         *
         * For a UUID:
         *
         *   - If get() returns null, the player has not been cached yet.
         *   - If get() returns NULL_ENTRY, the player has been cached as not found.
         *   - If get() returns a valid entry, the player has been cached as found.
         */
        private val CACHE = ConcurrentHashMap<UUID, Entry?>()

        /** Null entry. */
        private val NULL_ENTRY = Entry(UUID(0, 0), Discord.INVALID_ID, 0, "", "")

        /** Player data directory */
        private val PlayerDataDir get() = (Server as MinecraftServerAccessor)
            .session.getDirectory(WorldSavePath.PLAYERDATA).toFile()

        /** Retrieve Nguhcraft-specific data for all players, even if they’re offline.  */
        fun AllPlayers(): PlayerList {
            val DatFiles = PlayerDataDir.list { _, name -> name.endsWith(".dat") }
            val PlayerData = HashMap<UUID, Entry>()

            // Enumerate all players for which we have an entry, adding them to the list.
            assert(Server.isOnThread) { "Must run on the server thread" }
            for (F in DatFiles) {
                try {
                    val ID = UUID.fromString(F.substring(0, F.length - 4))
                    AddPlayerData(PlayerData, ID)
                }

                // Filename parsing failed. Ignore random garbage in this directory.
                catch (_: IllegalArgumentException) { }
                catch (_: IndexOutOfBoundsException) { }
            }

            // Also add online players that haven’t been saved yet.
            for (P in Server.playerManager.playerList) AddPlayerData(PlayerData, P.uuid)
            return PlayerList(PlayerData)
        }

        /** Retrieve Nguhcraft-specific data for a player that is online. */
        fun Player(SP: ServerPlayerEntity): Entry {
            // Should always be a cache hit if they’re online.
            val Data = CACHE[SP.uuid]
            if (Data != null && Data != NULL_ENTRY) return Data

            // If not, add them to the cache and return the data.
            return UpdateCacheEntry(SP)
        }

        /** Retrieve Nguhcraft-specific data for a game profile. */
        fun Player(GP: GameProfile) = GetPlayerData(GP.id)

        /** Override the cache entry for a player that is online. */
        @JvmStatic
        fun UpdateCacheEntry(SP: ServerPlayerEntity): Entry {
            val NewData = Entry(
                SP.uuid,
                SP.discordId,
                SP.discordColour,
                SP.nameForScoreboard,
                SP.discordName
            )

            CACHE[SP.uuid] = NewData
            return NewData
        }

        /** Add a player’s data to a set, fetching it from wherever appropriate.  */
        private fun AddPlayerData(Map: HashMap<UUID, Entry>, PlayerId: UUID) {
            if (Map.containsKey(PlayerId)) return
            GetPlayerData(PlayerId)?.let { Map[PlayerId] = it }
        }

        private fun GetPlayerData(PlayerId: UUID): Entry? {
            // If we’ve cached their data, use that.
            val Data = CACHE[PlayerId]
            if (Data != null) return if (Data != NULL_ENTRY) Data else null

            // Otherwise, load the player from disk. If this fails, there is nothing we can do.
            val Nbt = ReadPlayerData(PlayerId)
            if (Nbt == null) {
                CACHE[PlayerId] = NULL_ENTRY
                return null
            }

            // There no longer is a way to get a player’s name from their UUID (thanks
            // a lot for that, Mojang), so we have to store it ourselves.
            var Name = ""
            var DiscordName = ""
            var DiscordID = Discord.INVALID_ID
            var RoleColour: Int = Constants.Grey
            if (Nbt.contains(ServerPlayerAccessor.TAG_ROOT)) {
                val Nguhcraft = Nbt.getCompound(ServerPlayerAccessor.TAG_ROOT)
                Name = Nguhcraft.getString(ServerPlayerDiscordAccessor.TAG_LAST_KNOWN_NAME)
                DiscordID = Nguhcraft.getLong(ServerPlayerDiscordAccessor.TAG_DISCORD_ID)
                RoleColour = Nguhcraft.getInt(ServerPlayerDiscordAccessor.TAG_DISCORD_COLOUR)
                DiscordName = Nguhcraft.getString(ServerPlayerDiscordAccessor.TAG_DISCORD_NAME)
            }

            // Once upon a time, this was a paper server; remnants of that should still be
            // in the player data; don’t rely on them for the name, but use them if we don’t
            // have anything else.
            if (Name.isEmpty() && Nbt.contains("bukkit"))
                Name = Nbt.getCompound("bukkit").getString("lastKnownName")

            // Get the rest of the data from the tag and cache it.
            val NewData = Entry(
                PlayerId,
                DiscordID,
                RoleColour,
                Name,
                DiscordName
            )

            // Save the data and add it to the map.
            CACHE[PlayerId] = NewData
            return NewData
        }

        private fun ReadPlayerData(PlayerID: UUID): NbtCompound? {
            val file = File(PlayerDataDir, "$PlayerID.dat")
            if (file.exists() && file.isFile) {
                try {
                    return NbtIo.readCompressed(file.toPath(), NbtSizeTracker.ofUnlimitedBytes())
                } catch (_: Exception) {
                    LOGGER.warn("Failed to load player data for {}", PlayerID)
                }
            }
            return null
        }
    }
}

@Environment(EnvType.SERVER)
object DiscordCommand {
    private val NOT_LINKED: SimpleCommandExceptionType = Exn("Player is not linked to a Discord account!")
    private val ALREADY_LINKED: SimpleCommandExceptionType = Exn("Player is already linked to a Discord account!")
    private val LIST_ENTRY = Text.of("\n  - ")
    private val IS_LINKED_TO = Text.of(" → ")
    private val LPAREN = Text.of(" (")
    private val RPAREN = Text.of(")")
    private val IS_NOT_LINKED: Text = Text.literal("not linked").formatted(Formatting.GRAY)
    private val ERR_EMPTY_FILTER = Text.of("Filter may not be empty!")
    private val ERR_LIST_SYNTAX = Text.of("""
        Syntax:
          (1) /discord list all
          (2) /discord list linked
          (3) /discord list <regex>

        Description:
          (1) List all players, linked or not.
          (2) List only linked players.
          (3) List players whose data (name, id, ...) matches the regex.

        Example:
          /discord list 123

          This lists all players with minecraft name, discord name,
          uuid, or discord id containing '123'.
    """.trimIndent())

    private fun AddPlayer(List: MutableText, PD: PlayerList.Entry) {
        if (PD.isLinked) {
            List.append(LIST_ENTRY)
                .append(Text.literal(PD.toString()).formatted(Formatting.AQUA))
                .append(IS_LINKED_TO)
                .append(Text.literal(PD.DiscordName).withColor(PD.DiscordColour))
                .append(LPAREN)
                .append(Text.literal(PD.DiscordID.toString()).formatted(Formatting.GRAY))
                .append(RPAREN)
        } else {
            List.append(LIST_ENTRY)
                .append(Text.literal(PD.toString()).withColor(Constants.Lavender))
                .append(LPAREN)
                .append(IS_NOT_LINKED)
                .append(RPAREN)
        }
    }

    @Throws(CommandSyntaxException::class)
    fun ForceLink(S: ServerCommandSource, SP: ServerPlayerEntity, ID: Long): Int {
        return Try(S) {
            Discord.ForceLink(S, SP, ID)
            1
        }
    }

    fun ListAllOrLinked(S: ServerCommandSource, All: Boolean): Int {
        val Players = PlayerList.AllPlayers()

        // List all players that are linked.
        val List = Text.literal("Linked players:")
        for (PD in Players) if (All || PD.isLinked) AddPlayer(List, PD)

        // Send the list to the player.
        S.Reply(List)
        return 1
    }

    fun ListPlayers(S: ServerCommandSource, Filter: String): Int {
        try {
            if (Filter.isEmpty()) {
                S.sendError(ERR_EMPTY_FILTER)
                return 0
            }

            // Find all players that match the filter.
            val Pat = Regex(Filter, RegexOption.IGNORE_CASE)
            val Players = PlayerList.AllPlayers().filter { Pat.containsMatchIn(NormaliseNFKCLower(it.toString())) }
            if (Players.isEmpty()) {
                S.Reply("No players matching '$Filter' were found.")
                return 0
            }

            // List all players that match the filter.
            val List = Text.literal("Players: ")
            for (PD in Players) AddPlayer(List, PD)
            S.Reply(List)
            return Players.size
        } catch (E: PatternSyntaxException) {
            S.Error("Invalid regular expression: ${E.message}")
            return 0
        }
    }

    fun ListSyntaxError(S: ServerCommandSource): Int {
        S.sendError(ERR_LIST_SYNTAX)
        return 0
    }

    @Throws(CommandSyntaxException::class)
    private fun Try(S: ServerCommandSource, R: () -> Int): Int {
        try {
            return R()
        } catch (CE: CommandSyntaxException) {
            throw CE
        } catch (E: Exception) {
            val Message = "Failed to send (un)link request: ${E.message}"
            E.printStackTrace()
            S.Error("$Message\nPlease report this to the server administrator.")
            return 0
        }
    }

    @Throws(CommandSyntaxException::class)
    fun TryLink(S: ServerCommandSource, SP: ServerPlayerEntity, ID: Long): Int {
        return Try(S) {
            if (SP.isLinked) throw ALREADY_LINKED.create()
            Discord.Link(S, SP, ID)
            1
        }
    }

    @Throws(CommandSyntaxException::class)
    fun TryUnlink(S: ServerCommandSource, SP: ServerPlayerEntity): Int {
        return Try(S) {
            if (!SP.isLinked) throw NOT_LINKED.create()
            Discord.Unlink(S, SP)
            1
        }
    }
}