package org.nguh.nguhcraft.mixin.protect.server;

import net.minecraft.world.level.material.LavaFluid;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LavaFluid.class)
public abstract class LavaFluidMixin {
    /** Disable lava lighting protected blocks on fire. */
    @Inject(method = "isFlammable", at = @At("HEAD"), cancellable = true)
    private void onHasBurnableBlock(LevelReader WV, BlockPos Pos, CallbackInfoReturnable<Boolean> CIR) {
        if (!(WV instanceof ServerLevel SW)) return; // Ignore during world generation.
        if (ProtectionManager.IsProtectedBlock(SW, Pos)) CIR.setReturnValue(false);
    }
}
