package org.nguh.nguhcraft.server.command

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.minecraft.client.multiplayer.ClientSuggestionProvider
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.resources.ResourceKey
import net.minecraft.core.registries.Registries
import net.minecraft.commands.CommandSourceStack
import net.minecraft.server.level.ServerPlayer
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.Level
import org.nguh.nguhcraft.client.ClientUtils.Client
import org.nguh.nguhcraft.event.NguhMobType
import org.nguh.nguhcraft.protect.ProtectionManager
import org.nguh.nguhcraft.server.Data
import org.nguh.nguhcraft.server.DisplayHandle
import org.nguh.nguhcraft.server.DisplayManager
import org.nguh.nguhcraft.server.Home
import org.nguh.nguhcraft.server.MCBASIC
import org.nguh.nguhcraft.server.PlayerByName
import org.nguh.nguhcraft.server.ProcedureManager
import org.nguh.nguhcraft.server.Server
import org.nguh.nguhcraft.server.ServerRegion
import org.nguh.nguhcraft.server.WarpManager
import org.nguh.nguhcraft.server.command.Commands.Exn
import java.util.concurrent.CompletableFuture

fun StringReader.ReadUntilWhitespace(): String {
    val Start = cursor
    while (canRead() && !Character.isWhitespace(peek())) skip()
    return string.substring(Start, cursor)
}

class DisplayArgumentType : ArgumentType<String> {
    override fun parse(R: StringReader) =  R.readString()
    companion object {
        private val NO_SUCH_DISPLAY = DynamicCommandExceptionType { Component.literal("No such display: '$it'") }

        fun Display() = ProcedureArgumentType()

        fun Resolve(Ctx: CommandContext<CommandSourceStack>, ArgName: String): DisplayHandle {
            val Name = Ctx.getArgument(ArgName, String::class.java)
            return Ctx.source.server.DisplayManager.GetExisting(Name) ?: throw NO_SUCH_DISPLAY.create(Name)
        }

        fun Suggest(
            Ctx: CommandContext<CommandSourceStack>,
            SB: SuggestionsBuilder
        ): CompletableFuture<Suggestions> {
            val Displays = Ctx.source.server.DisplayManager.GetExistingDisplayNames()
            return SharedSuggestionProvider.suggest(Displays, SB)
        }
    }
}

class HomeArgumentType : ArgumentType<String> {
    override fun parse(R: StringReader) = R.ReadUntilWhitespace()
    companion object {
        private val NO_SUCH_HOME = DynamicCommandExceptionType { Component.literal("No such home: $it") }
        private val NO_SUCH_PLAYER = DynamicCommandExceptionType { Component.literal("No such player: $it") }
        private val INVALID_HOME_NAME = Exn("Home names may not contain ':'!")

        /**
        * Helper to handle other players’ homes.
        *
        * This splits the 'RawName' into a player name and a home name,
        * if an explicit player name is given. Otherwise, the original
        * player and the home name are returned.
        *
        * If the player is not an operator, an error is raised if this
        * would access another player’s home.
        */
        fun MapOrThrow(SP: ServerPlayer, RawName: String): Pair<ServerPlayer, String> {
            if (":" !in RawName) return SP to RawName
            if (!SP.hasPermissions(4)) throw INVALID_HOME_NAME.create()
            val (Player, Home) = RawName.split(":", limit = 2)
            val Other = SP.Server.PlayerByName(Player) ?: throw NO_SUCH_PLAYER.create(Player)
            return Other to Home
        }

        fun Resolve(Ctx: CommandContext<CommandSourceStack>, ArgName: String): Home {
            val S = Ctx.source
            val (SP, Name) = MapOrThrow(S.playerOrException, Ctx.getArgument(ArgName, String::class.java))
            if (Name == Home.BED_HOME) return Home.Bed(SP)
            val Homes = SP.Data.Homes
            return Homes.find { it.Name == Name } ?: throw NO_SUCH_HOME.create(Name)
        }

        fun Suggest(
            Ctx: CommandContext<CommandSourceStack>,
            SB: SuggestionsBuilder
        ): CompletableFuture<Suggestions> {
            val S = Ctx.source
            val SP = S.playerOrException
            val Homes = SP.Data.Homes
            val Names = Homes.map { it.Name }.toMutableList()
            Names.add(Home.BED_HOME)

            // If the player is an operator, and the argument so far contains
            // a colon, also try to search for the homes of another player, and
            // suggest all players otherwise.
            if (SP.hasPermissions(4)) {
                if (":" in SB.remaining) {
                    S.server.PlayerByName(SB.remaining.split(":")[0])?.let {
                        val OtherHomes = SP.Data.Homes
                        Names.addAll(OtherHomes.map { H -> "${it.name.string}:${H.Name}" })
                    }
                } else {
                    for (P in S.server.playerList.players)
                        Names.add("${P.name.string}:")
                }
            }

            return SharedSuggestionProvider.suggest(Names, SB)
        }

        fun Home() = HomeArgumentType()
    }
}

class MobArgumentType : ArgumentType<String> {
    override fun parse(R: StringReader) =  R.ReadUntilWhitespace()

    override fun <S> listSuggestions(
        Ctx: CommandContext<S>,
        SB: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        return SharedSuggestionProvider.suggest(LOWERCASE_KEYS, SB)
    }

    companion object {
        private val UNKNOWN_KEY = DynamicCommandExceptionType { Component.literal("Unknown mob type: '${it}'") }
        private val LOWERCASE_MAP = NguhMobType.entries.associateBy { it.name.lowercase() }
        private val LOWERCASE_KEYS = LOWERCASE_MAP.keys

        fun Mob() = MobArgumentType()
        fun Resolve(Ctx: CommandContext<CommandSourceStack>, ArgName: String): NguhMobType {
            val Name = Ctx.getArgument(ArgName, String::class.java)
            return LOWERCASE_MAP[Name] ?: throw UNKNOWN_KEY.create(Name)
        }
    }
}

class ProcedureArgumentType : ArgumentType<String> {
    override fun parse(R: StringReader) =  R.ReadUntilWhitespace()
    companion object {
        private val NO_SUCH_PROCEDURE = DynamicCommandExceptionType { Component.literal("No such procedure: '$it'") }

        fun Procedure() = ProcedureArgumentType()

        fun Resolve(Ctx: CommandContext<CommandSourceStack>, ArgName: String): MCBASIC.Procedure {
            val Name = Ctx.getArgument(ArgName, String::class.java)
            return Ctx.source.server.ProcedureManager.GetExisting(Name) ?: throw NO_SUCH_PROCEDURE.create(Name)
        }

        fun Suggest(
            Ctx: CommandContext<CommandSourceStack>,
            SB: SuggestionsBuilder
        ): CompletableFuture<Suggestions> {
            val Procedures = Ctx.source.server.ProcedureManager.Procedures.map { it.Name }
            return SharedSuggestionProvider.suggest(Procedures, SB)
        }
    }
}

class RegionArgumentType : ArgumentType<String> {
    override fun parse(R: StringReader) =  R.ReadUntilWhitespace()

    // We can do this client-side since we have the regions on the client.
    override fun <S> listSuggestions(
        Ctx: CommandContext<S>,
        SB: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        val S = Ctx.source
        if (S !is ClientSuggestionProvider) return Suggestions.empty()
        val P = ProtectionManager.Get(Client().level!!)

        // If the input contains a ':', filter by the world
        // to the left of it.
        if (":" in SB.remaining) {
            val (W, _) = SB.remaining.split(":", limit = 2)
            val Key = ResourceKey.create(Registries.DIMENSION, ResourceLocation.withDefaultNamespace(W))
            val Regions = P.TryGetRegions(Key) ?: return Suggestions.empty()
            return SharedSuggestionProvider.suggest(Regions.map { "${Key.location().path}:${it.Name}" }, SB)
        }

        // Otherwise, suggest all regions in the current world if there is one.
        val Regions = mutableListOf<String>()
        val W = Client().level
        if (W != null) Regions.addAll(ProtectionManager.GetRegions(W).map { it.Name })

        // And add the registry keys for any worlds that contain regions.
        fun AddWorldKey(W: ResourceKey<Level>) {
            if (P.TryGetRegions(W)!!.isNotEmpty())
                Regions.add("${W.location().path}:")
        }

        AddWorldKey(Level.OVERWORLD)
        AddWorldKey(Level.NETHER)
        AddWorldKey(Level.END)
        return SharedSuggestionProvider.suggest(Regions, SB)
    }

    companion object {
        private val NO_SUCH_WORLD = DynamicCommandExceptionType { Component.literal("No such world: $it") }
        private val NO_SUCH_REGION = Dynamic2CommandExceptionType { R, W -> Component.literal(
            "No region '$R' in world ${(W as Level).dimension().location().path}"
        ) }

        fun Region() = RegionArgumentType()

        fun Resolve(Ctx: CommandContext<CommandSourceStack>, ArgName: String): ServerRegion {
            var Name = Ctx.getArgument(ArgName, String::class.java)
            val S = Ctx.source
            var W = S.level
            if (":" in Name) {
                val (WorldKey, RegionName) = Name.split(":", limit = 2)
                val Key = ResourceKey.create(Registries.DIMENSION, ResourceLocation.withDefaultNamespace(WorldKey))
                W = S.server.getLevel(Key) ?: throw NO_SUCH_WORLD.create(Key.location().path)
                Name = RegionName
            }

            return ProtectionManager.GetRegion(W, Name) as? ServerRegion ?: throw NO_SUCH_REGION.create(Name, W)
        }
    }
}

class WarpArgumentType : ArgumentType<String> {
    override fun parse(R: StringReader) = R.readUnquotedString()
    companion object {
        private val NO_SUCH_WARP = DynamicCommandExceptionType { Component.literal("No such warp: $it") }

        fun Resolve(Ctx: CommandContext<CommandSourceStack>, Name: String): WarpManager.Warp {
            val WarpName = Ctx.getArgument(Name, String::class.java)
            return Ctx.source.server.WarpManager.Warps[WarpName] ?: throw NO_SUCH_WARP.create(WarpName)
        }

        fun Suggest(
            Ctx: CommandContext<CommandSourceStack>,
            SB: SuggestionsBuilder
        ): CompletableFuture<Suggestions> = SharedSuggestionProvider.suggest(
            Ctx.source.server.WarpManager.Warps.keys,
            SB
        )

        fun Warp() = WarpArgumentType()
    }
}