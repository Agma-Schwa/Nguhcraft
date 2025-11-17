package org.nguh.nguhcraft.mixin.protect.server;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.level.Level;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ThrownEgg.class)
public abstract class ThrownEggMixin extends ThrowableItemProjectile {
    public ThrownEggMixin(EntityType<? extends ThrowableItemProjectile> entityType, Level world) {
        super(entityType, world);
    }

    /** Prevent thrown eggs from spawning chickens (or someone might decide to be funny). */
    @ModifyExpressionValue(
        method = "onHit", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/RandomSource;nextInt(I)I",
            ordinal = 0
        )
    )
    private int inject$onCollision(int original) {
        // Zero here means that we spawn the chicken, so return 1 instead.
        if (original == 0 && ProtectionManager.IsProtectedBlock(level(), blockPosition())) return 1;
        return original;
    }
}
