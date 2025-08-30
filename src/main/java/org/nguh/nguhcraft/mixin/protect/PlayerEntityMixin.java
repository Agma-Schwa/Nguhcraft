package org.nguh.nguhcraft.mixin.protect;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity {
    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Unique private PlayerEntity This() { return (PlayerEntity) (Object) this; }

    /** Prevent players from attacking certain entities. */
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void inject$attack$0(Entity Target, CallbackInfo CI) {
        if (!ProtectionManager.AllowEntityAttack(This(), Target))
            CI.cancel();
    }

    /** Prevent interactions within a region. */
    @Inject(method = "canInteractWithBlockAt", at = @At("HEAD"), cancellable = true)
    private void inject$canInteractWithBlockAt(
        BlockPos Pos,
        double Range,
        CallbackInfoReturnable<Boolean> CIR
    ) {
        // This acts as a server-side gate to prevent block interactions. On
        // the client, they should have already been rewritten to item uses.
        if (!ProtectionManager.HandleBlockInteract(This(), getWorld(), Pos, getMainHandStack()).isAccepted())
            CIR.setReturnValue(false);
    }

    /** Prevent fall damage in certain regions. */
    @Inject(method = "handleFallDamage", at = @At("HEAD"), cancellable = true)
    private void inject$handleFallDamage(double FD, float DM, DamageSource DS, CallbackInfoReturnable<Boolean> CIR) {
        if (!ProtectionManager.AllowFallDamage(This()))
            CIR.setReturnValue(false);
    }
}
