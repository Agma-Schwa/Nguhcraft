package org.nguh.nguhcraft.mixin.entity;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.mob.GhastEntity;
import net.minecraft.entity.projectile.FireballEntity;
import org.nguh.nguhcraft.entity.GhastModeAccessor;
import org.nguh.nguhcraft.entity.MachineGunGhastMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.entity.mob.GhastEntity$ShootFireballGoal")
public abstract class GhastEntity_ShootFireballGoalMixin {
    @Shadow @Final private GhastEntity ghast;

    /** Reset cooldown. */
    @ModifyConstant(method = "tick", constant = @Constant(intValue = -40))
    private int inject$tick$0(int constant) {
        var G = (GhastModeAccessor) ghast;
        return G.Nguhcraft$GetGhastMode().CooldownReset;
    }

    /** Speed up fireballs. */
    @Inject(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/World;spawnEntity(Lnet/minecraft/entity/Entity;)Z"
        )
    )
    private void inject$tick$1(CallbackInfo CI, @Local FireballEntity FB) {
        var G = (GhastModeAccessor) ghast;
        var M = G.Nguhcraft$GetGhastMode().ordinal();
        var S = MachineGunGhastMode.FASTER.ordinal();
        if (M >= S) FB.setVelocity(FB.getVelocity().multiply(8 * (M - S + 1)));
    }
}