package org.nguh.nguhcraft.mixin.entity;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.Fox.FoxEatBerriesGoal;
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
    @Shadow @Final
    Fox field_17975;

    // makes foxes want to harvest grapes
    @Inject(method = "isValidTarget", at = @At("HEAD"), cancellable = true)
    private void inject$isValidTarget(LevelReader levelReader, BlockPos blockPos, CallbackInfoReturnable<Boolean> cir) {
        BlockState blockState = levelReader.getBlockState(blockPos);
        if (blockState.is(NguhBlocks.GRAPE_CROP) && blockState.getValue(GrapeCropBlock.AGE) == GrapeCropBlock.MAX_AGE) {
            cir.setReturnValue(!ProtectionManager.IsProtectedBlock(field_17975.level(), blockPos));
        }
    }

    // makes the foxes actually get the grapes
    @Inject(method = "onReachedTarget", at = @At("TAIL"))
    private void inject$onReachedTarget(CallbackInfo ci, @Local BlockState St) {
        if (St.is(NguhBlocks.GRAPE_CROP)) {
            GrapeCropBlock.OnFoxUse(St, field_17975);
        }
    }
}
