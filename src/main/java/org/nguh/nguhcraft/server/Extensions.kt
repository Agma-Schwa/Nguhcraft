package org.nguh.nguhcraft.server

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.block.entity.LockableContainerBlockEntity
import net.minecraft.entity.Entity
import net.minecraft.inventory.ContainerLock
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.world.TeleportTarget
import org.nguh.nguhcraft.mixin.protect.LockableContainerBlockEntityAccessor
import org.nguh.nguhcraft.server.accessors.ServerPlayerAccessor

val ServerPlayerEntity.isLinked get() = (this as ServerPlayerAccessor).isLinked
val ServerPlayerEntity.isOperator get() = hasPermissionLevel(4)
val ServerPlayerEntity.isLinkedOrOperator get() = isLinked || isOperator

var ServerPlayerEntity.discordId
    get() = (this as ServerPlayerAccessor).discordId
    set(value) { (this as ServerPlayerAccessor).discordId = value }
var ServerPlayerEntity.discordColour
    get() = (this as ServerPlayerAccessor).discordColour
    set(value) { (this as ServerPlayerAccessor).discordColour = value }
var ServerPlayerEntity.discordName: String?
    get() = (this as ServerPlayerAccessor).discordName
    set(value) { (this as ServerPlayerAccessor).discordName = value }
var ServerPlayerEntity.discordAvatarURL: String?
    get() = (this as ServerPlayerAccessor).discordAvatarURL
    set(value) { (this as ServerPlayerAccessor).discordAvatarURL = value }
var ServerPlayerEntity.discordDisplayName: Text?
    get() = (this as ServerPlayerAccessor).nguhcraftDisplayName
    set(value) { (this as ServerPlayerAccessor).nguhcraftDisplayName = value }

@Environment(EnvType.SERVER)
fun Entity.Teleport(ToWorld: ServerWorld, OnTopOf: BlockPos) {
    val Vec = Vec3d(OnTopOf.x.toDouble(), OnTopOf.y.toDouble() + 1, OnTopOf.z.toDouble())
    teleportTo(TeleportTarget(ToWorld, Vec, Vec3d.ZERO, 0F, 0F, TeleportTarget.NO_OP))
}

@Environment(EnvType.SERVER)
fun LockableContainerBlockEntity.UpdateLock(NewLock: ContainerLock) {
    (this as LockableContainerBlockEntityAccessor).lock = NewLock
    (world as ServerWorld).chunkManager.markForUpdate(pos)
    markDirty()
}