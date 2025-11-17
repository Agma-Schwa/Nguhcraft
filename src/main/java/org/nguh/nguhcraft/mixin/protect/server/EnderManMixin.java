package org.nguh.nguhcraft.mixin.protect.server;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.monster.EnderMan;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EnderMan.class)
public abstract class EnderManMixin {

    /** Just hijack the call that adds the PickupBlockGoal to do nothing. */
    @Redirect(
        method = "registerGoals()V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/ai/goal/GoalSelector;addGoal(ILnet/minecraft/world/entity/ai/goal/Goal;)V",
            ordinal = 7
        )
    )
    private void inject$initGoals(GoalSelector instance, int priority, Goal goal) {}
}