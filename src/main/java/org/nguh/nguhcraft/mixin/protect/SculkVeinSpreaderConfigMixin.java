package org.nguh.nguhcraft.mixin.protect;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.SculkVeinBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SculkVeinBlock.SculkVeinSpreaderConfig.class)
public abstract class SculkVeinSpreaderConfigMixin {
    /** Prevent vine growth in regions. */
    @Inject(
        method = "stateCanBeReplaced(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/world/level/block/state/BlockState;)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void inject$canGrowOn(
        BlockGetter BV,
        BlockPos Pos,
        BlockPos GrowPos,
        Direction Dir,
        BlockState St,
        CallbackInfoReturnable<Boolean> CIR
    ) {
        // Ignore regions during world generation.
        if (!(BV instanceof Level W)) return;
        if (ProtectionManager.IsProtectedBlock(W, Pos))
            CIR.setReturnValue(false);
    }
}
