package org.nguh.nguhcraft.mixin.protect.server;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractThrownPotion;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractThrownPotion.class)
public abstract class AbstractThrownPotionMixin extends ThrowableItemProjectile {
    public AbstractThrownPotionMixin(EntityType<? extends ThrowableItemProjectile> entityType, Level world) {
        super(entityType, world);
    }

    @Inject(method = "dowseFire", at = @At("HEAD"), cancellable = true)
    private void inject$extinguishFire(BlockPos Pos, CallbackInfo CI) {
        if (ProtectionManager.IsProtectedBlock(level(), Pos))
            CI.cancel();
    }
}
