package org.nguh.nguhcraft.mixin.entity;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.projectile.LargeFireball;
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

@Mixin(targets = "net.minecraft.world.entity.monster.Ghast$GhastShootFireballGoal")
public abstract class Ghast_GhastShootFireballGoalMixin {
    @Shadow @Final private Ghast ghast;


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
            target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"
        )
    )
    private void inject$tick$1(CallbackInfo CI, @Local LargeFireball FB) {
        var G = (GhastModeAccessor) ghast;
        var M = G.Nguhcraft$GetGhastMode().ordinal();
        var S = MachineGunGhastMode.FASTER.ordinal();
        if (M >= S) FB.setDeltaMovement(FB.getDeltaMovement().scale(8 * (M - S + 1)));
    }
}