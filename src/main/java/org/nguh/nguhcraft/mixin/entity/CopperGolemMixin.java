package org.nguh.nguhcraft.mixin.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CopperGolem.class)
public abstract class CopperGolemMixin {
    /** Prevent copper golems from turning to statues or weathering in protected regions. */
    @Inject(method = "updateWeathering", at = @At("HEAD"), cancellable = true)
    private void inject$updateWeathering(ServerLevel SL, RandomSource Rng, long Time, CallbackInfo CI) {
        if (ProtectionManager.IsProtectedEntity((CopperGolem) (Object)this)) CI.cancel();
    }
}
