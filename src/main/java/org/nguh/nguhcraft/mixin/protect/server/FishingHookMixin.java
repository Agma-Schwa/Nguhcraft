package org.nguh.nguhcraft.mixin.protect.server;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.Nullable;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FishingHook.class)
public abstract class FishingHookMixin {
    @Shadow public abstract @Nullable Player getPlayerOwner();

    @Inject(method = "onHitEntity", at = @At("HEAD"), cancellable = true)
    private void inject$onEntityHit(EntityHitResult EHR, CallbackInfo CI) {
        var PE = getPlayerOwner();
        if (PE == null) return;
        if (!ProtectionManager.AllowEntityAttack(PE, EHR.getEntity())) CI.cancel();
    }
}
