package org.nguh.nguhcraft.event

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.server.level.ServerLevel
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import org.nguh.nguhcraft.entity.Parameters
import org.nguh.nguhcraft.event.NguhMobs.DoSpawn

enum class NguhMobType(private val Factory: (SW: ServerLevel, Where: Vec3, D: EventDifficulty) -> Entity?) {
    // Default mobs for the event.
    BOGGED(DoSpawn(EntityTypes.BOGGED)),
    CREEPER(DoSpawn(EntityTypes.CREEPER)),
    DROWNED(DoSpawn(EntityTypes.DROWNED)),
    GHAST(DoSpawn(EntityTypes.GHAST)),
    SKELETON(DoSpawn(EntityTypes.SKELETON)),
    STRAY(DoSpawn(EntityTypes.STRAY)),
    VINDICATOR(DoSpawn(EntityTypes.VINDICATOR)),
    ZOMBIE(DoSpawn(EntityTypes.ZOMBIE));

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

        val Update: (T) -> Unit = { E ->
            E.snapTo(Where.x, Where.y, Where.z, E.yRot, E.xRot)
            if (E is LivingEntity) Parameters.BY_TYPE[Type]?.Apply(E, D)
            Transform?.invoke(E)
        }

        Type.spawn(SW, Update, BlockPos.containing(Where), EntitySpawnReason.COMMAND, true, false)
    }
}