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
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.arguments.*
import net.minecraft.commands.arguments.ResourceArgument
import net.minecraft.commands.synchronization.SingletonArgumentInfo
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.component.TooltipDisplay
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LightningBolt
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.effect.MobEffectCategory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceKey
import net.minecraft.core.registries.Registries
import net.minecraft.core.Holder
import net.minecraft.commands.Commands.argument
import net.minecraft.commands.Commands.literal
import net.minecraft.commands.CommandSourceStack
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.level.ServerLevel
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.Component
import net.minecraft.ChatFormatting
import net.minecraft.commands.arguments.coordinates.BlockPosArgument
import net.minecraft.commands.arguments.coordinates.ColumnPosArgument
import net.minecraft.commands.arguments.coordinates.Vec3Argument
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ColumnPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.contents.PlainTextContents
import net.minecraft.world.phys.Vec3
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.level.Level
import net.minecraft.world.level.chunk.status.ChunkStatus
import org.nguh.nguhcraft.Constants
import org.nguh.nguhcraft.server.MCBASIC
import org.nguh.nguhcraft.Nguhcraft.Companion.Id
import org.nguh.nguhcraft.SyncedGameRule
import org.nguh.nguhcraft.entity.EntitySpawnManager
import org.nguh.nguhcraft.event.EventDifficulty
import org.nguh.nguhcraft.event.EventManager
import org.nguh.nguhcraft.event.NguhMobType
import org.nguh.nguhcraft.item.KeyItem
import org.nguh.nguhcraft.network.ClientFlags
import org.nguh.nguhcraft.protect.ProtectionManager
import org.nguh.nguhcraft.protect.Region
import org.nguh.nguhcraft.protect.TeleportResult
import org.nguh.nguhcraft.server.*
import org.nguh.nguhcraft.server.ServerUtils.IsIntegratedServer
import org.nguh.nguhcraft.server.ServerUtils.StrikeLightning
import org.nguh.nguhcraft.server.dedicated.Vanish

fun CommandSourceStack.Error(Msg: String?) = sendFailure(Component.nullToEmpty(Msg))

// Used when a command causes a general change, or for status updates.
fun CommandSourceStack.Reply(Msg: String) = Reply(Component.literal(Msg))
fun CommandSourceStack.Reply(Msg: Component) = Reply(Component.empty().append(Msg))
fun CommandSourceStack.Reply(Msg: MutableComponent) = sendSystemMessage(Msg.withStyle(ChatFormatting.YELLOW))

// Used when a command results in the addition or creation of something.
fun CommandSourceStack.Success(Msg: String) = Success(Component.literal(Msg))
fun CommandSourceStack.Success(Msg: Component) = Success(Component.empty().append(Msg))
fun CommandSourceStack.Success(Msg: MutableComponent) = sendSystemMessage(Msg.withStyle(ChatFormatting.GREEN))

val CommandSourceStack.HasModeratorPermissions: Boolean get() =
    hasPermission(4) || (isPlayer && playerOrException.Data.IsModerator)

fun ReplyMsg(Msg: String): Component = Component.literal(Msg).withStyle(ChatFormatting.YELLOW)

object Commands {
    inline fun <reified T : ArgumentType<*>> ArgType(Key: String, noinline Func: () -> T) {
        ArgumentTypeRegistry.registerArgumentType(
            Id(Key),
            T::class.java,
            SingletonArgumentInfo.contextFree(Func)
        )
    }

    const val OPERATOR_PERMISSION_LEVEL = 4

    fun Register() {
        CommandRegistrationCallback.EVENT.register { D, A, E ->
            if (E.includeDedicated) {
                D.register(DiscordCommand())           // /discord
                D.register(ModCommand())               // /mod
                D.register(UpdateBotCommandsCommand()) // /update_bot_commands
                D.register(VanishCommand())            // /vanish
            }

            D.register(BackCommand())                  // /back
            D.register(BypassCommand())                // /bypass
            D.register(DelHomeCommand())               // /delhome
            D.register(DiscardCommand())               // /discard
            D.register(DisplayCommand())               // /display
            D.register(EnchantCommand(A))              // /enchant
            D.register(EntityCountCommand())           // /entity_count
            D.register(EventCommand())                 // /event
            D.register(FixCommand())                   // /fix
            D.register(HealCommand())                  // /heal
            D.register(HereCommand())                  // /here
            D.register(HomeCommand())                  // /home
            D.register(HomesCommand())                 // /homes
            D.register(KeyCommand())                   // /key
            val Msg = D.register(MessageCommand())     // /msg
            D.register(ObliterateCommand())            // /obliterate
            D.register(ProcedureCommand())             // /procedure
            D.register(RegionCommand())                // /region
            D.register(RenameCommand(A))               // /rename
            D.register(RuleCommand())                  // /rule
            D.register(SayCommand())                   // /say
            D.register(SetHomeCommand())               // /sethome
            D.register(SmiteCommand())                 // /smite
            D.register(SpawnsCommand(A))               // /spawns
            D.register(SpeedCommand())                 // /speed
            D.register(SubscribeToConsoleCommand())    // /subscribe_to_console
            D.register(literal("tell").redirect(Msg))  // /tell
            D.register(TopCommand())                   // /top
            D.register(UUIDCommand())                  // /uuid
            D.register(literal("w").redirect(Msg))     // /w
            D.register(WarpCommand())                  // /warp
            D.register(WarpsCommand())                 // /warps
        }

        ArgType("display", DisplayArgumentType::Display)
        ArgType("home", HomeArgumentType::Home)
        ArgType("procedure", ProcedureArgumentType::Procedure)
        ArgType("region", RegionArgumentType::Region)
        ArgType("warp", WarpArgumentType::Warp)
        ArgType("mob", MobArgumentType::Mob)
    }

    fun Exn(message: String): SimpleCommandExceptionType {
        return SimpleCommandExceptionType(Component.literal(message))
    }

    // =========================================================================
    //  Command Implementations
    // =========================================================================
    object BackCommand {
        private val ERR_NO_TARGET = Exn("No saved target to teleport back to!")

        fun Teleport(SP: ServerPlayer): Int {
            val Pos = SP.Data.LastPositionBeforeTeleport ?: throw ERR_NO_TARGET.create()
            SP.Teleport(Pos.Instantiate(SP.Server), true)
            return 1
        }
    }

    object BypassCommand {
        private val BYPASSING = ReplyMsg("Now bypassing region protection.")
        private val NOT_BYPASSING = ReplyMsg("No longer bypassing region protection.")

        fun Toggle(S: CommandSourceStack, SP: ServerPlayer): Int {
            val NewState = !SP.Data.BypassesRegionProtection
            SP.Data.BypassesRegionProtection = NewState
            SP.SetClientFlag(ClientFlags.BYPASSES_REGION_PROTECTION, NewState)
            S.sendSystemMessage(if (NewState) BYPASSING else NOT_BYPASSING)
            return 1
        }
    }

    object DiscardCommand {
        private val REASON = ReplyMsg("Player entity was discarded")

        fun Execute(S: CommandSourceStack, Entities: Collection<Entity>): Int {
            for (E in Entities) {
                // Discard normal entities.
                if (E !is ServerPlayer) E.discard()

                // Disconnect players instead of discarding them, but do
                // not disconnect ourselves in single player.
                else if (!IsIntegratedServer()) E.connection.disconnect(REASON)
            }

            S.Reply("Discarded ${Entities.size} entities")
            return Entities.size
        }
    }

    object DisplayCommand {
        fun Clear(S: CommandSourceStack, Players: Collection<ServerPlayer>): Int {
            for (SP in Players) S.server.DisplayManager.SetActiveDisplay(SP, null)
            return Players.size
        }

        fun List(S: CommandSourceStack, D: DisplayHandle): Int {
            S.sendSystemMessage(D.Listing())
            return 1
        }

        fun ListAll(S: CommandSourceStack): Int {
            S.Reply(S.server.DisplayManager.ListAll())
            return 0
        }

        fun SetDisplay(S: CommandSourceStack, Players: Collection<ServerPlayer>, D: DisplayHandle): Int {
            for (SP in Players) S.server.DisplayManager.SetActiveDisplay(SP, D)
            return Players.size
        }
    }

    object EnchantCommand {
        private val ERR_NO_ITEM = Exn("You must be holding an item to enchant it!")

        fun Enchant(
            S: CommandSourceStack,
            SP: ServerPlayer,
            E: Holder<Enchantment>,
            Lvl: Int
        ): Int {
            // This *does* work for books. Fabric’s documentation says otherwise,
            // but it’s simply incorrect about that.
            val ItemStack = SP.mainHandItem
            if (ItemStack.isEmpty) throw ERR_NO_ITEM.create()
            ItemStack.enchant(E, Lvl)
            S.Success(
                Component.translatable(
                    "commands.enchant.success.single", *arrayOf<Any>(
                        Enchantment.getFullname(E, Lvl),
                        SP.Name,
                    )
                )
            )
            return 1
        }
    }

    object EventCommand {
        private val SPAWN_FAILED = Exn("Failed to spawn mob")

        fun AddPlayer(S: CommandSourceStack, SP: ServerPlayer): Int {
            if (S.server.EventManager.Add(SP)) S.Success("Added player '${SP.scoreboardName}' to the event")
            else S.Reply("Player '${SP.scoreboardName}' is already participating")
            return 1
            1
        }

        fun ListPlayers(S: CommandSourceStack): Int {
            val Players = S.server.EventManager.Players
            if (Players.isEmpty()) {
                S.Reply("No players are participating in the event")
                return 0
            }

            // Only print online players here; we *could* go to the trouble of
            // getting offline player’s names (via NguhPlayerList) and print
            // them too, but most players during an event are probably online,
            // so we don’t really care.
            val Msg = Component.literal("Players:")
            for (Id in Players) {
                val SP = S.server.playerList.getPlayer(Id)
                Msg.append(Component.literal("\n  - ").append(SP?.Name ?: Component.literal(Id.toString()).withStyle(ChatFormatting.GRAY)))
            }
            S.Reply(Msg)
            return Players.size
        }

        fun RemovePlayer(S: CommandSourceStack, SP: ServerPlayer): Int {
            if (S.server.EventManager.Remove(SP)) S.Success("Removed player '${SP.scoreboardName}' from the event")
            else S.Error("Player '${SP.scoreboardName}' is not participating")
            return 1
        }

        fun SetDifficulty(S: CommandSourceStack, D: EventDifficulty): Int {
            if (S.server.EventManager.Difficulty == D) S.Reply("Event difficulty is already set to $D")
            else {
                S.server.EventManager.Difficulty = D
                S.Success("Set event difficulty to $D")
            }
            return 1
        }

        fun SpawnEventMob(S: CommandSourceStack, Type: NguhMobType, Where: Vec3): Int {
            Type.Spawn(S.level, Where) ?: throw SPAWN_FAILED.create()
            return 1
        }

        fun SpawnEventMobTesting(S: CommandSourceStack, Type: NguhMobType): Int {
            val SP = S.playerOrException
            val Rot = SP.getViewVector(1.0F)
            val Dir = Direction.getApproximateNearest(Rot.x, 0.0, Rot.z)
            val Orth = if (Dir.axis == Direction.Axis.X) Direction.NORTH else Direction.WEST
            val Pos = SP.blockPosition().mutable().move(Dir, 2).move(Orth, -7)
            for (D in EventDifficulty.entries) {
                val E = Type.Spawn(SP.level(), Pos.move(Orth, 2).bottomCenter, D)
                if (E is LivingEntity) E.getAttribute(Attributes.MOVEMENT_SPEED)?.baseValue = 0.0
                E?.isSilent = true
            }
            return 1
        }
    }

    object FixCommand {
        private val FIXED_ONE = ReplyMsg("Fixed item in hand")
        private val FIXED_ALL = ReplyMsg("Fixed all items in inventory")

        fun Fix(S: CommandSourceStack, SP: ServerPlayer): Int {
            FixStack(SP.mainHandItem)
            S.sendSystemMessage(FIXED_ONE)
            return 1
        }

        fun FixAll(S: CommandSourceStack, SP: ServerPlayer): Int {
            for (St in SP.inventory.nonEquipmentItems) FixStack(St)
            S.sendSystemMessage(FIXED_ALL)
            return 1
        }

        private fun FixStack(St: ItemStack) {
            if (St.isEmpty) return
            St.remove(DataComponents.LORE)
            St.set(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT)
        }
    }

    object HealCommand {
        fun Heal(S: CommandSourceStack, Entities: Collection<Entity>): Int {
            for (E in Entities) {
                if (E is LivingEntity) {
                    // Heal to maximum health.
                    E.heal(Float.MAX_VALUE)

                    // Remove status effects. Take care to copy the list first so we
                    // don’t try to modify it while iterating over it.
                    for (S in E.activeEffectsMap.values.filter {
                        it.effect.value().category == MobEffectCategory.HARMFUL
                    }) E.removeEffect(S.effect)

                    // Replenish saturation.
                    if (E is Player) E.foodData.eat(10000, 10000.0F)
                }

                // Extinguish fire.
                E.clearFire()

                // Reset oxygen level.
                E.airSupply = E.maxAirSupply
            }

            val Size = Entities.size
            val Name = Entities.first().displayName ?: Component.literal(Entities.first().scoreboardName)
            if (Size == 1) S.Success(Component.literal("Healed ").append(Name))
            else S.Success("Healed $Size entities")
            return Size
        }
    }

    object HomeCommand {
        private val CANT_TOUCH_THIS = Exn("The 'bed' home is special and cannot be deleted or set!")
        private val CANT_ENTER = DynamicCommandExceptionType { Component.literal("The home '$it' is in a region that restricts teleporting!") }
        private val CANT_LEAVE = DynamicCommandExceptionType { Component.literal("Teleporting out of this region is not allowed!") }
        private val CANT_SETHOME_HERE = Exn("Cannot /sethome here as this region restricts teleporting!")
        private val CONNOR_MACLEOD = Exn("You may only have one home!")
        private val NO_HOMES = ReplyMsg("No homes defined!")

        fun Delete(S: CommandSourceStack, SP: ServerPlayer, H: Home): Int {
            if (H.Name == Home.BED_HOME) throw CANT_TOUCH_THIS.create()
            SP.Data.Homes.remove(H)
            S.Reply(Component.literal("Deleted home ").append(Component.literal(H.Name).withStyle(ChatFormatting.AQUA)))
            return 1
        }

        fun DeleteDefault(S: CommandSourceStack, SP: ServerPlayer): Int {
            val H = SP.Data.Homes.find { it.Name == Home.DEFAULT_HOME }?: return 0
            return Delete(S, SP, H)
        }

        private fun FormatHome(H: Home): Component =
            Component.literal("\n  - ")
                .append(Component.literal(H.Name).withStyle(ChatFormatting.AQUA))
                .append(" in ")
                .append(Component.literal(H.World.location().path.toString()).withColor(Constants.Lavender))
                .append(" at [")
                .append(Component.literal("${H.Pos.x}").withStyle(ChatFormatting.GRAY))
                .append(", ")
                .append(Component.literal("${H.Pos.y}").withStyle(ChatFormatting.GRAY))
                .append(", ")
                .append(Component.literal("${H.Pos.z}").withStyle(ChatFormatting.GRAY))
                .append("]")

        fun List(S: CommandSourceStack, SP: ServerPlayer): Int {
            val Homes = SP.Data.Homes
            if (Homes.isEmpty()) {
                S.sendSystemMessage(NO_HOMES)
                return 0
            }

            val List = Component.literal("Homes:")
            List.append(FormatHome(Home.Bed(SP)))
            for (H in Homes) List.append(FormatHome(H))
            S.Reply(List)
            return 1
        }

        fun Set(S: CommandSourceStack, SP: ServerPlayer, RawName: String): Int {
            val (TargetPlayer, Name) = HomeArgumentType.MapOrThrow(SP, RawName)
            if (Name == Home.BED_HOME) throw CANT_TOUCH_THIS.create()
            val Homes = SP.Data.Homes

            // If this region doesn’t allow entry by teleport, then setting a home here makes
            // no sense as we can’t use it, which might not be obvious to people.
            //
            // Note that we pass in 'SP', not 'TargetPlayer' as the player whose permissions to
            // check here; this allows admins to set someone else’s home in a restricted region if
            // need be.
            if (!ProtectionManager.AllowTeleportToFromAnywhere(SP, SP.level(), SP.blockPosition()))
                throw CANT_SETHOME_HERE.create()

            // Remove the home *after* the check above to ensure we only remove it if we’re about
            // to add a new home to the list.
            Homes.removeIf { it.Name == Name }

            // Check that either there are no other homes or this player can have more than one home.
            if (!TargetPlayer.hasPermissions(4) && Homes.isNotEmpty())
                throw CONNOR_MACLEOD.create()

            // If yes, add it.
            Homes.add(Home(Name, SP.level().dimension(), SP.blockPosition()))
            S.Success(Component.literal("Set home ").append(Component.literal(Name).withStyle(ChatFormatting.AQUA)))
            return 1
        }

        fun Teleport(SP: ServerPlayer, H: Home): Int {
            val World = SP.Server.getLevel(H.World)!!
            when (ProtectionManager.GetTeleportResult(SP, World, H.Pos)) {
                TeleportResult.ENTRY_DISALLOWED -> throw CANT_ENTER.create(H.Name)
                TeleportResult.EXIT_DISALLOWED -> throw CANT_LEAVE.create(H.Name)
                else -> {}
            }

            SP.Teleport(World, H.Pos, true)
            return 1
        }

        fun TeleportToDefault(SP: ServerPlayer): Int {
            val H = SP.Data.Homes.firstOrNull() ?: Home.Bed(SP)
            return Teleport(SP, H)
        }
    }

    object KeyCommand {
        private val ERR_EMPTY = Component.nullToEmpty("Key may not be empty!")

        fun Generate(S: CommandSourceStack, SP: ServerPlayer, Key: String): Int {
            if (Key.isEmpty()) {
                S.sendFailure(ERR_EMPTY)
                return 0
            }

            SP.inventory.add(KeyItem.Create(Key))
            SP.containerMenu.broadcastChanges()
            S.Success(Component.literal("Generated key ").append(Component.literal(Key).withStyle(ChatFormatting.LIGHT_PURPLE)))
            return 1
        }
    }

    object ProcedureCommand {
        private val PROC_EMPTY = ReplyMsg("Procedure is already empty")
        private val NO_PROCEDURES = ReplyMsg("No procedures defined")
        private val INVALID_LINE_NUMBER = Exn("Line number is out of bounds!")

        fun Append(S: CommandSourceStack, Proc: MCBASIC.Procedure, Text: String) =
            InsertLine(S, Proc, Proc.LineCount(), Text)

        fun Call(S: CommandSourceStack, Proc: MCBASIC.Procedure): Int {
            try {
                Proc.ExecuteAndThrow(S)
            } catch (E: Exception) {
                S.sendFailure(Component.literal("Failed to execute procedure ").append(Component.literal(Proc.Name).withStyle(ChatFormatting.GOLD)))
                S.Error(E.message)
                E.printStackTrace()
                return 0
            }

            return 1
        }

        fun Clear(S: CommandSourceStack, Proc: MCBASIC.Procedure): Int {
            if (Proc.IsEmpty()) {
                S.sendSystemMessage(PROC_EMPTY)
                return 0
            }

            Proc.Clear()
            S.Reply(Component.literal("Cleared procedure ").append(Component.literal(Proc.Name).withStyle(ChatFormatting.GOLD)))
            return 1
        }

        fun Create(S: CommandSourceStack, Name: String): Int {
            if (S.server.ProcedureManager.GetExisting(Name) != null) {
                S.Reply(Component.literal("Procedure ")
                    .append(Component.literal(Name).withStyle(ChatFormatting.GOLD))
                    .append(" already exists!")
                )
                return 0
            }

            try {
                S.server.ProcedureManager.GetOrCreate(Name)
                S.Success(Component.literal("Created procedure ").append(Component.literal(Name).withStyle(ChatFormatting.GOLD)))
                return 1
            } catch (E: IllegalArgumentException) {
                S.sendFailure(
                    Component.literal("Failed to create procedure ")
                    .append(Component.literal(Name).withStyle(ChatFormatting.GOLD))
                    .append(": ${E.message}")
                )
                return 0
            }
        }

        fun DeleteLine(S: CommandSourceStack, Proc: MCBASIC.Procedure, Line: Int, Until: Int? = null): Int {
            if (Line >= Proc.LineCount()) throw INVALID_LINE_NUMBER.create()
            if (Until != null && Until >= Proc.LineCount()) throw INVALID_LINE_NUMBER.create()
            Proc.Delete(Line..(Until ?: Line))
            S.Reply("Removed command at index $Line")
            return 1
        }

        fun DeleteProcedure(S: CommandSourceStack, Proc: MCBASIC.Procedure): Int {
            if (Proc.Managed) {
                S.sendFailure(Component.literal("Cannot delete managed procedure ").append(Component.literal(Proc.Name).withStyle(ChatFormatting.GOLD)))
                return 0
            }

            S.server.ProcedureManager.Delete(Proc)
            S.Reply(Component.literal("Deleted procedure ").append(Component.literal(Proc.Name).withStyle(ChatFormatting.GOLD)))
            return 1
        }

        fun InsertLine(S: CommandSourceStack, Proc: MCBASIC.Procedure, Line: Int, Code: String): Int {
            if (Line > Proc.LineCount() || Line < 0) throw INVALID_LINE_NUMBER.create() // '>', not '>='!
            Proc.Insert(Line, Code)
            S.Success(Component.literal("Added command at index $Line"))
            return 1
        }

        fun List(S: CommandSourceStack): Int {
            val Procs = S.server.ProcedureManager.Procedures
            if (Procs.isEmpty()) {
                S.sendSystemMessage(NO_PROCEDURES)
                return 0
            }

            val List = Component.literal("Procedures:")
            for (P in Procs) List.append(Component.literal("\n  - ").append(Component.literal(P.Name).withStyle(ChatFormatting.GOLD)))
            S.Reply(List)
            return Procs.size
        }

        fun Listing(S: CommandSourceStack, Proc: MCBASIC.Procedure): Int {
            val Msg = Component.literal("Procedure ").append(Component.literal(Proc.Name).withStyle(ChatFormatting.GOLD)).append(":\n")
            Proc.Listing(Msg)
            S.Reply(Msg)
            return 1
        }

        fun SetLine(S: CommandSourceStack, Proc: MCBASIC.Procedure, Line: Int, Code: String): Int {
            if (Line >= Proc.LineCount()) throw INVALID_LINE_NUMBER.create()
            Proc[Line] = Code
            S.Reply("Set command at index $Line")
            return 1
        }

        fun Source(S: CommandSourceStack, Proc: MCBASIC.Procedure): Int {
            val Msg = Component.literal("Procedure ").append(Component.literal(Proc.Name).withStyle(ChatFormatting.GOLD)).append(":\n")
            Proc.DisplaySource(Msg, 0)
            S.Reply(Msg)
            return 1
        }
    }

    object RegionCommand {
        private val NOT_IN_ANY_REGION = Component.nullToEmpty("You are not in any region!")
        private val CANNOT_CREATE_EMPTY = Component.nullToEmpty("Refusing to create empty region!")

        fun AddRegion(S: CommandSourceStack, W: Level, Name: String, From: ColumnPos, To: ColumnPos): Int {
            if (From == To) {
                S.sendFailure(CANNOT_CREATE_EMPTY)
                return 0
            }

            try {
                val R = ServerRegion(
                    S.server,
                    W.dimension(),
                    Region(
                        Name,
                        FromX = From.x,
                        FromZ = From.z,
                        ToX = To.x,
                        ToZ = To.z
                    )
                )

                S.server.ProtectionManager.AddRegion(S.server, R)
                S.Success(Component.literal("Created region ")
                    .append(Component.literal(Name).withStyle(ChatFormatting.AQUA))
                    .append(" in world ")
                    .append(Component.literal(W.dimension().location().path.toString()).withColor(Constants.Lavender))
                    .append(" with bounds [")
                    .append(Component.literal("${R.MinX}").withStyle(ChatFormatting.GRAY))
                    .append(", ")
                    .append(Component.literal("${R.MinZ}").withStyle(ChatFormatting.GRAY))
                    .append("] → [")
                    .append(Component.literal("${R.MaxX}").withStyle(ChatFormatting.GRAY))
                    .append(", ")
                    .append(Component.literal("${R.MaxZ}").withStyle(ChatFormatting.GRAY))
                    .append("]")
                )
                return 1
            } catch (E: MalformedRegionException) {
                S.sendFailure(E.Msg)
                return 0
            }
        }

        fun DeleteRegion(S: CommandSourceStack, R: ServerRegion): Int {
            if (!S.server.ProtectionManager.DeleteRegion(S.server, R)) {
                S.sendFailure(R.AppendWorldAndName(Component.literal("No such region: ")))
                return 0
            }

            S.sendSystemMessage(
                R.AppendWorldAndName(Component.literal("Deleted region "))
                .withStyle(ChatFormatting.GREEN)
            )
            return 1
        }

        fun ListAllRegions(S: CommandSourceStack): Int {
            ListRegions(S, S.server.overworld())
            ListRegions(S, S.server.getLevel(Level.NETHER)!!)
            ListRegions(S, S.server.getLevel(Level.END)!!)
            return 3
        }

        fun ListRegions(S: CommandSourceStack, W: Level): Int {
            val Regions = ProtectionManager.GetRegions(W)
            if (Regions.isEmpty()) {
                S.Reply(Component.literal("No regions defined in world ")
                    .append(Component.literal(W.dimension().location().path.toString()).withColor(Constants.Lavender))
                )
                return 0
            }

            val List = Component.literal("Regions in world ")
                .append(Component.literal(W.dimension().location().path.toString()).withColor(Constants.Lavender))
                .append(":")

            for (R in Regions) {
                List.append(Component.literal("\n  - "))
                    .append(Component.literal(R.Name).withStyle(ChatFormatting.AQUA))
                (R as ServerRegion).AppendBounds(List)
            }

            S.Reply(List)
            return 1
        }

        fun PrintRegionInfo(S: CommandSourceStack, R: ServerRegion): Int {
            val Stats = R.AppendWorldAndName(Component.literal("Region "))
            R.AppendBounds(Stats)
            Stats.append(R.Stats)
            S.Reply(Stats)
            return 1
        }

        fun PrintRegionInfo(S: CommandSourceStack, SP: ServerPlayer): Int {
            val W = SP.level()
            val Regions = ProtectionManager.GetRegions(W)
            val R = Regions.find { SP.blockPosition() in it }
            if (R == null) {
                S.sendFailure(NOT_IN_ANY_REGION)
                return 0
            }

            return PrintRegionInfo(S, R as ServerRegion)
        }

        fun SetFlag(
            S: CommandSourceStack,
            R: ServerRegion,
            Flag: Region.Flags,
            Allow: Boolean
        ): Int {
            R.SetFlag(S.server, Flag, Allow)
            val Mess = Component.literal("Set region flag ")
                .append(Component.literal(Flag.name.lowercase()).withColor(Constants.Orange))
                .append(" to ")
                .append(
                    if (Allow) Component.literal("allow").withStyle(ChatFormatting.GREEN)
                    else Component.literal("deny").withStyle(ChatFormatting.RED)
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

        fun Execute(S: CommandSourceStack, Name: Component): Int {
            val SP = S.playerOrException
            val St = SP.mainHandItem
            if (St.isEmpty) throw NO_ITEM.create()
            if (Name.string.trim() == "") throw ERR_EMPTY.create()
            St.set(DataComponents.CUSTOM_NAME, Component.empty().append(Name).setStyle(NO_ITALIC))
            S.Success(RENAME_SUCCESS)
            return 1
        }
    }

    object SpawnsCommands {
        val NONESUCH = Exn("Could not find a nearest spawn")
        val INVALID_POS = Exn("Position not valid for spawn;")

        fun AddSpawn(
            S: CommandSourceStack,
            W: ResourceKey<Level>,
            Pos: Vec3,
            Id: String,
            EntityType: Holder.Reference<EntityType<*>>,
            DataParam: CompoundTag? = null
        ): Int {
            if (!Level.isInSpawnableBounds(BlockPos.containing(Pos)))
                throw INVALID_POS.create()

            try {
                val Data = DataParam?.copy() ?: CompoundTag()
                Data.putString("id", EntityType.key().location().toString()) // See SummonCommand::summon()
                val Spawn = EntitySpawnManager.ServerSpawn(W, Pos, Id, Data)
                S.server.EntitySpawnManager.Add(Spawn)
                S.Success("Added spawn $Spawn")
            } catch (E: Exception) {
                S.Error(E.message)
            }

            return 1
        }

        fun Find(S: CommandSourceStack): EntitySpawnManager.ServerSpawn {
            val Pos = S.playerOrException.position()
            val World = S.playerOrException.level()
            return S.server.EntitySpawnManager.Spawns.filter { it.World == World.dimension() }.minByOrNull {
                it.SpawnPos.distanceToSqr(Pos)
            } ?: throw NONESUCH.create()
        }

        fun DeleteNearest(S: CommandSourceStack): Int {
            val Spawn = Find(S)
            S.server.EntitySpawnManager.Delete(Spawn)
            S.Reply("Deleted spawn $Spawn")
            return 1
        }

        fun FindNearest(S: CommandSourceStack): Int {
            val Spawn = Find(S)
            S.Reply("The nearest spawn is $Spawn")
            return 1
        }

        fun ListSpawns(S: CommandSourceStack): Int {
            val Msg = Component.literal("Spawns: ")
            for (S in S.server.EntitySpawnManager.Spawns)
                Msg.append("\n - ").append(S.toString())
            S.Reply(Msg)
            return 0
        }

        fun TeleportNearest(S: CommandSourceStack): Int {
            val Spawn = Find(S)
            S.playerOrException.Teleport(S.server.getLevel(Spawn.World)!!, Spawn.SpawnPos, true)
            return 1
        }
    }

    object SpeedCommand {
        private val SPEED_LIMIT = Component.nullToEmpty("Speed must be between 1 and 10")

        fun Execute(S: CommandSourceStack, Value: Int): Int {
            // Sanity check so the server doesn’t explode when we move.
            if (Value < 1 || Value > 10) {
                S.sendFailure(SPEED_LIMIT)
                return 0
            }

            // Convert flying speed to blocks per tick.
            val SP = S.playerOrException
            SP.abilities.flyingSpeed = Value / 20f
            SP.onUpdateAbilities()
            S.Reply("Set flying speed to $Value")
            return 1
        }
    }

    object WarpsCommand {
        private val NO_WARPS = ReplyMsg("No warps defined")

        fun Delete(S: CommandSourceStack, W: WarpManager.Warp): Int {
            S.server.WarpManager.Warps.remove(W.Name)
            S.Reply(Component.literal("Deleted warp ").append(Component.literal(W.Name).withStyle(ChatFormatting.AQUA)))
            return 1
        }

        private fun FormatWarp(W: WarpManager.Warp): Component =
            Component.empty()
                .append(Component.literal(W.Name).withStyle(ChatFormatting.AQUA))
                .append(" in ")
                .append(Component.literal(W.World.location().path.toString()).withColor(Constants.Lavender))
                .append(" at [")
                .append(Component.literal("${W.X.toInt()}").withStyle(ChatFormatting.GRAY))
                .append(", ")
                .append(Component.literal("${W.Y.toInt()}").withStyle(ChatFormatting.GRAY))
                .append(", ")
                .append(Component.literal("${W.Z.toInt()}").withStyle(ChatFormatting.GRAY))
                .append("]")


        fun List(S: CommandSourceStack): Int {
            if (S.server.WarpManager.Warps.isEmpty()) {
                S.sendSystemMessage(NO_WARPS)
                return 0
            }

            val List = Component.literal("Warps:")
            for (W in S.server.WarpManager.Warps.values) {
                List.append(Component.literal("\n  - "))
                    .append(FormatWarp(W))
            }

            S.Reply(List)
            return 1
        }

        fun Set(S: CommandSourceStack, SP: ServerPlayer, Name: String): Int {
            val W = WarpManager.Warp(Name, SP.level().dimension(), SP.position().x, SP.position().y, SP.position().z, SP.yRot, SP.xRot)
            S.server.WarpManager.Warps[Name] = W
            S.Reply(Component.literal("Set warp ").append(FormatWarp(W)))
            return 1
        }
    }

    // =========================================================================
    //  Command Trees
    // =========================================================================
    private fun BackCommand(): LiteralArgumentBuilder<CommandSourceStack> = literal("back")
        .requires { it.isPlayer && it.hasPermission(4) }
        .executes { BackCommand.Teleport(it.source.playerOrException) }

    private fun BypassCommand(): LiteralArgumentBuilder<CommandSourceStack> = literal("bypass")
        .requires { it.isPlayer && it.hasPermission(4) }
        .executes { BypassCommand.Toggle(it.source, it.source.playerOrException) }

    private fun DelHomeCommand(): LiteralArgumentBuilder<CommandSourceStack> = literal("delhome")
        .requires { it.isPlayer }
        .then(argument("name", HomeArgumentType.Home())
            .requires { it.hasPermission(4) }
            .suggests(HomeArgumentType::Suggest)
            .executes {
                HomeCommand.Delete(
                    it.source,
                    it.source.playerOrException,
                    HomeArgumentType.Resolve(it, "name")
                )
            }
        )
        .executes {
            HomeCommand.DeleteDefault(
                it.source,
                it.source.playerOrException
            )
        }

    private fun DiscardCommand(): LiteralArgumentBuilder<CommandSourceStack> = literal("discard")
        .requires { it.hasPermission(4) }
        .then(argument("entity", EntityArgument.entities())
            .executes { DiscardCommand.Execute(it.source, EntityArgument.getEntities(it, "entity")) }
        )

    private fun DisplayCommand(): LiteralArgumentBuilder<CommandSourceStack> = literal("display")
        .requires { it.hasPermission(4) }
        .then(literal("clear")
            .then(argument("players", EntityArgument.players())
                .executes { DisplayCommand.Clear(
                    it.source,
                    EntityArgument.getPlayers(it, "players")
                ) }
            )
        )
        .then(literal("set")
            .then(argument("players", EntityArgument.players())
                .then(argument("display", DisplayArgumentType.Display())
                    .suggests(DisplayArgumentType::Suggest)
                    .executes { DisplayCommand.SetDisplay(
                        it.source,
                        EntityArgument.getPlayers(it, "players"),
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
    private fun DiscordCommand(): LiteralArgumentBuilder<CommandSourceStack> = literal("discord")
        .then(literal("force-link")
            .requires { it.hasPermission(4) }
            .then(argument("player", EntityArgument.player())
                .then(argument("id", LongArgumentType.longArg())
                    .executes {
                        org.nguh.nguhcraft.server.dedicated.DiscordCommand.ForceLink(
                            it.source,
                            EntityArgument.getPlayer(it, "player"),
                            LongArgumentType.getLong(it, "id")
                        )
                    }
                )
            )
        )
        .then(literal("link")
            .requires { it.isPlayer && !(it.entity as ServerPlayer).Data.IsLinked }
            .then(argument("id", LongArgumentType.longArg())
                .executes {
                    org.nguh.nguhcraft.server.dedicated.DiscordCommand.TryLink(
                        it.source,
                        it.source.playerOrException,
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
            .then(argument("player", EntityArgument.player())
                .requires { it.HasModeratorPermissions }
                .executes {
                    org.nguh.nguhcraft.server.dedicated.DiscordCommand.TryUnlink(
                        it.source,
                        EntityArgument.getPlayer(it, "player")
                    )
                }
            )
            .requires {
                (it.isPlayer && (it.entity as ServerPlayer).Data.IsLinked) ||
                it.hasPermission(4)
            }
            .executes {
                org.nguh.nguhcraft.server.dedicated.DiscordCommand.TryUnlink(
                    it.source,
                    it.source.playerOrException
                )
            }
        )

    private fun EnchantCommand(A: CommandBuildContext): LiteralArgumentBuilder<CommandSourceStack> = literal("enchant")
        .requires { it.hasPermission(4) }
        .then(
            argument("enchantment", ResourceArgument.resource(A, Registries.ENCHANTMENT))
                .then(argument("level", IntegerArgumentType.integer())
                    .executes {
                        EnchantCommand.Enchant(
                            it.source,
                            it.source.playerOrException,
                            ResourceArgument.getEnchantment(it, "enchantment"),
                            IntegerArgumentType.getInteger(it, "level")
                        )
                    }
                )
                .executes {
                    EnchantCommand.Enchant(
                        it.source,
                        it.source.playerOrException,
                        ResourceArgument.getEnchantment(it, "enchantment"),
                        1
                    )
                }
        )

    private fun EntityCountCommand(): LiteralArgumentBuilder<CommandSourceStack> = literal("entity_count")
        .requires { it.hasPermission(4) }
        .then(argument("selector", EntityArgument.entities())
            .executes {
                val E = EntityArgument.getEntities(it, "selector")
                it.source.Reply("There are ${E.size} entities that match the given selector.")
                E.size
            }
        )

    fun EventCommand(): LiteralArgumentBuilder<CommandSourceStack> = literal("event")
        .requires { it.hasPermission(2) }
        .then(literal("add-player")
            .then(argument("player", EntityArgument.player())
                .executes { EventCommand.AddPlayer(it.source, EntityArgument.getPlayer(it, "player")) }
            )
        )
        .then(literal("difficulty")
            .executes { it.source.Reply("The current difficulty is: ${it.source.server.EventManager.Difficulty.name}"); 1 }
            .also {
                for (E in EventDifficulty.entries) it.then(literal(E.name)
                    .executes { EventCommand.SetDifficulty(it.source, E) }
                )
            }
        )
        .then(literal("list-players")
            .executes { EventCommand.ListPlayers(it.source) }
        )
        .then(literal("remove-player")
            .then(argument("player", EntityArgument.player())
                .executes { EventCommand.RemovePlayer(it.source, EntityArgument.getPlayer(it, "player")) }
            )
        )
        .then(literal("spawn")
            .requires { it.isPlayer }
            .then(argument("mob", MobArgumentType.Mob())
                .then(argument("where", Vec3Argument.vec3())
                    .executes { EventCommand.SpawnEventMob(
                        it.source,
                        MobArgumentType.Resolve(it, "mob"),
                        Vec3Argument.getVec3(it, "where")
                    ) }
                )
            )
        )
        .then(literal("spawn-test")
            .requires { it.isPlayer }
            .then(argument("mob", MobArgumentType.Mob())
                .executes { EventCommand.SpawnEventMobTesting(it.source, MobArgumentType.Resolve(it, "mob")) }
            )
        )

    private fun FixCommand(): LiteralArgumentBuilder<CommandSourceStack> = literal("fix")
        .requires { it.isPlayer && it.hasPermission(4) }
        .then(literal("all").executes { FixCommand.FixAll(it.source, it.source.playerOrException) })
        .executes { FixCommand.Fix(it.source, it.source.playerOrException) }

    private fun HereCommand(): LiteralArgumentBuilder<CommandSourceStack> = literal("here")
        .requires { it.isPlayer }
        .executes {
            val P = it.source.playerOrException.blockPosition()
            Chat.DispatchMessage(it.source.server, it.source.playerOrException, "${P.x} ${P.y} ${P.z}")
            1
        }

    private fun HealCommand(): LiteralArgumentBuilder<CommandSourceStack> = literal("heal")
        .requires { it.hasPermission(2) }
        .then(argument("entities", EntityArgument.entities())
            .executes { HealCommand.Heal(it.source, EntityArgument.getEntities(it, "entities")) }
        )
        .executes { HealCommand.Heal(it.source, listOf(it.source.entityOrException)) }

    private fun HomeCommand(): LiteralArgumentBuilder<CommandSourceStack> = literal("home")
        .requires { it.isPlayer }
        .then(argument("home", HomeArgumentType.Home())
            .suggests(HomeArgumentType::Suggest)
            .executes {
                HomeCommand.Teleport(
                    it.source.playerOrException,
                    HomeArgumentType.Resolve(it, "home")
                )
            }
        )
        .executes { HomeCommand.TeleportToDefault(it.source.playerOrException) }

    private fun HomesCommand(): LiteralArgumentBuilder<CommandSourceStack> = literal("homes")
        .then(argument("player", EntityArgument.player())
            .requires { it.hasPermission(4) }
            .executes {
                HomeCommand.List(
                    it.source,
                    EntityArgument.getPlayer(it, "player")
                )
            }
        )
        .executes { HomeCommand.List(it.source, it.source.playerOrException) }

    private fun KeyCommand(): LiteralArgumentBuilder<CommandSourceStack> = literal("key")
        .requires { it.hasPermission(4) }
        .then(argument("key", StringArgumentType.string())
            .executes {
                KeyCommand.Generate(
                    it.source,
                    it.source.playerOrException,
                    StringArgumentType.getString(it, "key")
                )
            }
        )

    private fun MessageCommand(): LiteralArgumentBuilder<CommandSourceStack> = literal("msg")
        .then(argument("targets", EntityArgument.players())
            .then(argument("message", StringArgumentType.greedyString())
                .executes {
                    val Players = EntityArgument.getPlayers(it, "targets")
                    val Message = StringArgumentType.getString(it, "message")
                    Chat.SendPrivateMessage(it.source.player, Players, Message)
                    Players.size
                }
            )
        )

    private fun ModCommand(): LiteralArgumentBuilder<CommandSourceStack> = literal("mod")
        .requires { it.hasPermission(4) }
        .then(argument("player", EntityArgument.player())
            .executes {
                val S = it.source
                val SP = EntityArgument.getPlayer(it, "player")
                SP.Data.IsModerator = !SP.Data.IsModerator
                S.server.commands.sendCommands(SP)
                S.sendSystemMessage(
                    Component.literal("Player '${SP.displayName?.string}' is ${if (SP.Data.IsModerator) "now" else "no longer"} a moderator")
                    .withStyle(ChatFormatting.YELLOW)
                )
                1
            }
        )

    private fun ObliterateCommand(): LiteralArgumentBuilder<CommandSourceStack> = literal("obliterate")
        .requires { it.hasPermission(2) }
        .then(argument("players", EntityArgument.players())
            .executes {
                val Players = EntityArgument.getPlayers(it, "players")
                for (SP in Players) ServerUtils.Obliterate(SP)
                it.source.sendSystemMessage(Component.literal("Obliterated ${Players.size} players"))
                Players.size
            }
        )

    private fun ProcedureCommand(): LiteralArgumentBuilder<CommandSourceStack> = literal("procedure")
        .requires { it.hasPermission(4) } // Procedures should not be able to create themselves.
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

    private fun RegionCommand(): LiteralArgumentBuilder<CommandSourceStack> {
        val RegionFlagsNameNode = argument("region", RegionArgumentType.Region())
        Region.Flags.entries.forEach { Flag ->
            fun Set(C: CommandContext<CommandSourceStack>, Value: Boolean) = RegionCommand.SetFlag(
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
            .requires { it.hasPermission(4) }
            .then(literal("list")
                .then(literal("all").executes { RegionCommand.ListAllRegions(it.source) })
                .then(argument("world", DimensionArgument.dimension())
                    .executes {
                        RegionCommand.ListRegions(
                            it.source,
                            DimensionArgument.getDimension(it, "world")
                        )
                    }
                )
                .executes { RegionCommand.ListRegions(it.source, it.source.level) }
            )
            .then(literal("add")
                .then(argument("name", StringArgumentType.word())
                    .then(argument("from", ColumnPosArgument.columnPos())
                        .then(argument("to", ColumnPosArgument.columnPos())
                            .executes {
                                RegionCommand.AddRegion(
                                    it.source,
                                    it.source.level,
                                    StringArgumentType.getString(it, "name"),
                                    ColumnPosArgument.getColumnPos(it, "from"),
                                    ColumnPosArgument.getColumnPos(it, "to"),
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
                .executes { RegionCommand.PrintRegionInfo(it.source, it.source.playerOrException) }
            )
            .then(literal("flags").then(RegionFlagsNameNode))
    }

    private fun RenameCommand(A: CommandBuildContext): LiteralArgumentBuilder<CommandSourceStack>  = literal("rename")
        .requires { it.isPlayer && it.hasPermission(2) }
        .then(argument("name", ComponentArgument.textComponent(A))
            .executes { RenameCommand.Execute(it.source, ComponentArgument.getRawComponent(it, "name")) }
        )

    private fun RuleCommand(): LiteralArgumentBuilder<CommandSourceStack> {
        var Command = literal("rule").requires { it.hasPermission(4) }
        SyncedGameRule.entries.forEach { Rule ->
            Command = Command.then(literal(Rule.Name)
                .then(argument("value", BoolArgumentType.bool())
                    .executes {
                        Rule.Set(it.source.server, BoolArgumentType.getBool(it, "value"))
                        it.source.sendSystemMessage(Component.literal("Set '${Rule.Name}' to ${Rule.IsSet()}"))
                        1
                    }
                )
                .executes {
                    it.source.sendSystemMessage(Component.literal("Rule '${Rule.Name}' is set to ${Rule.IsSet()}"))
                    1
                }
            )
        }
        return Command
    }

    private fun SayCommand(): LiteralArgumentBuilder<CommandSourceStack> = literal("say")
        .requires { it.hasPermission(2) }
        .then(argument("message", StringArgumentType.greedyString())
            .executes {
                Chat.SendServerMessage(it.source.server, StringArgumentType.getString(it, "message"))
                1
            }
        )

    private fun SetHomeCommand(): LiteralArgumentBuilder<CommandSourceStack> = literal("sethome")
        .requires { it.isPlayer }
        .then(argument("name", StringArgumentType.word())
            .requires { it.hasPermission(4) }
            .executes {
                HomeCommand.Set(
                    it.source,
                    it.source.playerOrException,
                    StringArgumentType.getString(it, "name")
                )
            }
        )
        .executes {
            HomeCommand.Set(
                it.source,
                it.source.playerOrException,
                Home.DEFAULT_HOME
            )
        }

    private fun SmiteCommand(): LiteralArgumentBuilder<CommandSourceStack> = literal("smite")
        .requires { it.hasPermission(2) }
        .then(argument("targets", EntityArgument.entities())
            .executes {
                // Smite everything that isn’t also lightning, because that becomes
                // exponential *really* quick...
                val Entities = EntityArgument.getEntities(it, "targets")
                for (E in Entities)
                    if (E !is LightningBolt)
                        StrikeLightning(E.level() as ServerLevel, E.position())

                // And tell the user how many things were smitten.
                it.source.sendSystemMessage(Component.literal(
                    if (Entities.size == 1) "${Entities.first().scoreboardName} has been smitten"
                    else "${Entities.size} entities have been smitten"
                ).withStyle(ChatFormatting.YELLOW))
                Entities.size
            }
        )
        .then(argument("where", BlockPosArgument.blockPos())
            .requires { it.isPlayer }
            .executes {
                val Pos = BlockPosArgument.getBlockPos(it, "where")
                StrikeLightning(it.source.level as ServerLevel, Vec3.atBottomCenterOf(Pos))
                it.source.sendSystemMessage(Component.literal("[$Pos] has been smitten").withStyle(ChatFormatting.YELLOW))
                1
            }
        )

    private fun SpawnsCommand(A: CommandBuildContext): LiteralArgumentBuilder<CommandSourceStack> = literal("spawns")
        .requires { it.hasPermission(4) && it.isPlayer }
        .executes { SpawnsCommands.ListSpawns(it.source) }
        .then(literal("add")
            .then(argument("id", StringArgumentType.word())
                .then(argument("entity", ResourceArgument.resource(A, Registries.ENTITY_TYPE))
                    .then(argument("pos", Vec3Argument.vec3())
                        .executes {
                            SpawnsCommands.AddSpawn(
                                it.source,
                                it.source.playerOrException.level().dimension(),
                                Vec3Argument.getVec3(it, "pos"),
                                StringArgumentType.getString(it, "id"),
                                ResourceArgument.getSummonableEntityType(it, "entity")
                            )
                        }
                        .then(argument("nbt", CompoundTagArgument.compoundTag())
                            .executes {
                                SpawnsCommands.AddSpawn(
                                    it.source,
                                    it.source.playerOrException.level().dimension(),
                                    Vec3Argument.getVec3(it, "pos"),
                                    StringArgumentType.getString(it, "id"),
                                    ResourceArgument.getSummonableEntityType(it, "entity"),
                                    CompoundTagArgument.getCompoundTag(it, "nbt")
                                )
                            }
                        )
                    )
                )
            )
        )
        .then(literal("del-nearest").executes {
            SpawnsCommands.DeleteNearest(it.source)
        })
        .then(literal("find-nearest").executes {
            SpawnsCommands.FindNearest(it.source)
        })
        .then(literal("tp-nearest").executes {
            SpawnsCommands.TeleportNearest(it.source)
        })

    private fun SpeedCommand(): LiteralArgumentBuilder<CommandSourceStack> = literal("speed")
        .requires { it.hasPermission(4) && it.isPlayer }
        .then(argument("value", IntegerArgumentType.integer())
            .executes {
                SpeedCommand.Execute(
                    it.source,
                    IntegerArgumentType.getInteger(it, "value")
                )
            }
        )

    private fun SubscribeToConsoleCommand(): LiteralArgumentBuilder<CommandSourceStack> = literal("subscribe_to_console")
        .requires { it.isPlayer && it.hasPermission(4) }
        .executes {
            val SP = it.source.playerOrException
            SP.Data.IsSubscribedToConsole = !SP.Data.IsSubscribedToConsole
            it.source.sendSystemMessage(Component.literal(
                "You are ${if (SP.Data.IsSubscribedToConsole) "now" else "no longer"} receiving console messages"
            ).withStyle(ChatFormatting.YELLOW))
            1
        }

    private fun TopCommand(): LiteralArgumentBuilder<CommandSourceStack> = literal("top")
        .requires { it.hasPermission(4) && it.isPlayer }
        .executes {
            val SP = it.source.playerOrException
            val SW = SP.level()
            val TopY = SW.getHeight(Heightmap.Types.WORLD_SURFACE, SP.x.toInt(), SP.z.toInt()) - 1

            // Make sure this doesn’t put us in the void.
            if (TopY <= SW.minY) return@executes 0

            // Make sure the block is solid.
            val Pos = BlockPos(SP.x.toInt(), TopY, SP.z.toInt())
            val St = SW.getBlockState(Pos)
            if (!St.isAir) SP.Teleport(SW, Pos, true)
            else it.source.sendFailure(Component.literal("Couldn’t find a suitable location to teleport to!"))
            1
        }

    @Environment(EnvType.SERVER)
    private fun UpdateBotCommandsCommand(): LiteralArgumentBuilder<CommandSourceStack> = literal("update_bot_commands")
        .requires { it.hasPermission(4) && !it.isPlayer }
        .executes { org.nguh.nguhcraft.server.dedicated.Discord.RegisterCommands(); 0 }

    private fun UUIDCommand(): LiteralArgumentBuilder<CommandSourceStack> = literal("uuid")
        .then(argument("player", EntityArgument.player())
            .requires { it.hasPermission(4) }
            .executes {
                val Player = EntityArgument.getPlayer(it, "player")
                it.source.sendSystemMessage(Component.literal("UUID: ${Player.uuid}"))
                1
            }
        )
        .executes {
            it.source.sendSystemMessage(Component.literal("Your UUID: ${it.source.playerOrException.uuid}"))
            1
        }

    @Environment(EnvType.SERVER)
    private fun VanishCommand(): LiteralArgumentBuilder<CommandSourceStack> = literal("vanish")
        .requires { it.isPlayer && it.hasPermission(4) }
        .executes {
            val SP = it.source.playerOrException
            Vanish.Toggle(SP)
            it.source.sendSystemMessage(Component.literal(
                "You are ${if (SP.Data.Vanished) "now" else "no longer"} vanished"
            ).withStyle(ChatFormatting.YELLOW))
            1
        }

    private fun WarpCommand(): LiteralArgumentBuilder<CommandSourceStack> = literal("warp")
        .requires { it.isPlayer }
        .then(argument("warp", WarpArgumentType.Warp())
            .suggests(WarpArgumentType::Suggest)
            .executes {
                val W = WarpArgumentType.Resolve(it, "warp")
                val SP = it.source.playerOrException
                SP.Teleport(SP.Server.getLevel(W.World)!!, W.Pos, W.Yaw, W.Pitch, true)
                1
            }
        )

    private fun WarpsCommand(): LiteralArgumentBuilder<CommandSourceStack> = literal("warps")
        .requires { it.isPlayer }
        .then(literal("set")
            .requires { it.hasPermission(4) }
            .then(argument("warp", StringArgumentType.word())
                .executes {
                    WarpsCommand.Set(
                        it.source,
                        it.source.playerOrException,
                        StringArgumentType.getString(it, "warp")
                    )
                }
            )
        )
        .then(literal("del")
            .requires { it.hasPermission(4) }
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
}