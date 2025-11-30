package org.nguh.nguhcraft.mixin.entity;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.Fox.FoxEatBerriesGoal;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import org.nguh.nguhcraft.block.GrapeCropBlock;
import org.nguh.nguhcraft.block.NguhBlocks;
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
        if (blockState.is(NguhBlocks.INSTANCE.getGRAPE_CROP()) && blockState.getValue(GrapeCropBlock.Companion.getAGE()) == GrapeCropBlock.MAX_AGE) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    // makes the foxes actually get the grapes
    @Inject(method = "onReachedTarget", at = @At("TAIL"))
    private void inject$onReachedTarget(CallbackInfo ci, @Local BlockState St) {
        if (St.is(NguhBlocks.INSTANCE.getGRAPE_CROP())) {
            GrapeCropBlock.Companion.OnFoxUse(St, field_17975);
        }
    }
}
