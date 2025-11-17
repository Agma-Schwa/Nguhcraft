package org.nguh.nguhcraft.mixin.protect.server;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SnowLayerBlock.class)
public abstract class SnowLayerBlockMixin {
    /** Prevent snow from falling on protected blocks. */
    @Inject(method = "canSurvive", at = @At("HEAD"), cancellable = true)
    private void inject$canPlaceAt(
        BlockState St,
        LevelReader WV,
        BlockPos Pos,
        CallbackInfoReturnable<Boolean> CIR
    ) {
        if (WV instanceof Level W && ProtectionManager.IsProtectedBlock(W, Pos))
            CIR.setReturnValue(false);
    }

    /** Prevent protected snow from melting. */
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void inject$randomTick(
        BlockState St,
        ServerLevel W,
        BlockPos Pos,
        RandomSource Rng,
        CallbackInfo CI
    ) {
        if (ProtectionManager.IsProtectedBlock(W, Pos))
            CI.cancel();
    }
}
