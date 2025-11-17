package org.nguh.nguhcraft.mixin.protect;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntitySpawnReason;
import org.jetbrains.annotations.NotNull;
import org.nguh.nguhcraft.protect.SpawnReasonAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements SpawnReasonAccessor {
    @Unique private EntitySpawnReason Reason;
    @Override public EntitySpawnReason Nguhcraft$GetSpawnReason() { return Reason; }
    @Override public void Nguhcraft$SetSpawnReason(@NotNull EntitySpawnReason R) { Reason = R; }
}
