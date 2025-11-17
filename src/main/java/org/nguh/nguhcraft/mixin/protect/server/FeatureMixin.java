package org.nguh.nguhcraft.mixin.protect.server;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelWriter;
import net.minecraft.world.level.levelgen.feature.Feature;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Feature.class)
public abstract class FeatureMixin {
    /**
     * Prevent features from replacing protected blocks.
     * <p>
     * For instance, this prevents locked chests from being
     * replaced by huge mushroom caps.
     */
    @Inject(method = "setBlock", at = @At("HEAD"), cancellable = true)
    private void inject$setBlockState(LevelWriter MW, BlockPos Pos, BlockState St, CallbackInfo CI) {
        if (MW instanceof ServerLevel SW && ProtectionManager.IsProtectedBlock(SW, Pos))
            CI.cancel();
    }
}
