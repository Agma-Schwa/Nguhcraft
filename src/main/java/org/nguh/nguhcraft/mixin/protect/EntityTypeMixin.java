package org.nguh.nguhcraft.mixin.protect;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import org.nguh.nguhcraft.protect.SpawnReasonAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityType.class)
public abstract class EntityTypeMixin {
    /** Save spawn reason for living entities. */
    @ModifyReturnValue(
        method = "create(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/EntitySpawnReason;)Lnet/minecraft/world/entity/Entity;",
        at = @At("RETURN")
    )
    private <T extends Entity> T inject$create(T E, Level W, EntitySpawnReason R) {
        if (E instanceof LivingEntity) ((SpawnReasonAccessor)E).Nguhcraft$SetSpawnReason(R);
        return E;
    }
}
