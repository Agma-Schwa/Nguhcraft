package org.nguh.nguhcraft.event

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.server.level.ServerLevel
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import org.nguh.nguhcraft.entity.Parameters
import org.nguh.nguhcraft.event.NguhMobs.DoSpawn

enum class NguhMobType(private val Factory: (SW: ServerLevel, Where: Vec3, D: EventDifficulty) -> Entity?) {
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
        SW: ServerLevel,
        Where: Vec3,
        D: EventDifficulty = SW.server.EventManager.Difficulty
    ) = Factory(SW, Where, D)
}

object NguhMobs {
    internal inline fun<reified T : Entity> DoSpawn(
        Type: EntityType<T>,
        noinline Transform: (T.() -> Unit)? = null
    ): (SW: ServerLevel, Where: Vec3, D: EventDifficulty) -> Entity? = {
            SW: ServerLevel,
            Where: Vec3,
            D: EventDifficulty

        ->

        fun Update(E: T) {
            E.snapTo(Where.x, Where.y, Where.z, E.yRot, E.xRot)
            if (E is LivingEntity) Parameters.BY_TYPE[Type]?.Apply(E, D)
            Transform?.invoke(E)
        }

        Type.spawn(SW, ::Update, BlockPos.containing(Where), EntitySpawnReason.COMMAND, true, false)
    }
}