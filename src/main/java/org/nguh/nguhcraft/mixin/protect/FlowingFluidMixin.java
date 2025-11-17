package org.nguh.nguhcraft.mixin.protect;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FlowingFluid.class)
public abstract class FlowingFluidMixin {
    /** Prevent fluids from flowing in(to) protected areas. */
    @Inject(
        method = "canHoldFluid(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/Fluid;)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void inject$canFill(
            BlockGetter BV,
            BlockPos Pos,
            BlockState St,
            Fluid Fl,
            CallbackInfoReturnable<Boolean> CIR
    ) {
        // Ignore region check during world generation.
        if (!(BV instanceof Level W) || Pos == null) return;
        if (ProtectionManager.IsProtectedBlock(W, Pos))
            CIR.setReturnValue(false);
    }
}
