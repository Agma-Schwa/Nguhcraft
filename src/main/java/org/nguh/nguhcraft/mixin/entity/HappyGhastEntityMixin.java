package org.nguh.nguhcraft.mixin.entity;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.passive.HappyGhastEntity;
import net.minecraft.registry.entry.RegistryEntry;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(HappyGhastEntity.class)
public abstract class HappyGhastEntityMixin {
    @Shadow public abstract @Nullable LivingEntity getControllingPassenger();

    /** Make happy ghasts faster if they are controlled by someone. */
    @Redirect(
        method = "travel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/passive/HappyGhastEntity;getAttributeValue(Lnet/minecraft/registry/entry/RegistryEntry;)D"
        )
    )
    private double inject$getAttributeValue(HappyGhastEntity G, RegistryEntry<EntityAttribute> RE) {
        var Value = G.getAttributeValue(RE);
        if (getControllingPassenger() != null) Value *= 4;
        return Value;
    }
}
