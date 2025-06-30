package org.nguh.nguhcraft.mixin.common;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    /**
    * MC-136249
    * <p>
    * Prevent depth strider from adding drag when riptide is active by simply ignoring it.
    */
    @Redirect(
        method = "travelInFluid",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/LivingEntity;getAttributeValue(Lnet/minecraft/registry/entry/RegistryEntry;)D"
        )
    )
    private double inject$travelInFluid(
        LivingEntity I,
        RegistryEntry<EntityAttribute> A
    ) {
        if (I.isUsingRiptide() && A == EntityAttributes.WATER_MOVEMENT_EFFICIENCY) return 0;
        return I.getAttributeValue(A);
    }
}
