package org.nguh.nguhcraft.mixin.common;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.core.Holder;
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
            target = "Lnet/minecraft/world/entity/LivingEntity;getAttributeValue(Lnet/minecraft/core/Holder;)D"
        )
    )
    private double inject$travelInFluid(
            LivingEntity I,
            Holder<Attribute> A
    ) {
        if (I.isAutoSpinAttack() && A == Attributes.WATER_MOVEMENT_EFFICIENCY) return 0;
        return I.getAttributeValue(A);
    }
}
