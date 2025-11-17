package org.nguh.nguhcraft.mixin.protect;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerEntityMixin extends LivingEntity {
    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Unique private Player This() { return (Player) (Object) this; }

    /** Prevent players from attacking certain entities. */
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void inject$attack$0(Entity Target, CallbackInfo CI) {
        if (!ProtectionManager.AllowEntityAttack(This(), Target))
            CI.cancel();
    }

    /** Prevent interactions within a region. */
    @Inject(method = "canInteractWithBlock", at = @At("HEAD"), cancellable = true)
    private void inject$canInteractWithBlockAt(
            BlockPos Pos,
            double Range,
            CallbackInfoReturnable<Boolean> CIR
    ) {
        // This acts as a server-side gate to prevent block interactions. On
        // the client, they should have already been rewritten to item uses.
        if (!ProtectionManager.HandleBlockInteract(This(), level(), Pos, getMainHandItem()).consumesAction())
            CIR.setReturnValue(false);
    }

    /** Prevent fall damage in certain regions. */
    @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
    private void inject$handleFallDamage(double FD, float DM, DamageSource DS, CallbackInfoReturnable<Boolean> CIR) {
        if (!ProtectionManager.AllowFallDamage(This()))
            CIR.setReturnValue(false);
    }
}
