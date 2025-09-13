package org.nguh.nguhcraft.server

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.entity.Entity
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.packet.CustomPayload
import net.minecraft.network.packet.Packet
import net.minecraft.screen.ScreenTexts
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.world.TeleportTarget
import org.nguh.nguhcraft.Nbt
import org.nguh.nguhcraft.network.ClientFlags
import org.nguh.nguhcraft.network.ClientboundSyncFlagPacket
import org.nguh.nguhcraft.set
import java.util.*

fun CreateUpdateBlockEntityUpdatePacket(Update: NbtCompound.() -> Unit) = Nbt {
    Update()

    // An empty tag prevents deserialisation on the client, so
    // ensure that this is never empty.
    if (isEmpty) set("nguhcraft_ensure_deserialised", true)
}

fun Entity.Teleport(
    ToWorld: ServerWorld,
    OnTopOf: BlockPos,
    SaveLastPos: Boolean = false
) {
    val Vec = Vec3d(OnTopOf.x.toDouble(), OnTopOf.y.toDouble() + 1, OnTopOf.z.toDouble())
    Teleport(TeleportTarget(ToWorld, Vec, Vec3d.ZERO, 0F, 0F, TeleportTarget.NO_OP), SaveLastPos)
}

fun Entity.Teleport(
    ToWorld: ServerWorld,
    To: Vec3d,
    SaveLastPos: Boolean = false
) {
    Teleport(TeleportTarget(ToWorld, To, Vec3d.ZERO, yaw, pitch, TeleportTarget.NO_OP), SaveLastPos)
}

fun Entity.Teleport(
    ToWorld: ServerWorld,
    To: Vec3d,
    Yaw: Float,
    Pitch: Float,
    SaveLastPos: Boolean = false
) {
    Teleport(TeleportTarget(ToWorld, To, Vec3d.ZERO, Yaw, Pitch, TeleportTarget.NO_OP), SaveLastPos)
}

fun Entity.Teleport(Target: TeleportTarget, SaveLastPos: Boolean) {
    if (SaveLastPos && this is ServerPlayerEntity) SavePositionBeforeTeleport()
    teleportTo(Target)
}

/** Send a packet to every client except one. */
fun MinecraftServer.Broadcast(Except: ServerPlayerEntity, P: CustomPayload) {
    for (Player in playerManager.playerList)
        if (Player != Except)
            ServerPlayNetworking.send(Player, P)
}

fun MinecraftServer.Broadcast(Except: ServerPlayerEntity, P: Packet<*>) {
    for (Player in playerManager.playerList)
        if (Player != Except)
            Player.networkHandler.sendPacket(P)
}


/** Send a packet to every client in a world. */
fun MinecraftServer.Broadcast(W: ServerWorld, P: CustomPayload) {
    for (Player in W.players)
        ServerPlayNetworking.send(Player, P)
}

fun MinecraftServer.Broadcast(W: ServerWorld, P: Packet<*>) {
    for (Player in W.players)
        Player.networkHandler.sendPacket(P)
}

/** Send a packet to every client. */
fun MinecraftServer.Broadcast(P: CustomPayload) {
    for (Player in playerManager.playerList)
        ServerPlayNetworking.send(Player, P)
}

fun MinecraftServer.Broadcast(P: Packet<*>) {
    for (Player in playerManager.playerList)
        Player.networkHandler.sendPacket(P)
}

fun MinecraftServer.Broadcast(Msg: Text) {
    playerManager.broadcast(Msg, false)
}

/** Broadcast a message in the operator chat. */
fun MinecraftServer.BroadcastToOperators(Msg: Text, Except: ServerPlayerEntity? = null) {
    val Decorated = Text.empty()
        .append(Chat.SERVER_COMPONENT)
        .append(ScreenTexts.SPACE)
        .append(Msg)

    for (P in playerManager.playerList)
        if (P != Except && P.Data.IsSubscribedToConsole && P.hasPermissionLevel(4))
            P.sendMessage(Decorated, false)
}

/** Get a player by Name. */
fun MinecraftServer.PlayerByName(Name: String): ServerPlayerEntity? {
    return playerManager.getPlayer(Name)
}

/** Get a player by UUID. */
fun MinecraftServer.PlayerByUUID(ID: String?): ServerPlayerEntity? {
    return try { playerManager.getPlayer(UUID.fromString(ID)) }
    catch (E: RuntimeException) { null }
}

/** Save the player’s current position as a teleport target. */
fun ServerPlayerEntity.SavePositionBeforeTeleport() {
    Data.LastPositionBeforeTeleport = SerialisedTeleportTarget(
        this.world.registryKey,
        X = this.pos.x,
        Y = this.pos.y,
        Z = this.pos.z,
        Yaw = this.yaw,
        Pitch = this.pitch,
    )
}

fun ServerPlayerEntity.SetClientFlag(F: ClientFlags, V: Boolean) {
    ServerPlayNetworking.send(this, ClientboundSyncFlagPacket(F, V))
}

/**
 * This is never null for server players, but the 'server' accessor
 * ends up being that of 'Entity', so we get bogus ‘server can be
 * null’ warnings everywhere if we use that.
 */
val ServerPlayerEntity.Server get(): MinecraftServer = this.server!!