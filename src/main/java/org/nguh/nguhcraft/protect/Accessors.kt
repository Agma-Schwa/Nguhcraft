package org.nguh.nguhcraft.protect

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.EntitySpawnReason

interface SpawnReasonAccessor {
    fun `Nguhcraft$SetSpawnReason`(R: EntitySpawnReason)
    fun `Nguhcraft$GetSpawnReason`(): EntitySpawnReason?
}

val LivingEntity.SpawnReason get() = (this as SpawnReasonAccessor).`Nguhcraft$GetSpawnReason`()