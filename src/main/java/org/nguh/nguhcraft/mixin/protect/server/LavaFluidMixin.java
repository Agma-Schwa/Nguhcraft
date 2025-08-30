package org.nguh.nguhcraft.mixin.protect.server;

import net.minecraft.fluid.LavaFluid;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LavaFluid.class)
public abstract class LavaFluidMixin {
    /** Disable lava lighting protected blocks on fire. */
    @Inject(method = "hasBurnableBlock", at = @At("HEAD"), cancellable = true)
    private void onHasBurnableBlock(WorldView WV, BlockPos Pos, CallbackInfoReturnable<Boolean> CIR) {
        if (!(WV instanceof ServerWorld SW)) return; // Ignore during world generation.
        if (ProtectionManager.IsProtectedBlock(SW, Pos)) CIR.setReturnValue(false);
    }
}
