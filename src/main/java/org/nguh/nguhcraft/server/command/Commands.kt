package org.nguh.nguhcraft.server.command

import com.mojang.brigadier.arguments.*
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.command.CommandRegistryAccess
import net.minecraft.command.argument.*
import net.minecraft.command.argument.serialize.ConstantArgumentSerializer
import net.minecraft.component.DataComponentTypes
import net.minecraft.enchantment.Enchantment
import net.minecraft.entity.Entity
import net.minecraft.entity.LightningEntity
import net.minecraft.item.ItemStack
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.MutableText
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ColumnPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import net.minecraft.world.Heightmap
import net.minecraft.world.World
import net.minecraft.world.chunk.ChunkStatus
import org.nguh.nguhcraft.Constants
import org.nguh.nguhcraft.server.MCBASIC
import org.nguh.nguhcraft.Nguhcraft.Companion.Id
import org.nguh.nguhcraft.SyncedGameRule
import org.nguh.nguhcraft.item.KeyItem
import org.nguh.nguhcraft.network.ClientFlags
import org.nguh.nguhcraft.protect.ProtectionManager
import org.nguh.nguhcraft.protect.Region
import org.nguh.nguhcraft.protect.TeleportResult
import org.nguh.nguhcraft.server.*
import org.nguh.nguhcraft.server.ServerUtils.IsIntegratedServer
import org.nguh.nguhcraft.server.ServerUtils.StrikeLightning
import org.nguh.nguhcraft.server.accessors.ServerPlayerAccessor
import org.nguh.nguhcraft.server.accessors.ServerPlayerDiscordAccessor
import org.nguh.nguhcraft.server.dedicated.Vanish

fun ServerCommandSource.Error(Msg: String?) = sendError(Text.of(Msg))

// Used when a command causes a general change, or for status updates.
fun ServerCommandSource.Reply(Msg: String) = Reply(Text.literal(Msg))
fun ServerCommandSource.Reply(Msg: Text) = Reply(Text.empty().append(Msg))
fun ServerCommandSource.Reply(Msg: MutableText) = sendMessage(Msg.formatted(Formatting.YELLOW))

// Used when a command results in the addition or creation of something.
fun ServerCommandSource.Success(Msg: String) = Success(Text.literal(Msg))
fun ServerCommandSource.Success(Msg: Text) = Success(Text.empty().append(Msg))
fun ServerCommandSource.Success(Msg: MutableText) = sendMessage(Msg.formatted(Formatting.GREEN))

val ServerCommandSource.HasModeratorPermissions: Boolean get() =
    hasPermissionLevel(4) || (isExecutedByPlayer && playerOrThrow.IsModerator)

fun ReplyMsg(Msg: String): Text = Text.literal(Msg).formatted(Formatting.YELLOW)

object Commands {
    inline fun <reified T : ArgumentType<*>> ArgType(Key: String, noinline Func: () -> T) {
        ArgumentTypeRegistry.registerArgumentType(
            Id(Key),
            T::class.java,
            ConstantArgumentSerializer.of(Func)
        )
    }

    const val OPERATOR_PERMISSION_LEVEL = 4

    fun Register() {
        CommandRegistrationCallback.EVENT.register { D, A, E ->
            if (E.dedicated) {
                D.register(DiscordCommand())          // /discord
                D.register(ModCommand())              // /mod
                D.register(VanishCommand())           // /vanish
            }

            D.register(BackCommand())                 // /back
            D.register(BypassCommand())               // /bypass
            D.register(DelHomeCommand())              // /delhome
            D.register(DiscardCommand())              // /discard
            D.register(DisplayCommand())              // /display
            D.register(EnchantCommand(A))             // /enchant
            D.register(EntityCountCommand())          // /entity_count
            D.register(FixCommand())                  // /fix
            D.register(HomeCommand())                 // /home
            D.register(HomesCommand())                // /homes
            D.register(KeyCommand())                  // /key
            val Msg = D.register(MessageCommand())    // /msg
            D.register(ObliterateCommand())           // /obliterate
            D.register(ProcedureCommand())            // /procedure
            D.register(RegionCommand())               // /region
            D.register(RenameCommand(A))              // /rename
            D.register(RuleCommand())                 // /rule
            D.register(SayCommand())                  // /say
            D.register(SetHomeCommand())              // /sethome
            D.register(SmiteCommand())                // /smite
            D.register(SpeedCommand())                // /speed
            D.register(SubscribeToConsoleCommand())   // /subscribe_to_console
            D.register(literal("tell").redirect(Msg)) // /tell
            D.register(TopCommand())                  // /top
            D.register(UUIDCommand())                 // /uuid
            D.register(literal("w").redirect(Msg))    // /w
            D.register(WarpCommand())                 // /warp
            D.register(WarpsCommand())                // /warps
            D.register(WildCommand())                 // /wild
        }

        ArgType("display", DisplayArgumentType::Display)
        ArgType("home", HomeArgumentType::Home)
        ArgType("procedure", ProcedureArgumentType::Procedure)
        ArgType("region", RegionArgumentType::Region)
        ArgType("warp", WarpArgumentType::Warp)
    }

    fun Exn(message: String): SimpleCommandExceptionType {
        return SimpleCommandExceptionType(Text.literal(message))
    }

    // =========================================================================
    //  Command Implementations
    // =========================================================================
    object BackCommand {
        private val ERR_NO_TARGET = Exn("No saved target to teleport back to!")

        fun Teleport(SP: ServerPlayerEntity): Int {
            val Pos = (SP as ServerPlayerAccessor).lastPositionBeforeTeleport
            if (Pos == null) throw ERR_NO_TARGET.create()
            SP.Teleport(Pos, true)
            return 1
        }
    }

    object BypassCommand {
        private val BYPASSING = ReplyMsg("Now bypassing region protection.")
        private val NOT_BYPASSING = ReplyMsg("No longer bypassing region protection.")

        fun Toggle(S: ServerCommandSource, SP: ServerPlayerEntity): Int {
            val A = SP as ServerPlayerAccessor
            val NewState = !A.bypassesRegionProtection
            A.bypassesRegionProtection = NewState
            SP.SetClientFlag(ClientFlags.BYPASSES_REGION_PROTECTION, NewState)
            S.sendMessage(if (NewState) BYPASSING else NOT_BYPASSING)
            return 1
        }
    }

    object DiscardCommand {
        private val REASON = ReplyMsg("Player entity was discarded")

        fun Execute(S: ServerCommandSource, Entities: Collection<Entity>): Int {
            for (E in Entities) {
                // Discard normal entities.
                if (E !is ServerPlayerEntity) E.discard()

                // Disconnect players instead of discarding them, but do
                // not disconnect ourselves in single player.
                else if (!IsIntegratedServer()) E.networkHandler.disconnect(REASON)
            }

            S.Reply("Discarded ${Entities.size} entities")
            return Entities.size
        }
    }

    object DisplayCommand {
        fun Clear(S: ServerCommandSource, Players: Collection<ServerPlayerEntity>): Int {
            for (SP in Players) S.server.DisplayManager.SetActiveDisplay(SP, null)
            return Players.size
        }

        fun List(S: ServerCommandSource, D: DisplayHandle): Int {
            S.sendMessage(D.Listing())
            return 1
        }

        fun ListAll(S: ServerCommandSource): Int {
            S.Reply(S.server.DisplayManager.ListAll())
            return 0
        }

        fun SetDisplay(S: ServerCommandSource, Players: Collection<ServerPlayerEntity>, D: DisplayHandle): Int {
            for (SP in Players) S.server.DisplayManager.SetActiveDisplay(SP, D)
            return Players.size
        }
    }

    object EnchantCommand {
        private val ERR_NO_ITEM = Exn("You must be holding an item to enchant it!")

        fun Enchant(
            S: ServerCommandSource,
            SP: ServerPlayerEntity,
            E: RegistryEntry<Enchantment>,
            Lvl: Int
        ): Int {
            // This *does* work for books. Fabric’s documentation says otherwise,
            // but it’s simply incorrect about that.
            val ItemStack = SP.mainHandStack
            if (ItemStack.isEmpty) throw ERR_NO_ITEM.create()
            ItemStack.addEnchantment(E, Lvl)
            S.Success(
                Text.translatable(
                    "commands.enchant.success.single", *arrayOf<Any>(
                        Enchantment.getName(E, Lvl),
                        SP.displayName!!,
                    )
                )
            )
            return 1
        }
    }

    object FixCommand {
        private val FIXED_ONE = ReplyMsg("Fixed item in hand")
        private val FIXED_ALL = ReplyMsg("Fixed all items in inventory")

        fun Fix(S: ServerCommandSource, SP: ServerPlayerEntity): Int {
            FixStack(SP.mainHandStack)
            S.sendMessage(FIXED_ONE)
            return 1
        }

        fun FixAll(S: ServerCommandSource, SP: ServerPlayerEntity): Int {
            for (St in SP.inventory.main) FixStack(St)
            for (St in SP.inventory.armor) FixStack(St)
            FixStack(SP.inventory.offHand[0])
            S.sendMessage(FIXED_ALL)
            return 1
        }

        private fun FixStack(St: ItemStack) {
            if (St.isEmpty) return
            St.remove(DataComponentTypes.LORE)
            St.remove(DataComponentTypes.HIDE_TOOLTIP)
        }
    }

    object HomeCommand {
        private val CANT_TOUCH_THIS = Exn("The 'bed' home is special and cannot be deleted or set!")
        private val CANT_ENTER = DynamicCommandExceptionType { Text.literal("The home '$it' is in a region that restricts teleporting!") }
        private val CANT_LEAVE = DynamicCommandExceptionType { Text.literal("Teleporting out of this region is not allowed!") }
        private val CANT_SETHOME_HERE = Exn("Cannot /sethome here as this region restricts teleporting!")
        private val CONNOR_MACLEOD = Exn("You may only have one home!")
        private val NO_HOMES = ReplyMsg("No homes defined!")

        fun Delete(S: ServerCommandSource, SP: ServerPlayerEntity, H: Home): Int {
            if (H.Name == Home.BED_HOME) throw CANT_TOUCH_THIS.create()
            (SP as ServerPlayerAccessor).Homes().remove(H)
            S.Reply(Text.literal("Deleted home ").append(Text.literal(H.Name).formatted(Formatting.AQUA)))
            return 1
        }

        fun DeleteDefault(S: ServerCommandSource, SP: ServerPlayerEntity): Int {
            val H = (SP as ServerPlayerAccessor).Homes().find { it.Name == Home.DEFAULT_HOME }?: return 0
            return Delete(S, SP, H)
        }

        private fun FormatHome(H: Home): Text =
            Text.literal("\n  - ")
                .append(Text.literal(H.Name).formatted(Formatting.AQUA))
                .append(" in ")
                .append(Text.literal(H.World.value.path.toString()).withColor(Constants.Lavender))
                .append(" at [")
                .append(Text.literal("${H.Pos.x}").formatted(Formatting.GRAY))
                .append(", ")
                .append(Text.literal("${H.Pos.y}").formatted(Formatting.GRAY))
                .append(", ")
                .append(Text.literal("${H.Pos.z}").formatted(Formatting.GRAY))
                .append("]")

        fun List(S: ServerCommandSource, SP: ServerPlayerEntity): Int {
            val Homes = (SP as ServerPlayerAccessor).Homes()
            if (Homes.isEmpty()) {
                S.sendMessage(NO_HOMES)
                return 0
            }

            val List = Text.literal("Homes:")
            List.append(FormatHome(Home.Bed(SP)))
            for (H in Homes) List.append(FormatHome(H))
            S.Reply(List)
            return 1
        }

        fun Set(S: ServerCommandSource, SP: ServerPlayerEntity, RawName: String): Int {
            val (TargetPlayer, Name) = HomeArgumentType.MapOrThrow(SP, RawName)
            if (Name == Home.BED_HOME) throw CANT_TOUCH_THIS.create()
            val Homes = (TargetPlayer as ServerPlayerAccessor).Homes()

            // If this region doesn’t allow entry by teleport, then setting a home here makes
            // no sense as we can’t use it, which might not be obvious to people.
            //
            // Note that we pass in 'SP', not 'TargetPlayer' as the player whose permissions to
            // check here; this allows admins to set someone else’s home in a restricted region if
            // need be.
            if (!ProtectionManager.AllowTeleportToFromAnywhere(SP, SP.world, SP.blockPos))
                throw CANT_SETHOME_HERE.create()

            // Remove the home *after* the check above to ensure we only remove it if we’re about
            // to add a new home to the list.
            Homes.removeIf { it.Name == Name }

            // Check that either there are no other homes or this player can have more than one home.
            if (!TargetPlayer.hasPermissionLevel(4) && Homes.isNotEmpty())
                throw CONNOR_MACLEOD.create()

            // If yes, add it.
            Homes.add(Home(Name, SP.world.registryKey, SP.blockPos))
            S.Success(Text.literal("Set home ").append(Text.literal(Name).formatted(Formatting.AQUA)))
            return 1
        }

        fun Teleport(SP: ServerPlayerEntity, H: Home): Int {
            val World = SP.server.getWorld(H.World)!!
            when (ProtectionManager.GetTeleportResult(SP, World, H.Pos)) {
                TeleportResult.ENTRY_DISALLOWED -> throw CANT_ENTER.create(H.Name)
                TeleportResult.EXIT_DISALLOWED -> throw CANT_LEAVE.create(H.Name)
                else -> {}
            }

            SP.Teleport(World, H.Pos, true)
            return 1
        }

        fun TeleportToDefault(SP: ServerPlayerEntity): Int {
            val H = (SP as ServerPlayerAccessor).Homes().firstOrNull() ?: Home.Bed(SP)
            return Teleport(SP, H)
        }
    }

    object KeyCommand {
        private val ERR_EMPTY = Text.of("Key may not be empty!")

        fun Generate(S: ServerCommandSource, SP: ServerPlayerEntity, Key: String): Int {
            if (Key.isEmpty()) {
                S.sendError(ERR_EMPTY)
                return 0
            }

            SP.inventory.insertStack(KeyItem.Create(Key))
            SP.currentScreenHandler.sendContentUpdates()
            S.Success(Text.literal("Generated key ").append(Text.literal(Key).formatted(Formatting.LIGHT_PURPLE)))
            return 1
        }
    }

    object ProcedureCommand {
        private val PROC_EMPTY = ReplyMsg("Procedure is already empty")
        private val NO_PROCEDURES = ReplyMsg("No procedures defined")
        private val INVALID_LINE_NUMBER = Exn("Line number is out of bounds!")

        fun Append(S: ServerCommandSource, Proc: MCBASIC.Procedure, Text: String) =
            InsertLine(S, Proc, Proc.LineCount(), Text)

        fun Call(S: ServerCommandSource, Proc: MCBASIC.Procedure): Int {
            try {
                Proc.ExecuteAndThrow(S)
            } catch (E: Exception) {
                S.sendError(Text.literal("Failed to execute procedure ").append(Text.literal(Proc.Name).formatted(Formatting.GOLD)))
                S.Error(E.message)
                E.printStackTrace()
                return 0
            }

            return 1
        }

        fun Clear(S: ServerCommandSource, Proc: MCBASIC.Procedure): Int {
            if (Proc.IsEmpty()) {
                S.sendMessage(PROC_EMPTY)
                return 0
            }

            Proc.Clear()
            S.Reply(Text.literal("Cleared procedure ").append(Text.literal(Proc.Name).formatted(Formatting.GOLD)))
            return 1
        }

        fun Create(S: ServerCommandSource, Name: String): Int {
            if (S.server.ProcedureManager.GetExisting(Name) != null) {
                S.Reply(Text.literal("Procedure ")
                    .append(Text.literal(Name).formatted(Formatting.GOLD))
                    .append(" already exists!")
                )
                return 0
            }

            try {
                S.server.ProcedureManager.GetOrCreate(Name)
                S.Success(Text.literal("Created procedure ").append(Text.literal(Name).formatted(Formatting.GOLD)))
                return 1
            } catch (E: IllegalArgumentException) {
                S.sendError(
                    Text.literal("Failed to create procedure ")
                    .append(Text.literal(Name).formatted(Formatting.GOLD))
                    .append(": ${E.message}")
                )
                return 0
            }
        }

        fun DeleteLine(S: ServerCommandSource, Proc: MCBASIC.Procedure, Line: Int, Until: Int? = null): Int {
            if (Line >= Proc.LineCount()) throw INVALID_LINE_NUMBER.create()
            if (Until != null && Until >= Proc.LineCount()) throw INVALID_LINE_NUMBER.create()
            Proc.Delete(Line..(Until ?: Line))
            S.Reply("Removed command at index $Line")
            return 1
        }

        fun DeleteProcedure(S: ServerCommandSource, Proc: MCBASIC.Procedure): Int {
            if (Proc.Managed) {
                S.sendError(Text.literal("Cannot delete managed procedure ").append(Text.literal(Proc.Name).formatted(Formatting.GOLD)))
                return 0
            }

            S.server.ProcedureManager.Delete(Proc)
            S.Reply(Text.literal("Deleted procedure ").append(Text.literal(Proc.Name).formatted(Formatting.GOLD)))
            return 1
        }

        fun InsertLine(S: ServerCommandSource, Proc: MCBASIC.Procedure, Line: Int, Code: String): Int {
            if (Line > Proc.LineCount() || Line < 0) throw INVALID_LINE_NUMBER.create() // '>', not '>='!
            Proc.Insert(Line, Code)
            S.Success(Text.literal("Added command at index $Line"))
            return 1
        }

        fun List(S: ServerCommandSource): Int {
            val Procs = S.server.ProcedureManager.Procedures
            if (Procs.isEmpty()) {
                S.sendMessage(NO_PROCEDURES)
                return 0
            }

            val List = Text.literal("Procedures:")
            for (P in Procs) List.append(Text.literal("\n  - ").append(Text.literal(P.Name).formatted(Formatting.GOLD)))
            S.Reply(List)
            return Procs.size
        }

        fun Listing(S: ServerCommandSource, Proc: MCBASIC.Procedure): Int {
            val Msg = Text.literal("Procedure ").append(Text.literal(Proc.Name).formatted(Formatting.GOLD)).append(":\n")
            Proc.Listing(Msg)
            S.Reply(Msg)
            return 1
        }

        fun SetLine(S: ServerCommandSource, Proc: MCBASIC.Procedure, Line: Int, Code: String): Int {
            if (Line >= Proc.LineCount()) throw INVALID_LINE_NUMBER.create()
            Proc[Line] = Code
            S.Reply("Set command at index $Line")
            return 1
        }

        fun Source(S: ServerCommandSource, Proc: MCBASIC.Procedure): Int {
            val Msg = Text.literal("Procedure ").append(Text.literal(Proc.Name).formatted(Formatting.GOLD)).append(":\n")
            Proc.DisplaySource(Msg, 0)
            S.Reply(Msg)
            return 1
        }
    }

    object RegionCommand {
        private val NOT_IN_ANY_REGION = Text.of("You are not in any region!")
        private val CANNOT_CREATE_EMPTY = Text.of("Refusing to create empty region!")

        fun AddRegion(S: ServerCommandSource, W: World, Name: String, From: ColumnPos, To: ColumnPos): Int {
            if (From == To) {
                S.sendError(CANNOT_CREATE_EMPTY)
                return 0
            }

            try {
                val R = ServerRegion(
                    S.server,
                    Name,
                    W.registryKey,
                    FromX = From.x,
                    FromZ = From.z,
                    ToX = To.x,
                    ToZ = To.z
                )

                S.server.ProtectionManager.AddRegion(S.server, R)
                S.Success(Text.literal("Created region ")
                    .append(Text.literal(Name).formatted(Formatting.AQUA))
                    .append(" in world ")
                    .append(Text.literal(W.registryKey.value.path.toString()).withColor(Constants.Lavender))
                    .append(" with bounds [")
                    .append(Text.literal("${R.MinX}").formatted(Formatting.GRAY))
                    .append(", ")
                    .append(Text.literal("${R.MinZ}").formatted(Formatting.GRAY))
                    .append("] → [")
                    .append(Text.literal("${R.MaxX}").formatted(Formatting.GRAY))
                    .append(", ")
                    .append(Text.literal("${R.MaxZ}").formatted(Formatting.GRAY))
                    .append("]")
                )
                return 1
            } catch (E: MalformedRegionException) {
                S.sendError(E.Msg)
                return 0
            }
        }

        fun DeleteRegion(S: ServerCommandSource, R: ServerRegion): Int {
            if (!S.server.ProtectionManager.DeleteRegion(S.server, R)) {
                S.sendError(R.AppendWorldAndName(Text.literal("No such region: ")))
                return 0
            }

            S.sendMessage(
                R.AppendWorldAndName(Text.literal("Deleted region "))
                .formatted(Formatting.GREEN)
            )
            return 1
        }

        fun ListAllRegions(S: ServerCommandSource): Int {
            ListRegions(S, S.server.overworld)
            ListRegions(S, S.server.getWorld(World.NETHER)!!)
            ListRegions(S, S.server.getWorld(World.END)!!)
            return 3
        }

        fun ListRegions(S: ServerCommandSource, W: World): Int {
            val Regions = ProtectionManager.GetRegions(W)
            if (Regions.isEmpty()) {
                S.Reply(Text.literal("No regions defined in world ")
                    .append(Text.literal(W.registryKey.value.path.toString()).withColor(Constants.Lavender))
                )
                return 0
            }

            val List = Text.literal("Regions in world ")
                .append(Text.literal(W.registryKey.value.path.toString()).withColor(Constants.Lavender))
                .append(":")

            for (R in Regions) {
                List.append(Text.literal("\n  - "))
                    .append(Text.literal(R.Name).formatted(Formatting.AQUA))
                (R as ServerRegion).AppendBounds(List)
            }

            S.Reply(List)
            return 1
        }

        fun PrintRegionInfo(S: ServerCommandSource, R: ServerRegion): Int {
            val Stats = R.AppendWorldAndName(Text.literal("Region "))
            R.AppendBounds(Stats)
            Stats.append(R.Stats)
            S.Reply(Stats)
            return 1
        }

        fun PrintRegionInfo(S: ServerCommandSource, SP: ServerPlayerEntity): Int {
            val W = SP.world
            val Regions = ProtectionManager.GetRegions(W)
            val R = Regions.find { SP.blockPos in it }
            if (R == null) {
                S.sendError(NOT_IN_ANY_REGION)
                return 0
            }

            return PrintRegionInfo(S, R as ServerRegion)
        }

        fun SetFlag(
            S: ServerCommandSource,
            R: ServerRegion,
            Flag: Region.Flags,
            Allow: Boolean
        ): Int {
            R.SetFlag(S.server, Flag, Allow)
            val Mess = Text.literal("Set region flag ")
                .append(Text.literal(Flag.name.lowercase()).withColor(Constants.Orange))
                .append(" to ")
                .append(
                    if (Allow) Text.literal("allow").formatted(Formatting.GREEN)
                    else Text.literal("deny").formatted(Formatting.RED)
                )
                .append(" for region ")

            R.AppendWorldAndName(Mess)
            S.Reply(Mess)
            return 1
        }
    }

    object RenameCommand {
        private val ERR_EMPTY = Exn("New name may not be empty!")
        private val NO_ITEM = Exn("You must be holding an item to rename it!")
        private val RENAME_SUCCESS = ReplyMsg("Your item has been renamed!")
        private val NO_ITALIC = Style.EMPTY.withItalic(false)

        fun Execute(S: ServerCommandSource, Name: Text): Int {
            val SP = S.playerOrThrow
            val St = SP.mainHandStack
            if (St.isEmpty) throw NO_ITEM.create()
            if (Name.string.trim() == "") throw ERR_EMPTY.create()
            St.set(DataComponentTypes.CUSTOM_NAME, Text.empty().append(Name).setStyle(NO_ITALIC))
            S.Success(RENAME_SUCCESS)
            return 1
        }
    }

    object SpeedCommand {
        private val SPEED_LIMIT = Text.of("Speed must be between 1 and 10")

        fun Execute(S: ServerCommandSource, Value: Int): Int {
            // Sanity check so the server doesn’t explode when we move.
            if (Value < 1 || Value > 10) {
                S.sendError(SPEED_LIMIT)
                return 0
            }

            // Convert flying speed to blocks per tick.
            val SP = S.playerOrThrow
            SP.abilities.flySpeed = Value / 20f
            SP.sendAbilitiesUpdate()
            S.Reply("Set flying speed to $Value")
            return 1
        }
    }

    object WarpsCommand {
        private val NO_WARPS = ReplyMsg("No warps defined")

        fun Delete(S: ServerCommandSource, W: WarpManager.Warp): Int {
            WarpManager.Warps.remove(W.Name)
            S.Reply(Text.literal("Deleted warp ").append(Text.literal(W.Name).formatted(Formatting.AQUA)))
            return 1
        }

        private fun FormatWarp(W: WarpManager.Warp): Text =
            Text.empty()
                .append(Text.literal(W.Name).formatted(Formatting.AQUA))
                .append(" in ")
                .append(Text.literal(W.World.value.path.toString()).withColor(Constants.Lavender))
                .append(" at [")
                .append(Text.literal("${W.Pos.x.toInt()}").formatted(Formatting.GRAY))
                .append(", ")
                .append(Text.literal("${W.Pos.y.toInt()}").formatted(Formatting.GRAY))
                .append(", ")
                .append(Text.literal("${W.Pos.z.toInt()}").formatted(Formatting.GRAY))
                .append("]")


        fun List(S: ServerCommandSource): Int {
            if (WarpManager.Warps.isEmpty()) {
                S.sendMessage(NO_WARPS)
                return 0
            }

            val List = Text.literal("Warps:")
            for (W in WarpManager.Warps.values) {
                List.append(Text.literal("\n  - "))
                    .append(FormatWarp(W))
            }

            S.Reply(List)
            return 1
        }

        fun Set(S: ServerCommandSource, SP: ServerPlayerEntity, Name: String): Int {
            val W = WarpManager.Warp(Name, SP.serverWorld.registryKey, SP.pos, SP.yaw, SP.pitch)
            WarpManager.Warps[Name] = W
            S.Reply(Text.literal("Set warp ").append(FormatWarp(W)))
            return 1
        }
    }

    object WildCommand {
        const val MAX_ATTEMPTS = 50
        val TELEPORT_FAILED = Text.of("Sorry, couldn’t find a suitable teleport location. Please try again.")

        /**
         * Teleport a player to a random position in the world
         *
         * The destination must not be within a region, and it must be within
         * the border.
         *
         * We also don't want to send players into a lava lake or ocean. For
         * this, we need to make sure the block they land on is a solid block.
         *
         * This is achieved by iterating downwards from the top of the world
         * once random x and z coordinates have been computed and choosing
         * different x and z coordinates should no suitable block be found
         * at that location.
         */
        fun RandomTeleport(S: ServerCommandSource, SP: ServerPlayerEntity): Int {
            val SW = SP.world as ServerWorld
            val WB = SW.worldBorder
            val MinY = SW.bottomY
            val Sz = (WB.size * .6).toInt()
            val Dim = SW.dimension
            val Nether = Dim.hasCeiling()
            val Y = SW.logicalHeight

            // We may get really bad rolls, so don’t try for ever.
            for (tries in 0..<MAX_ATTEMPTS) {
                // Pick any position inside the world border and not too close
                // to the edge.
                val X = SP.random.nextInt(Sz) - Sz / 2
                val Z = SP.random.nextInt(Sz) - Sz / 2
                var Pos = BlockPos.Mutable(X, Y, Z)

                // Check that it is not in a region.
                if (!ProtectionManager.AllowTeleport(SP, SW, Pos)) continue

                // Check if we can place the player somewhere in this XZ columns.
                //
                // In anything that is not the nether, getTopY() is the fastest way
                // of accomplishing this; however, that won’t create the chunk if it
                // doesn’t already exist, and we also need to do some post-processing
                // in the nether, so grab the chunk first.
                //
                // But first, check if the chunk already exists; if not, we need to
                // generate it; since this is expensive, only allow doing this once.
                val Chunk = SW.getChunk(X, Z, ChunkStatus.SURFACE, false) ?: continue

                // Helpers we’ll need below.
                fun IsAir() = Chunk.getBlockState(Pos).isAir
                fun MoveDownWhile(Cond: () -> Boolean) {
                    while (Pos.y > MinY && Cond()) Pos = Pos.move(Direction.DOWN)
                }

                // Get the top-most solid block. In the nether, continue through
                // the roof, and then keep going again until we no longer hit air;
                // do the last part even if we’re not in the nether since TopY may
                // be in the air.
                Pos.y = SW.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, X, Z)
                if (Nether) MoveDownWhile { !IsAir() } // Scan through ceiling.
                MoveDownWhile(::IsAir) // Scan to ground.

                // Try teleporting if we’re still in bounds and not in a liquid.
                val Up1 = Pos.up()
                val Up2 = Up1.up()
                if (Pos.y > MinY &&
                    Chunk.getBlockState(Pos).isSideSolidFullSquare(SW, Pos, Direction.UP) &&
                    Chunk.getBlockState(Up1).isAir &&
                    Chunk.getBlockState(Up2).isAir
                ) {
                    SP.Teleport(SW, Pos, true)
                    return 1
                }
            }

            // Couldn’t find a suitable location.
            S.sendError(TELEPORT_FAILED)
            return 0
        }
    }

    // =========================================================================
    //  Command Trees
    // =========================================================================
    private fun BackCommand(): LiteralArgumentBuilder<ServerCommandSource> = literal("back")
        .requires { it.isExecutedByPlayer && it.hasPermissionLevel(4) }
        .executes { BackCommand.Teleport(it.source.playerOrThrow) }

    private fun BypassCommand(): LiteralArgumentBuilder<ServerCommandSource> = literal("bypass")
        .requires { it.isExecutedByPlayer && it.hasPermissionLevel(4) }
        .executes { BypassCommand.Toggle(it.source, it.source.playerOrThrow) }

    private fun DelHomeCommand(): LiteralArgumentBuilder<ServerCommandSource> = literal("delhome")
        .requires { it.isExecutedByPlayer }
        .then(argument("name", HomeArgumentType.Home())
            .requires { it.hasPermissionLevel(4) }
            .suggests(HomeArgumentType::Suggest)
            .executes {
                HomeCommand.Delete(
                    it.source,
                    it.source.playerOrThrow,
                    HomeArgumentType.Resolve(it, "name")
                )
            }
        )
        .executes {
            HomeCommand.DeleteDefault(
                it.source,
                it.source.playerOrThrow
            )
        }

    private fun DiscardCommand(): LiteralArgumentBuilder<ServerCommandSource> = literal("discard")
        .requires { it.hasPermissionLevel(4) }
        .then(argument("entity", EntityArgumentType.entities())
            .executes { DiscardCommand.Execute(it.source, EntityArgumentType.getEntities(it, "entity")) }
        )

    private fun DisplayCommand(): LiteralArgumentBuilder<ServerCommandSource> = literal("display")
        .requires { it.hasPermissionLevel(4) }
        .then(literal("clear")
            .then(argument("players", EntityArgumentType.players())
                .executes { DisplayCommand.Clear(
                    it.source,
                    EntityArgumentType.getPlayers(it, "players")
                ) }
            )
        )
        .then(literal("set")
            .then(argument("players", EntityArgumentType.players())
                .then(argument("display", DisplayArgumentType.Display())
                    .suggests(DisplayArgumentType::Suggest)
                    .executes { DisplayCommand.SetDisplay(
                        it.source,
                        EntityArgumentType.getPlayers(it, "players"),
                        DisplayArgumentType.Resolve(it, "display")
                    ) }
                )
            )
        )
        .then(literal("show")
            .then(argument("display", DisplayArgumentType.Display())
                .suggests(DisplayArgumentType::Suggest)
                .executes { DisplayCommand.List(it.source, DisplayArgumentType.Resolve(it, "display")) }
            )
        )
        .executes { DisplayCommand.ListAll(it.source) }

    @Environment(EnvType.SERVER)
    private fun DiscordCommand(): LiteralArgumentBuilder<ServerCommandSource> = literal("discord")
        .then(literal("force-link")
            .requires { it.hasPermissionLevel(4) }
            .then(argument("player", EntityArgumentType.player())
                .then(argument("id", LongArgumentType.longArg())
                    .executes {
                        org.nguh.nguhcraft.server.dedicated.DiscordCommand.ForceLink(
                            it.source,
                            EntityArgumentType.getPlayer(it, "player"),
                            LongArgumentType.getLong(it, "id")
                        )
                    }
                )
            )
        )
        .then(literal("link")
            .requires { it.entity is ServerPlayerEntity && !(it.entity as ServerPlayerDiscordAccessor).isLinked }
            .then(argument("id", LongArgumentType.longArg())
                .executes {
                    org.nguh.nguhcraft.server.dedicated.DiscordCommand.TryLink(
                        it.source,
                        it.source.playerOrThrow,
                        LongArgumentType.getLong(it, "id")
                    )
                }
            )
        )
        .then(literal("list")
            .requires { it.HasModeratorPermissions }
            .then(literal("all").executes { org.nguh.nguhcraft.server.dedicated.DiscordCommand.ListAllOrLinked(it.source, true) })
            .then(literal("linked").executes { org.nguh.nguhcraft.server.dedicated.DiscordCommand.ListAllOrLinked(it.source, false) })
            .then(argument("filter", StringArgumentType.greedyString())
                .executes {
                    org.nguh.nguhcraft.server.dedicated.DiscordCommand.ListPlayers(
                        it.source,
                        StringArgumentType.getString(it, "filter")
                    )
                }
            )
            .executes { org.nguh.nguhcraft.server.dedicated.DiscordCommand.ListSyntaxError(it.source) }
        )
        .then(literal("unlink")
            .then(argument("player", EntityArgumentType.player())
                .requires { it.HasModeratorPermissions }
                .executes {
                    org.nguh.nguhcraft.server.dedicated.DiscordCommand.TryUnlink(
                        it.source,
                        EntityArgumentType.getPlayer(it, "player")
                    )
                }
            )
            .requires {
                (it.entity is ServerPlayerEntity && (it.entity as ServerPlayerDiscordAccessor).isLinked) ||
                it.hasPermissionLevel(4)
            }
            .executes {
                org.nguh.nguhcraft.server.dedicated.DiscordCommand.TryUnlink(
                    it.source,
                    it.source.playerOrThrow
                )
            }
        )

    private fun EnchantCommand(A: CommandRegistryAccess): LiteralArgumentBuilder<ServerCommandSource> = literal("enchant")
        .requires { it.hasPermissionLevel(4) }
        .then(
            argument("enchantment", RegistryEntryReferenceArgumentType.registryEntry(A, RegistryKeys.ENCHANTMENT))
                .then(argument("level", IntegerArgumentType.integer())
                    .executes {
                        EnchantCommand.Enchant(
                            it.source,
                            it.source.playerOrThrow,
                            RegistryEntryReferenceArgumentType.getEnchantment(it, "enchantment"),
                            IntegerArgumentType.getInteger(it, "level")
                        )
                    }
                )
                .executes {
                    EnchantCommand.Enchant(
                        it.source,
                        it.source.playerOrThrow,
                        RegistryEntryReferenceArgumentType.getEnchantment(it, "enchantment"),
                        1
                    )
                }
        )

    private fun EntityCountCommand(): LiteralArgumentBuilder<ServerCommandSource> = literal("entity_count")
        .requires { it.hasPermissionLevel(4) }
        .then(argument("selector", EntityArgumentType.entities())
            .executes {
                val E = EntityArgumentType.getEntities(it, "selector")
                it.source.Reply("There are ${E.size} entities that match the given selector.")
                E.size
            }
        )

    private fun FixCommand(): LiteralArgumentBuilder<ServerCommandSource> = literal("fix")
        .requires { it.isExecutedByPlayer && it.hasPermissionLevel(4) }
        .then(literal("all").executes { FixCommand.FixAll(it.source, it.source.playerOrThrow) })
        .executes { FixCommand.Fix(it.source, it.source.playerOrThrow) }

    private fun HomeCommand(): LiteralArgumentBuilder<ServerCommandSource> = literal("home")
        .requires { it.isExecutedByPlayer }
        .then(argument("home", HomeArgumentType.Home())
            .suggests(HomeArgumentType::Suggest)
            .executes {
                HomeCommand.Teleport(
                    it.source.playerOrThrow,
                    HomeArgumentType.Resolve(it, "home")
                )
            }
        )
        .executes { HomeCommand.TeleportToDefault(it.source.playerOrThrow) }

    private fun HomesCommand(): LiteralArgumentBuilder<ServerCommandSource> = literal("homes")
        .then(argument("player", EntityArgumentType.player())
            .requires { it.hasPermissionLevel(4) }
            .executes {
                HomeCommand.List(
                    it.source,
                    EntityArgumentType.getPlayer(it, "player")
                )
            }
        )
        .executes { HomeCommand.List(it.source, it.source.playerOrThrow) }

    private fun KeyCommand(): LiteralArgumentBuilder<ServerCommandSource> = literal("key")
        .requires { it.hasPermissionLevel(4) }
        .then(argument("key", StringArgumentType.string())
            .executes {
                KeyCommand.Generate(
                    it.source,
                    it.source.playerOrThrow,
                    StringArgumentType.getString(it, "key")
                )
            }
        )

    private fun MessageCommand(): LiteralArgumentBuilder<ServerCommandSource> = literal("msg")
        .then(argument("targets", EntityArgumentType.players())
            .then(argument("message", StringArgumentType.greedyString())
                .executes {
                    val Players = EntityArgumentType.getPlayers(it, "targets")
                    val Message = StringArgumentType.getString(it, "message")
                    Chat.SendPrivateMessage(it.source.player, Players, Message)
                    Players.size
                }
            )
        )

    private fun ModCommand(): LiteralArgumentBuilder<ServerCommandSource> = literal("mod")
        .requires { it.hasPermissionLevel(4) }
        .then(argument("player", EntityArgumentType.player())
            .executes {
                val S = it.source
                val SP = EntityArgumentType.getPlayer(it, "player")
                SP.IsModerator = !SP.IsModerator
                S.server.commandManager.sendCommandTree(SP)
                S.sendMessage(
                    Text.literal("Player '${SP.displayName?.string}' is ${if (SP.IsModerator) "now" else "no longer"} a moderator")
                    .formatted(Formatting.YELLOW)
                )
                1
            }
        )

    private fun ObliterateCommand(): LiteralArgumentBuilder<ServerCommandSource> = literal("obliterate")
        .requires { it.hasPermissionLevel(2) }
        .then(argument("players", EntityArgumentType.players())
            .executes {
                val Players = EntityArgumentType.getPlayers(it, "players")
                for (SP in Players) ServerUtils.Obliterate(SP)
                it.source.sendMessage(Text.literal("Obliterated ${Players.size} players"))
                Players.size
            }
        )

    private fun ProcedureCommand(): LiteralArgumentBuilder<ServerCommandSource> = literal("procedure")
        .requires { it.hasPermissionLevel(4) } // Procedures should not be able to create themselves.
        .then(literal("append")
            .then(argument("procedure", ProcedureArgumentType.Procedure())
                .suggests(ProcedureArgumentType::Suggest)
                .then(argument("text", StringArgumentType.greedyString())
                    .executes { ProcedureCommand.Append(
                        it.source,
                        ProcedureArgumentType.Resolve(it, "procedure"),
                        StringArgumentType.getString(it, "text")
                    ) }
                )
            )
        )
        .then(literal("call")
            .then(argument("procedure", ProcedureArgumentType.Procedure())
                .suggests(ProcedureArgumentType::Suggest)
                .executes { ProcedureCommand.Call(it.source, ProcedureArgumentType.Resolve(it, "procedure")) }
            )
        )
        .then(literal("clear")
            .then(argument("procedure", ProcedureArgumentType.Procedure())
                .suggests(ProcedureArgumentType::Suggest)
                .executes { ProcedureCommand.Clear(it.source, ProcedureArgumentType.Resolve(it, "procedure")) }
            )
        )
        .then(literal("create")
            .then(argument("procedure", StringArgumentType.greedyString()) // Not a procedure arg because it doesn’t exist yet.
                .executes { ProcedureCommand.Create(it.source, StringArgumentType.getString(it, "procedure")) }
            )
        )
        .then(literal("del")
            .then(argument("procedure", ProcedureArgumentType.Procedure())
                .suggests(ProcedureArgumentType::Suggest)
                .then(argument("line", IntegerArgumentType.integer())
                    .then(literal("to")
                        .then(argument("until", IntegerArgumentType.integer())
                            .executes { ProcedureCommand.DeleteLine(
                                it.source,
                                ProcedureArgumentType.Resolve(it, "procedure"),
                                IntegerArgumentType.getInteger(it, "line"),
                                IntegerArgumentType.getInteger(it, "until")
                            ) }
                        )
                    )
                    .executes { ProcedureCommand.DeleteLine(
                        it.source,
                        ProcedureArgumentType.Resolve(it, "procedure"),
                        IntegerArgumentType.getInteger(it, "line")
                    ) }
                )
            )
        )
        .then(literal("delete-procedure")
            .then(argument("procedure", ProcedureArgumentType.Procedure())
                .suggests(ProcedureArgumentType::Suggest)
                .executes { ProcedureCommand.DeleteProcedure(it.source, ProcedureArgumentType.Resolve(it, "procedure")) }
            )
        )
        .then(literal("insert")
            .then(argument("procedure", ProcedureArgumentType.Procedure())
                .suggests(ProcedureArgumentType::Suggest)
                .then(argument("line", IntegerArgumentType.integer())
                    .then(argument("text", StringArgumentType.greedyString())
                        .executes { ProcedureCommand.InsertLine(
                            it.source,
                            ProcedureArgumentType.Resolve(it, "procedure"),
                            IntegerArgumentType.getInteger(it, "line"),
                            StringArgumentType.getString(it, "text")
                        ) }
                    )
                )
            )
        )
        .then(literal("list").executes { ProcedureCommand.List(it.source) })
        .then(literal("listing")
            .then(argument("procedure", ProcedureArgumentType.Procedure())
                .suggests(ProcedureArgumentType::Suggest)
                .executes { ProcedureCommand.Listing(it.source, ProcedureArgumentType.Resolve(it, "procedure")) }
            )
        )
        .then(literal("set")
            .then(argument("procedure", ProcedureArgumentType.Procedure())
                .suggests(ProcedureArgumentType::Suggest)
                .then(argument("line", IntegerArgumentType.integer())
                    .then(argument("text", StringArgumentType.greedyString())
                        .executes { ProcedureCommand.SetLine(
                            it.source,
                            ProcedureArgumentType.Resolve(it, "procedure"),
                            IntegerArgumentType.getInteger(it, "line"),
                            StringArgumentType.getString(it, "text")
                        ) }
                    )
                )
            )
        )
        .then(literal("source")
            .then(argument("procedure", ProcedureArgumentType.Procedure())
                .suggests(ProcedureArgumentType::Suggest)
                .executes { ProcedureCommand.Source(it.source, ProcedureArgumentType.Resolve(it, "procedure")) }
            )
        )

    private fun RegionCommand(): LiteralArgumentBuilder<ServerCommandSource> {
        val RegionFlagsNameNode = argument("region", RegionArgumentType.Region())
        Region.Flags.entries.forEach { Flag ->
            fun Set(C: CommandContext<ServerCommandSource>, Value: Boolean) = RegionCommand.SetFlag(
                C.source,
                RegionArgumentType.Resolve(C, "region"),
                Flag,
                Value
            )

            RegionFlagsNameNode.then(literal(Flag.name.lowercase())
                .then(literal("allow").executes { Set(it, true) })
                .then(literal("deny").executes { Set(it, false) })
                .then(literal("disable").executes { Set(it, false) })
                .then(literal("enable").executes { Set(it, true) })
            )
        }

        return literal("region")
            .requires { it.hasPermissionLevel(4) }
            .then(literal("list")
                .then(literal("all").executes { RegionCommand.ListAllRegions(it.source) })
                .then(argument("world", DimensionArgumentType.dimension())
                    .executes {
                        RegionCommand.ListRegions(
                            it.source,
                            DimensionArgumentType.getDimensionArgument(it, "world")
                        )
                    }
                )
                .executes { RegionCommand.ListRegions(it.source, it.source.world) }
            )
            .then(literal("add")
                .then(argument("name", StringArgumentType.word())
                    .then(argument("from", ColumnPosArgumentType.columnPos())
                        .then(argument("to", ColumnPosArgumentType.columnPos())
                            .executes {
                                RegionCommand.AddRegion(
                                    it.source,
                                    it.source.world,
                                    StringArgumentType.getString(it, "name"),
                                    ColumnPosArgumentType.getColumnPos(it, "from"),
                                    ColumnPosArgumentType.getColumnPos(it, "to"),
                                )
                            }
                        )
                    )
                )
            )
            .then(literal("del")
                .then(argument("region", RegionArgumentType.Region())
                    .executes {
                        RegionCommand.DeleteRegion(
                            it.source,
                            RegionArgumentType.Resolve(it, "region"),
                        )
                    }
                )
            )
            .then(literal("info")
                .then(argument("region", RegionArgumentType.Region())
                    .executes {
                        RegionCommand.PrintRegionInfo(
                            it.source,
                            RegionArgumentType.Resolve(it, "region")
                        )
                    }
                )
                .executes { RegionCommand.PrintRegionInfo(it.source, it.source.playerOrThrow) }
            )
            .then(literal("flags").then(RegionFlagsNameNode))
    }

    private fun RenameCommand(A: CommandRegistryAccess): LiteralArgumentBuilder<ServerCommandSource>  = literal("rename")
        .requires { it.isExecutedByPlayer && it.hasPermissionLevel(2) }
        .then(argument("name", TextArgumentType.text(A))
            .executes { RenameCommand.Execute(it.source, TextArgumentType.getTextArgument(it, "name")) }
        )

    private fun RuleCommand(): LiteralArgumentBuilder<ServerCommandSource> {
        var Command = literal("rule").requires { it.hasPermissionLevel(4) }
        SyncedGameRule.entries.forEach { Rule ->
            Command = Command.then(literal(Rule.Name)
                .then(argument("value", BoolArgumentType.bool())
                    .executes {
                        Rule.Set(it.source.server, BoolArgumentType.getBool(it, "value"))
                        it.source.sendMessage(Text.literal("Set '${Rule.Name}' to ${Rule.IsSet()}"))
                        1
                    }
                )
                .executes {
                    it.source.sendMessage(Text.literal("Rule '${Rule.Name}' is set to ${Rule.IsSet()}"))
                    1
                }
            )
        }
        return Command
    }

    private fun SayCommand(): LiteralArgumentBuilder<ServerCommandSource> = literal("say")
        .requires { it.hasPermissionLevel(2) }
        .then(argument("message", StringArgumentType.greedyString())
            .executes {
                Chat.SendServerMessage(it.source.server, StringArgumentType.getString(it, "message"))
                1
            }
        )

    private fun SetHomeCommand(): LiteralArgumentBuilder<ServerCommandSource> = literal("sethome")
        .requires { it.isExecutedByPlayer }
        .then(argument("name", StringArgumentType.word())
            .requires { it.hasPermissionLevel(4) }
            .executes {
                HomeCommand.Set(
                    it.source,
                    it.source.playerOrThrow,
                    StringArgumentType.getString(it, "name")
                )
            }
        )
        .executes {
            HomeCommand.Set(
                it.source,
                it.source.playerOrThrow,
                Home.DEFAULT_HOME
            )
        }

    private fun SmiteCommand(): LiteralArgumentBuilder<ServerCommandSource> = literal("smite")
        .requires { it.hasPermissionLevel(2) }
        .then(argument("targets", EntityArgumentType.entities())
            .executes {
                // Smite everything that isn’t also lightning, because that becomes
                // exponential *really* quick...
                val Entities = EntityArgumentType.getEntities(it, "targets")
                for (E in Entities)
                    if (E !is LightningEntity)
                        StrikeLightning(E.world as ServerWorld, E.pos)

                // And tell the user how many things were smitten.
                it.source.sendMessage(Text.literal(
                    if (Entities.size == 1) "${Entities.first().nameForScoreboard} has been smitten"
                    else "${Entities.size} entities have been smitten"
                ).formatted(Formatting.YELLOW))
                Entities.size
            }
        )
        .then(argument("where", BlockPosArgumentType.blockPos())
            .requires { it.isExecutedByPlayer }
            .executes {
                val Pos = BlockPosArgumentType.getBlockPos(it, "where")
                StrikeLightning(it.source.world as ServerWorld, Vec3d.ofBottomCenter(Pos))
                it.source.sendMessage(Text.literal("[$Pos] has been smitten").formatted(Formatting.YELLOW))
                1
            }
        )

    private fun SpeedCommand(): LiteralArgumentBuilder<ServerCommandSource> = literal("speed")
        .requires { it.hasPermissionLevel(4) && it.isExecutedByPlayer }
        .then(argument("value", IntegerArgumentType.integer())
            .executes {
                SpeedCommand.Execute(
                    it.source,
                    IntegerArgumentType.getInteger(it, "value")
                )
            }
        )

    private fun SubscribeToConsoleCommand(): LiteralArgumentBuilder<ServerCommandSource> = literal("subscribe_to_console")
        .requires { it.isExecutedByPlayer && it.hasPermissionLevel(4) }
        .executes {
            val SP = it.source.playerOrThrow
            SP.IsSubscribedToConsole = !SP.IsSubscribedToConsole
            it.source.sendMessage(Text.literal(
                "You are ${if (SP.IsSubscribedToConsole) "now" else "no longer"} receiving console messages"
            ).formatted(Formatting.YELLOW))
            1
        }

    private fun TopCommand(): LiteralArgumentBuilder<ServerCommandSource> = literal("top")
        .requires { it.hasPermissionLevel(4) && it.isExecutedByPlayer }
        .executes {
            val SP = it.source.playerOrThrow
            val SW = SP.serverWorld
            val TopY = SW.getTopY(Heightmap.Type.WORLD_SURFACE, SP.x.toInt(), SP.z.toInt()) - 1

            // Make sure this doesn’t put us in the void.
            if (TopY <= SW.bottomY) return@executes 0

            // Make sure the block is solid.
            val Pos = BlockPos(SP.x.toInt(), TopY, SP.z.toInt())
            val St = SW.getBlockState(Pos)
            if (!St.isAir) SP.Teleport(SW, Pos, true)
            else it.source.sendError(Text.literal("Couldn’t find a suitable location to teleport to!"))
            1
        }

    private fun UUIDCommand(): LiteralArgumentBuilder<ServerCommandSource> = literal("uuid")
        .then(argument("player", EntityArgumentType.player())
            .requires { it.hasPermissionLevel(4) }
            .executes {
                val Player = EntityArgumentType.getPlayer(it, "player")
                it.source.sendMessage(Text.literal("UUID: ${Player.uuid}"))
                1
            }
        )
        .executes {
            it.source.sendMessage(Text.literal("Your UUID: ${it.source.playerOrThrow.uuid}"))
            1
        }

    @Environment(EnvType.SERVER)
    private fun VanishCommand(): LiteralArgumentBuilder<ServerCommandSource> = literal("vanish")
        .requires { it.isExecutedByPlayer && it.hasPermissionLevel(4) }
        .executes {
            val SP = it.source.playerOrThrow
            Vanish.Toggle(SP)
            it.source.sendMessage(Text.literal(
                "You are ${if (SP.IsVanished) "now" else "no longer"} vanished"
            ).formatted(Formatting.YELLOW))
            1
        }

    private fun WarpCommand(): LiteralArgumentBuilder<ServerCommandSource> = literal("warp")
        .requires { it.isExecutedByPlayer }
        .then(argument("warp", WarpArgumentType.Warp())
            .suggests(WarpArgumentType::Suggest)
            .executes {
                val W = WarpArgumentType.Resolve(it, "warp")
                val SP = it.source.playerOrThrow
                SP.Teleport(SP.server.getWorld(W.World)!!, W.Pos, W.Yaw, W.Pitch, true)
                1
            }
        )

    private fun WarpsCommand(): LiteralArgumentBuilder<ServerCommandSource> = literal("warps")
        .requires { it.isExecutedByPlayer }
        .then(literal("set")
            .requires { it.hasPermissionLevel(4) }
            .then(argument("warp", StringArgumentType.word())
                .executes {
                    WarpsCommand.Set(
                        it.source,
                        it.source.playerOrThrow,
                        StringArgumentType.getString(it, "warp")
                    )
                }
            )
        )
        .then(literal("del")
            .requires { it.hasPermissionLevel(4) }
            .then(argument("warp", WarpArgumentType.Warp())
                .suggests(WarpArgumentType::Suggest)
                .executes {
                    WarpsCommand.Delete(
                        it.source,
                        WarpArgumentType.Resolve(it, "warp")
                    )
                }
            )
        )
        .executes { WarpsCommand.List(it.source) }

    private fun WildCommand(): LiteralArgumentBuilder<ServerCommandSource> = literal("wild")
        .executes { WildCommand.RandomTeleport(it.source, it.source.playerOrThrow) }
}