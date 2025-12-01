package org.nguh.nguhcraft.mixin.entity;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.Fox.FoxEatBerriesGoal;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import org.nguh.nguhcraft.block.GrapeCropBlock;
import org.nguh.nguhcraft.block.NguhBlocks;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FoxEatBerriesGoal.class)
public class FoxEatBerriesGoalMixin {
    @Shadow @Final Fox field_17975;

    /**
     * Make foxes want to harvest grapes, and while we’re at it also
     * disallow harvesting protected blocks.
     */
    @Inject(method = "isValidTarget", at = @At("HEAD"), cancellable = true)
    private void inject$isValidTarget(LevelReader L, BlockPos Pos, CallbackInfoReturnable<Boolean> CIR) {
        if (L instanceof Level Lvl && ProtectionManager.IsProtectedBlock(Lvl, Pos))
            CIR.setReturnValue(false);

        BlockState St = L.getBlockState(Pos);
        if (St.is(NguhBlocks.GRAPE_CROP) && St.getValue(GrapeCropBlock.AGE) == GrapeCropBlock.MAX_AGE)
            CIR.setReturnValue(true);
    }

    /** Actually harvest the grapes. */
    @Inject(method = "onReachedTarget", at = @At("TAIL"), cancellable = true)
    private void inject$onReachedTarget(CallbackInfo CI, @Local BlockState St) {
        if (
            ((ServerLevel)field_17975.level()).getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING) &&
            St.is(NguhBlocks.GRAPE_CROP)
        ) {
            GrapeCropBlock.OnFoxUse(field_17975);
            CI.cancel();
        }
    }
}
