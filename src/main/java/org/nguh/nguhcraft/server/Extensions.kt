package org.nguh.nguhcraft.server

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.commands.CommandSourceStack
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.network.protocol.Packet
import net.minecraft.network.chat.CommonComponents
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.permissions.LevelBasedPermissionSet
import net.minecraft.server.permissions.Permission
import net.minecraft.server.permissions.PermissionSet
import net.minecraft.server.permissions.Permissions
import net.minecraft.network.chat.Component
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import net.minecraft.world.level.portal.TeleportTransition
import org.nguh.nguhcraft.Nbt
import org.nguh.nguhcraft.network.ClientFlags
import org.nguh.nguhcraft.network.ClientboundSyncFlagPacket
import org.nguh.nguhcraft.set
import java.util.*

/** Map an integer operator level onto the new permission system. */
fun PermissionForLevel(Level: Int): Permission = when {
    Level >= 4 -> Permissions.COMMANDS_OWNER
    Level == 3 -> Permissions.COMMANDS_ADMIN
    Level == 2 -> Permissions.COMMANDS_GAMEMASTER
    else -> Permissions.COMMANDS_MODERATOR
}

/** Compatibility function for the old integer permission check. */
fun CommandSourceStack.hasPermission(Level: Int): Boolean = permissions().hasPermission(PermissionForLevel(Level))

/** Compatibility function for the old integer permission check. */
fun Player.hasPermissions(Level: Int): Boolean = permissions().hasPermission(PermissionForLevel(Level))

/** Build a PermissionSet equivalent to the old integer permission check. */
fun PermissionSetForLevel(Level: Int): PermissionSet = LevelBasedPermissionSet.forLevel(
    when {
        Level >= 4 -> net.minecraft.server.permissions.PermissionLevel.OWNERS
        Level == 3 -> net.minecraft.server.permissions.PermissionLevel.ADMINS
        Level == 2 -> net.minecraft.server.permissions.PermissionLevel.GAMEMASTERS
        else -> net.minecraft.server.permissions.PermissionLevel.MODERATORS
    }
)

fun CreateUpdateBlockEntityUpdatePacket(Update: CompoundTag.() -> Unit) = Nbt {
    Update()

    // An empty tag prevents deserialisation on the client, so
    // ensure that this is never empty.
    if (isEmpty) set("nguhcraft_ensure_deserialised", true)
}

fun Entity.Teleport(
    ToWorld: ServerLevel,
    OnTopOf: BlockPos,
    SaveLastPos: Boolean = false
) {
    val Vec = Vec3(OnTopOf.x.toDouble(), OnTopOf.y.toDouble() + 1, OnTopOf.z.toDouble())
    Teleport(TeleportTransition(ToWorld, Vec, Vec3.ZERO, 0F, 0F, TeleportTransition.DO_NOTHING), SaveLastPos)
}

fun Entity.Teleport(
    ToWorld: ServerLevel,
    To: Vec3,
    SaveLastPos: Boolean = false
) {
    Teleport(TeleportTransition(ToWorld, To, Vec3.ZERO, yRot, xRot, TeleportTransition.DO_NOTHING), SaveLastPos)
}

fun Entity.Teleport(
    ToWorld: ServerLevel,
    To: Vec3,
    Yaw: Float,
    Pitch: Float,
    SaveLastPos: Boolean = false
) {
    Teleport(TeleportTransition(ToWorld, To, Vec3.ZERO, Yaw, Pitch, TeleportTransition.DO_NOTHING), SaveLastPos)
}

fun Entity.Teleport(Target: TeleportTransition, SaveLastPos: Boolean) {
    if (SaveLastPos && this is ServerPlayer) SavePositionBeforeTeleport()
    teleport(Target)
}

/** Send a packet to every client except one. */
fun MinecraftServer.Broadcast(Except: ServerPlayer, P: CustomPacketPayload) {
    for (Player in playerList.players)
        if (Player != Except)
            ServerPlayNetworking.send(Player, P)
}

fun MinecraftServer.Broadcast(Except: ServerPlayer, P: Packet<*>) {
    for (Player in playerList.players)
        if (Player != Except)
            Player.connection.send(P)
}


/** Send a packet to every client in a world. */
fun MinecraftServer.Broadcast(W: ServerLevel, P: CustomPacketPayload) {
    for (Player in W.players())
        ServerPlayNetworking.send(Player, P)
}

fun MinecraftServer.Broadcast(W: ServerLevel, P: Packet<*>) {
    for (Player in W.players())
        Player.connection.send(P)
}

/** Send a packet to every client. */
fun MinecraftServer.Broadcast(P: CustomPacketPayload) {
    for (Player in playerList.players)
        ServerPlayNetworking.send(Player, P)
}

fun MinecraftServer.Broadcast(P: Packet<*>) {
    for (Player in playerList.players)
        Player.connection.send(P)
}

fun MinecraftServer.Broadcast(Msg: Component) {
    playerList.broadcastSystemMessage(Msg, false)
}

/** Broadcast a message in the operator chat. */
fun MinecraftServer.BroadcastToOperators(Msg: Component, Except: ServerPlayer? = null) {
    val Decorated = Component.empty()
        .append(Chat.SERVER_COMPONENT)
        .append(CommonComponents.SPACE)
        .append(Msg)

    for (P in playerList.players)
        if (P != Except && P.Data.IsSubscribedToConsole && P.hasPermissions(4))
            P.sendSystemMessage(Decorated, false)
}

/** Get a player by Name. */
fun MinecraftServer.PlayerByName(Name: String): ServerPlayer? {
    return playerList.getPlayerByName(Name)
}

/** Get a player by UUID. */
fun MinecraftServer.PlayerByUUID(ID: String?): ServerPlayer? {
    return try { playerList.getPlayer(UUID.fromString(ID)) }
    catch (E: RuntimeException) { null }
}

/** Save the player’s current position as a teleport target. */
fun ServerPlayer.SavePositionBeforeTeleport() {
    Data.LastPositionBeforeTeleport = SerialisedTeleportTarget(
        this.level().dimension(),
        X = this.position().x,
        Y = this.position().y,
        Z = this.position().z,
        Yaw = this.yRot,
        Pitch = this.xRot,
    )
}

fun ServerPlayer.SetClientFlag(F: ClientFlags, V: Boolean) {
    ServerPlayNetworking.send(this, ClientboundSyncFlagPacket(F, V))
}

/**
 * This is never null for server players, but the 'server' accessor
 * ends up being that of 'Entity', so we get bogus ‘server can be
 * null’ warnings everywhere if we use that.
 */
val ServerPlayer.Server get(): MinecraftServer = this.level().server