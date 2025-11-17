package org.nguh.nguhcraft.mixin.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.animal.HappyGhast;
import net.minecraft.core.Holder;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(HappyGhast.class)
public abstract class HappyGhastMixin {
    @Shadow public abstract @Nullable LivingEntity getControllingPassenger();

    /** Make happy ghasts faster if they are controlled by someone. */
    @Redirect(
        method = "travel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/HappyGhast;getAttributeValue(Lnet/minecraft/core/Holder;)D"
        )
    )
    private double inject$getAttributeValue(HappyGhast G, Holder<Attribute> RE) {
        var Value = G.getAttributeValue(RE);
        if (getControllingPassenger() != null) Value *= 4;
        return Value;
    }
}
