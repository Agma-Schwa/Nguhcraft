package org.nguh.nguhcraft.server.dedicated

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
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberUpdateEvent
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateAvatarEvent
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent
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
import org.nguh.nguhcraft.server.ServerUtils
import org.nguh.nguhcraft.server.accessors.ServerPlayerAccessor
import org.nguh.nguhcraft.server.accessors.ServerPlayerDiscordAccessor
import org.nguh.nguhcraft.server.command.Commands
import org.nguh.nguhcraft.server.command.Commands.Exn
import org.nguh.nguhcraft.server.dedicated.PlayerList.Companion.UpdateCacheEntry
import org.nguh.nguhcraft.toUUID
import org.slf4j.Logger
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer
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
private var ServerPlayerEntity.discordName: String?
    get() = (this as ServerPlayerDiscordAccessor).discordName
    set(value) { (this as ServerPlayerDiscordAccessor).discordName = value }
private var ServerPlayerEntity.discordAvatarURL: String?
    get() = (this as ServerPlayerDiscordAccessor).discordAvatarURL
    set(value) { (this as ServerPlayerDiscordAccessor).discordAvatarURL = value }
private var ServerPlayerEntity.discordDisplayName: Text?
    get() = (this as ServerPlayerDiscordAccessor).nguhcraftDisplayName
    set(value) { (this as ServerPlayerDiscordAccessor).nguhcraftDisplayName = value }

private lateinit var Server: MinecraftDedicatedServer

@Environment(EnvType.SERVER)
internal class Discord : ListenerAdapter() {
    override fun onButtonInteraction(E: ButtonInteractionEvent) {
        if (!Ready) return

        // Ignore any interactions that we don’t recognise.
        val ID = E.componentId
        if (!ID.startsWith(BUTTON_ID_LINK)) return

        // Try to retrieve the player we need to link to.
        val SP = Server.PlayerByUUID(ID.substring(BUTTON_ID_LINK.length))
        if (SP == null) E.reply("Invalid player!").setEphemeral(true).queue()

        // Sanity check. Overriding the link would not be the end of the
        // world, so we don’t check for that elsewhere, but we might as
        // well check for it here.
        else if (SP.isLinked) E.reply("This account is already linked to a Discord account!")
            .setEphemeral(true).queue()

        // If the account is not a server member, ignore.
        else AgmaSchwaGuild.retrieveMemberById(E.user.idLong).queue({ M ->
            // I don’t think it’s possible to get here w/ 'M' being null,
            // but I’d rather not take any chances in this particular part
            // of the code base...
            if (M == null) {
                E.reply("You are not a member of the server!").setEphemeral(true).queue()
                return@queue
            }

            // Last-minute check because race conditions.
            if (!IsAllowedToLink(M)) {
                E.reply("You must have at least the @ŋimp role to link your account!")
                    .setEphemeral(true).queue()
                return@queue
            }

            // Link the player. Take care to do this in the main thread.
            Server.execute { PerformLink(SP, M) }
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

    override fun onGuildMemberRemove(E: GuildMemberRemoveEvent) {
        if (Ready) UpdateLinkedPlayer(E.user.idLong) { PerformUnlink(it) }
    }

    override fun onGuildMemberRoleAdd(E: GuildMemberRoleAddEvent) {
        if (Ready) MemberRoleChanged(E.member)
    }

    override fun onGuildMemberRoleRemove(E: GuildMemberRoleRemoveEvent) {
        if (Ready) MemberRoleChanged(E.member)
    }

    override fun onGuildMemberUpdate(E: GuildMemberUpdateEvent) {
        val M = E.member
        UpdateLinkedPlayer(M.idLong) { UpdatePlayer(it, M) }
    }

    override fun onGuildMemberUpdateAvatar(E: GuildMemberUpdateAvatarEvent) {
        if (!Ready) return
        val URL = E.newAvatarUrl
        UpdateLinkedPlayer(E.member.idLong) { it.discordAvatarURL = URL }
    }

    override fun onGuildMemberUpdateNickname(E: GuildMemberUpdateNicknameEvent) {
        if (!Ready) return
        UpdateLinkedPlayer(E.member.idLong) {
            var Name = E.newNickname
            if (Name == null) Name = E.member.effectiveName
            it.discordName = Name
            BroadcastPlayerUpdate(it)
        }
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
        val A = E.author
        if (E.isWebhookMessage || A.isBot || E.channel.idLong != MessageChannel.idLong) return
        val M = E.member
        val Name = A.effectiveName
        val Colour = M?.colorRaw ?: Constants.Grey
        val Mess = E.message
        val Content = Mess.contentDisplay
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
            = Commands.Exn("You must enable DMs from server members to link your account!")

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
            UpdatePlayerName(SP)
            Vanish.BroadcastIfNotVanished(
                SP,
                ClientboundLinkUpdatePacket(
                    SP.uuid,
                    SP.gameProfile.name,
                    SP.discordColour,
                    SP.discordName!!,
                    SP.isLinked
                )
            )
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

        /** Check if a server member is allowed to link their account at all. */
        private fun IsAllowedToLink(M: Member): Boolean =
            M.roles.any { RequiredRoles.contains(it) }

        /** DO NOT USE. */
        fun __IsLinkedOrOperatorImpl(SP: ServerPlayerEntity): Boolean = SP.isLinkedOrOperator

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
            return try {
                AgmaSchwaGuild.retrieveMemberById(SP.discordId).complete()
            } catch (E: ErrorResponseException) {
                null
            }
        }

        private fun MemberRoleChanged(M: Member) {
            val Colour = M.colorRaw
            UpdateLinkedPlayer(M.idLong) { SP: ServerPlayerEntity ->
                if (Colour != SP.discordColour) {
                    Server.execute {
                        SP.discordColour = Colour
                        BroadcastPlayerUpdate(SP)
                    }
                }
            }
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
            if (SP.isLinked) Message(SP.discordName!!, SP.discordAvatarURL, Content)
            else {
                val N = SP.nameForScoreboard
                Message("$N (unlinked)", DefaultAvatarURL(N.length), Content)
            }
        }

        private fun PerformUnlink(SP: ServerPlayerEntity) {
            val ID = SP.discordId
            UpdatePlayer(SP, INVALID_ID, null, null, Constants.Grey)

            // Remove the @ŋguhcrafter role.
            AgmaSchwaGuild.removeRoleFromMember(UserSnowflake.fromId(ID), NguhcrafterRole).queue()
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
         * An abbreviation of `Sanitise(SerialiseLegacyString(s))`
         *
         * @param s a string
         * @return the return value of `Sanitise(SerialiseLegacyString(s))`
         *
         * @see .Sanitise
         * @see .SerialiseLegacyString
         */
        private fun S(s: String?) = if (s == null) "null" else Sanitise(SerialiseLegacyString(s))!!

        /**
         * Sanitise mentions by inserting a zero-width space after each @ sign in `s`
         *
         * @param s the String to be sanitised
         * @return a copy of `s`, with all mentions sanitised
         */
        @Contract(value = "null -> null; !null -> !null")
        fun Sanitise(s: String?) = s?.replace("@", "@\u200B")

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

        /**
         * Remove all § colour codes from a String
         *
         * @param s the string
         * @return a copy of `s`, with all § colour codes removed
         */
        @Contract(value = "null -> null; !null -> !null")
        fun SerialiseLegacyString(s: String?) = s?.replace("§.".toRegex(), "")

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
        fun UpdateLinkedPlayer(ID: Long, F: Consumer<ServerPlayerEntity>) {
            val SP = LinkedPlayerForMember(ID) ?: return
            Server.execute { F.accept(SP) }
        }

        /**
         * Refresh a player after (un)linking them.
         *
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

            // Dew it.
            SP.discordId = ID
            SP.discordName = DisplayName
            SP.discordColour = NameColour
            SP.discordAvatarURL = AvatarURL
            BroadcastPlayerUpdate(SP)

            // The 'link'/'unlink' options are only available if the player is
            // unlinked/linked, so we need to refresh the player’s commands.
            Server.commandManager.sendCommandTree(SP)
        }

        private fun UpdatePlayer(SP: ServerPlayerEntity, M: Member) = UpdatePlayer(
            SP,
            M.idLong,
            M.effectiveName,
            M.effectiveAvatarUrl,
            M.colorRaw
        )

        /**
         * Re-fetch a linked player’s info from Discord. Called after a player
         * first joins.
         */
        @JvmStatic
        fun UpdatePlayerAsync(SP: ServerPlayerEntity) {
            if (!SP.isLinked) {
                BroadcastPlayerUpdate(SP)
                return
            }

            // Retrieve the member to see if they’re still on the server and because
            // their name, avatar, etc. may have changed since we last saw them.
            AgmaSchwaGuild.retrieveMemberById(SP.discordId).useCache(false).queue({ M: Member? ->
                Server.execute {
                    if (M == null) PerformUnlink(SP)
                    else UpdatePlayer(SP, M)
                }
            }, { failure ->
                Server.execute {
                    // Failure likely indicates that the player is no longer on the
                    // server; unlink them just in case and log the error.
                    PerformUnlink(SP)
                    SP.sendMessage(INTERNAL_ERROR_PLEASE_RELINK)
                    failure.printStackTrace()
                    LOGGER.error("Failed to fetch Discord info for player '${SP.name}': ${failure.message}")
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
                SP.discordName ?: "",
                SP.discordColour
            )

            UpdateCacheEntry(SP)
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
            for (F in DatFiles!!) {
                try {
                    val ID = UUID.fromString(F.substring(0, F.length - 4))
                    AddPlayerData(PlayerData, ID)
                }

                // Filename parsing failed. Ignore random garbage in this directory.
                catch (ignored: IllegalArgumentException) { }
                catch (ignored: IndexOutOfBoundsException) { }
            }

            // Also add online players that haven’t been saved yet.
            for (P in Server.playerManager.playerList) AddPlayerData(PlayerData, P.uuid)
            return PlayerList(PlayerData)
        }

        /** Retrieve Nguhcraft-specific data for a player that is online.  */
        fun Player(SP: ServerPlayerEntity): Entry {
            // Should always be a cache hit if they’re online.
            val Data = CACHE[SP.uuid]
            if (Data != null && Data != NULL_ENTRY) return Data

            // If not, add them to the cache and return the data.
            return UpdateCacheEntry(SP)
        }

        /** Override the cache entry for a player that is online.  */
        @JvmStatic
        fun UpdateCacheEntry(SP: ServerPlayerEntity): Entry {
            val NewData = Entry(
                SP.uuid,
                SP.discordId,
                SP.discordColour,
                SP.nameForScoreboard,
                SP.discordName ?: ""
            )

            CACHE[SP.uuid] = NewData
            return NewData
        }

        /** Add a player’s data to a set, fetching it from wherever appropriate.  */
        private fun AddPlayerData(Map: HashMap<UUID, Entry>, PlayerID: UUID) {
            if (Map.containsKey(PlayerID)) return

            // If we’ve cached their data, use that.
            val Data = CACHE[PlayerID]
            if (Data != null) {
                if (Data != NULL_ENTRY) Map[PlayerID] = Data
                return
            }

            // Otherwise, load the player from disk. If this fails, there is nothing we can do.
            val Nbt = ReadPlayerData(PlayerID)
            if (Nbt == null) {
                CACHE[PlayerID] = NULL_ENTRY
                return
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
                PlayerID,
                DiscordID,
                RoleColour,
                Name,
                DiscordName
            )

            // Save the data and add it to the map.
            CACHE[PlayerID] = NewData
            Map[PlayerID] = NewData
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
    private val LIST_ENTRY = Text.literal("\n  - ")
    private val IS_LINKED_TO = Text.literal(" → ")
    private val LPAREN = Text.literal(" (")
    private val RPAREN = Text.literal(")")
    private val ERR_EMPTY_FILTER = Text.literal("Filter may not be empty!")
    private val ERR_EMPTY_QUERY = Text.literal("Query must not be empty!")
    private val ERR_LIST_SYNTAX = Text.literal("Syntax: /discord list (all|linked|<regex>)")

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
        }
    }

    fun ListAllOrLinked(S: ServerCommandSource, All: Boolean): Int {
        val Players = PlayerList.AllPlayers()

        // List all players that are linked.
        val List = Text.literal("Linked players:")
        for (PD in Players) if (All || PD.isLinked) AddPlayer(List, PD)

        // Send the list to the player.
        S.sendMessage(List.formatted(Formatting.YELLOW))
        return 1
    }

    fun ListPlayers(S: ServerCommandSource, Filter: String): Int {
        try {
            if (Filter.isEmpty()) {
                S.sendError(ERR_EMPTY_FILTER)
                return 0
            }

            val Pat = Regex(Filter, RegexOption.IGNORE_CASE)

            // Get ALL players, not just online ones.
            val Players = PlayerList.AllPlayers()

            // List all players that match the condition.
            val List = Text.literal("Players: ")
            for (PD in Players) {
                if (!Pat.containsMatchIn(PD.toString())) continue
                AddPlayer(List, PD)
            }

            // Send the list to the player.
            S.sendMessage(List.formatted(Formatting.YELLOW))
            return 1
        } catch (E: PatternSyntaxException) {
            S.sendError(Text.literal("Invalid regular expression: '${E.message}'"))
            return 0
        }
    }

    fun ListSyntaxError(S: ServerCommandSource): Int {
        S.sendError(ERR_LIST_SYNTAX)
        return 0
    }

    fun QueryMemberInfo(S: ServerCommandSource, Message: String): Int {
        val M = Message.trim()
        val Players = PlayerList.AllPlayers()

        // Message must not be empty.
        if (M.isEmpty()) {
            S.sendError(ERR_EMPTY_QUERY)
            return 0
        }

        // We iterate separately each time to correctly handle the case of e.g. someone
        // setting their name to someone else’s ID, in which case we still return the
        // member whose *ID* matches the query, if there is one.
        val Data = M.toLongOrNull()?.let { ID -> Players.find { it.DiscordID == ID }  }
            ?: M.toUUID()?.let { Players.find(it) }
            ?: NormaliseNFKCLower(M).let { Norm -> Players.find { it.NormalisedDiscordName == Norm } }
            ?: Players.find { M.equals(it.MinecraftName, ignoreCase = true) }

        // No player found.
        if (Data == null) {
            S.sendError(Text.literal("No player found for query: $Message"))
            return 0
        }

        // We found a player.
        return ShowLinkInfoForPlayer(S, Data)
    }

    fun ShowLinkInfoForPlayer(S: ServerCommandSource, SP: ServerPlayerEntity)
            = ShowLinkInfoForPlayer(S, PlayerList.Player(SP), SP.discordAvatarURL)

    fun ShowLinkInfoForPlayer(S: ServerCommandSource, PD: PlayerList.Entry, AvatarURL: String? = null): Int {
        if (!PD.isLinked) {
            S.sendMessage(Text.literal("Player '$PD' is not linked to a Discord account."))
            return 1
        }

        val Msg = Text.literal("""
                Player '$PD' is linked to ID ${PD.DiscordID}
                  Discord Name: ${PD.DiscordName}
                  Name Colour:  
                """.trimIndent()
        ).append(Text.literal("#${PD.DiscordColour.toString(16)}").withColor(PD.DiscordColour))
        if (AvatarURL != null) Msg.append("\n  Avatar URL:   $AvatarURL")
        S.sendMessage(Msg)
        return 1
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
            S.sendError(Text.literal("$Message\nPlease report this to the server administrator."))
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