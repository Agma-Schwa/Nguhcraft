package org.nguh.nguhcraft.mixin.protect.server;

import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ModifiableWorld;
import net.minecraft.world.gen.feature.Feature;
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
    @Inject(method = "setBlockState", at = @At("HEAD"), cancellable = true)
    private void inject$setBlockState(ModifiableWorld MW, BlockPos Pos, BlockState St, CallbackInfo CI) {
        if (MW instanceof ServerWorld SW && ProtectionManager.IsProtectedBlock(SW, Pos))
            CI.cancel();
    }
}
