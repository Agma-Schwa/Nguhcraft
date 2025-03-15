package org.nguh.nguhcraft.mixin.protect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import org.jetbrains.annotations.NotNull;
import org.nguh.nguhcraft.protect.SpawnReasonAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements SpawnReasonAccessor {
    @Unique private SpawnReason Reason;
    @Override public SpawnReason Nguhcraft$GetSpawnReason() { return Reason; }
    @Override public void Nguhcraft$SetSpawnReason(@NotNull SpawnReason R) { Reason = R; }
}
