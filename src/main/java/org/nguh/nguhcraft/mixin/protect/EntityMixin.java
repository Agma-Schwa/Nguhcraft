package org.nguh.nguhcraft.mixin.protect;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import com.google.common.collect.ImmutableList.Builder;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Shadow public abstract boolean isOnFire();
    @Shadow public abstract void clearFire();

    @Unique private Entity This() { return (Entity)(Object)this; }

    /** Handle collisions with regions. */
    @Inject(
        method = "collectColliders",
        at = @At(
            value = "INVOKE",
            remap = false,
            target = "Lcom/google/common/collect/ImmutableList$Builder;addAll(Ljava/lang/Iterable;)Lcom/google/common/collect/ImmutableList$Builder;",
            ordinal = 1
        )
    )
    static private void inject$findCollisionsForMovement(
        @Nullable Entity E,
        Level W,
        List<VoxelShape> Colls,
        AABB BB,
        CallbackInfoReturnable<List<VoxelShape>> CIR,
        @Local Builder<VoxelShape> Builder
    ) {
        if (E == null) return;
        ProtectionManager.GetCollisionsForEntity(W, E, BB, Builder::addAll);
    }

    /** Prevent damage to protected entities. */
    @Inject(method = "isInvulnerableToBase", at = @At("HEAD"), cancellable = true)
    private void inject$isAlwaysInvulnerableTo(DamageSource DS, CallbackInfoReturnable<Boolean> CIR) {
        if (ProtectionManager.IsProtectedEntity(This(), DS))
            CIR.setReturnValue(true);
    }

    /** Clear fire ticks if we’re in a protected region. */
    @Inject(method = "tick", at = @At("HEAD"))
    private void inject$tick(CallbackInfo CI) {
        if (isOnFire() && ProtectionManager.IsProtectedEntity(This()))
            clearFire();
    }
}
