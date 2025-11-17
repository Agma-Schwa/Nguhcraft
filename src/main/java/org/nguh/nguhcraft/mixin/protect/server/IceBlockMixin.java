package org.nguh.nguhcraft.mixin.protect.server;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IceBlock.class)
public abstract class IceBlockMixin {
    /** Prevent protected ice blocks from melting. */
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void inject$randomTick(
        BlockState St,
        ServerLevel SW,
        BlockPos Pos,
        RandomSource Rng,
        CallbackInfo CI
    ) {
        if (ProtectionManager.IsProtectedBlock(SW, Pos))
            CI.cancel();
    }
}
