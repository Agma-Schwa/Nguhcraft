package org.nguh.nguhcraft.mixin.common;

import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RangedAttribute.class)
public abstract class RangedAttributeMixin {
    @Mutable @Shadow @Final private double maxValue;
    @Inject(method = "<init>", at = @At("TAIL"))
    void inject$ctor(String Key, double Fallback, double Min, double Max, CallbackInfo CI) {
        if ("attribute.name.armor".equals(Key)) maxValue = 40.;
    }
}
