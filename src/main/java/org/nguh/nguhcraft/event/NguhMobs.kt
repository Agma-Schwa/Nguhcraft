package org.nguh.nguhcraft.event

import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.SpawnReason
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import org.nguh.nguhcraft.entity.Parameters
import org.nguh.nguhcraft.event.NguhMobs.DoSpawn

enum class NguhMobType(private val Factory: (SW: ServerWorld, Where: Vec3d, D: EventDifficulty) -> Entity?) {
    // Default mobs for the event.
    BOGGED(DoSpawn(EntityType.BOGGED)),
    CREEPER(DoSpawn(EntityType.CREEPER)),
    DROWNED(DoSpawn(EntityType.DROWNED)),
    GHAST(DoSpawn(EntityType.GHAST)),
    SKELETON(DoSpawn(EntityType.SKELETON)),
    STRAY(DoSpawn(EntityType.STRAY)),
    VINDICATOR(DoSpawn(EntityType.VINDICATOR)),
    ZOMBIE(DoSpawn(EntityType.ZOMBIE));

    fun Spawn(
        SW: ServerWorld,
        Where: Vec3d,
        D: EventDifficulty = SW.server.EventManager.Difficulty
    ) = Factory(SW, Where, D)
}

object NguhMobs {
    internal inline fun<reified T : Entity> DoSpawn(
        Type: EntityType<T>,
        noinline Transform: (T.() -> Unit)? = null
    ): (SW: ServerWorld, Where: Vec3d, D: EventDifficulty) -> Entity? = {
            SW: ServerWorld,
            Where: Vec3d,
            D: EventDifficulty

        ->

        fun Update(E: T) {
            E.refreshPositionAndAngles(Where.x, Where.y, Where.z, E.yaw, E.pitch)
            if (E is LivingEntity) Parameters.BY_TYPE[Type]?.Apply(E, D)
            Transform?.invoke(E)
        }

        Type.spawn(SW, ::Update, BlockPos.ofFloored(Where), SpawnReason.COMMAND, true, false)
    }
}