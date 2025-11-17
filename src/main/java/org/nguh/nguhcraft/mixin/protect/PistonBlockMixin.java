package org.nguh.nguhcraft.mixin.protect;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PistonBaseBlock.class)
public abstract class PistonBlockMixin {
    /** Prevent pistons from moving protected blocks. */
    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    private static void inject$isMovable(
            BlockState St,
            Level W,
            BlockPos Pos,
            Direction D,
            boolean CanBreak,
            Direction PistonDir,
            CallbackInfoReturnable<Boolean> CIR
    ) {
        if (ProtectionManager.IsProtectedBlock(W, Pos))
            CIR.setReturnValue(false);
    }
}
