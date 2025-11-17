package org.nguh.nguhcraft.server

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.network.chat.Component
import org.nguh.nguhcraft.Named
import org.nguh.nguhcraft.Read
import org.nguh.nguhcraft.Write
import java.util.*
import kotlin.jvm.optionals.getOrNull
import kotlin.text.ifEmpty

/**
 * Custom data saved with players.
 *
 * The Discord data is included even in the integrated server because
 * it’s just a lot easier to have only a single codec.
 */
class PlayerData {
    interface Access {
        fun `Nguhcraft$GetPlayerData`(): PlayerData
    }

    /** Whether this player is currently vanished. */
    var Vanished = false

    /** Whether this player is currently a moderator. */
    var IsModerator = false

    /** Whether this player bypasses region protection. */
    var BypassesRegionProtection = false

    /** Whether we should send the console stdout/stderr to this player. */
    var IsSubscribedToConsole = false

    /** Return position for /back. */
    var LastPositionBeforeTeleport: SerialisedTeleportTarget? = null

    /** The homes that this player owns (excluding the 'bed' home) */
    var Homes = mutableListOf<Home>()

    /** The Discord ID of the player. 0 if not linked. */
    var DiscordId: Long = 0

    /** If this player is linked, their Discord role colour, and 0 otherwise. */
    var DiscordColour = 0

    /** If this player is linked, their Discord name. */
    var DiscordName: String = ""

    /** If this player is linked, their Discord avatar. */
    var DiscordAvatar: String = ""

    /**
     * The scoreboard name recorded the last time the player was on the server.
     *
     * There no longer is a way to get a player’s name from their UUID (thanks
     * a lot for that, Mojang), so we have to store it ourselves.
     */
    var LastKnownMinecraftName: String = ""

    /** If this player is linked, their Discord display name. */
    @JvmField var NguhcraftDisplayName: Component? = null

    /** Whether this player is currently muted. */
    var Muted = false

    /** Whether this player is linked. */
    val IsLinked get() = DiscordId != 0L

    /** Serialise. */
    fun Save(WV: ValueOutput) = WV.Write(CODEC, this)

    companion object {
        private val CODEC = RecordCodecBuilder.create {
            it.group(
                Codec.BOOL.optionalFieldOf("Vanished", false).forGetter(PlayerData::Vanished),
                Codec.BOOL.optionalFieldOf("IsModerator", false).forGetter(PlayerData::IsModerator),
                Codec.BOOL.optionalFieldOf("BypassesRegionProtection", false).forGetter(PlayerData::BypassesRegionProtection),
                Codec.BOOL.optionalFieldOf("IsSubscribedToConsole", false).forGetter(PlayerData::IsSubscribedToConsole),
                SerialisedTeleportTarget.CODEC.optionalFieldOf("LastPositionBeforeTeleport").forGetter { Optional.ofNullable(it.LastPositionBeforeTeleport) },
                Home.CODEC.listOf().optionalFieldOf("Homes", listOf()).forGetter(PlayerData::Homes),
                Codec.LONG.optionalFieldOf("DiscordID", 0).forGetter(PlayerData::DiscordId),
                Codec.INT.optionalFieldOf("DiscordRoleColour", 0).forGetter(PlayerData::DiscordColour),
                Codec.STRING.optionalFieldOf("DiscordName", "").forGetter(PlayerData::DiscordName),
                Codec.STRING.optionalFieldOf("DiscordAvatar", "").forGetter(PlayerData::DiscordAvatar),
                Codec.BOOL.optionalFieldOf("Muted", false).forGetter(PlayerData::Muted),
                Codec.STRING.optionalFieldOf("LastKnownMinecraftName", "").forGetter(PlayerData::LastKnownMinecraftName)
            ).apply(it) {
                SV, Mod, Bypass, Console, Back, Homes, Discord, Colour, Name, Avatar, Muted, LastName -> PlayerData().also {
                    it.Vanished = SV
                    it.IsModerator = Mod
                    it.BypassesRegionProtection = Bypass
                    it.IsSubscribedToConsole = Console
                    it.LastPositionBeforeTeleport = Back.getOrNull()
                    it.Homes.addAll(Homes)
                    it.DiscordId = Discord
                    it.DiscordColour = Colour
                    it.DiscordName = Name
                    it.DiscordAvatar = Avatar
                    it.Muted = Muted
                    it.LastKnownMinecraftName = LastName
                }
            }
        }.Named("Nguhcraft")

        @JvmStatic
        fun Load(RV: ValueInput) = RV.Read(CODEC)
    }
}

val ServerPlayer.Data get() = (this as PlayerData.Access).`Nguhcraft$GetPlayerData`()