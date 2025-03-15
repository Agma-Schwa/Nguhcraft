package org.nguh.nguhcraft.mixin.protect;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.world.World;
import org.nguh.nguhcraft.protect.SpawnReasonAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityType.class)
public abstract class EntityTypeMixin {
    /** Save spawn reason for living entities. */
    @ModifyReturnValue(
        method = "create(Lnet/minecraft/world/World;Lnet/minecraft/entity/SpawnReason;)Lnet/minecraft/entity/Entity;",
        at = @At("RETURN")
    )
    private <T extends Entity> T inject$create(T E, World W, SpawnReason R) {
        if (E instanceof LivingEntity) ((SpawnReasonAccessor)E).Nguhcraft$SetSpawnReason(R);
        return E;
    }
}
