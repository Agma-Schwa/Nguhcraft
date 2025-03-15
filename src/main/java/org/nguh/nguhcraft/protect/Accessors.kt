package org.nguh.nguhcraft.protect

import net.minecraft.entity.LivingEntity
import net.minecraft.entity.SpawnReason

interface SpawnReasonAccessor {
    fun `Nguhcraft$SetSpawnReason`(R: SpawnReason)
    fun `Nguhcraft$GetSpawnReason`(): SpawnReason?
}

val LivingEntity.SpawnReason get() = (this as SpawnReasonAccessor).`Nguhcraft$GetSpawnReason`()