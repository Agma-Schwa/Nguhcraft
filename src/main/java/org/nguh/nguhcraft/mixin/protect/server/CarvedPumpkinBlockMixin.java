package org.nguh.nguhcraft.mixin.protect.server;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CarvedPumpkinBlock.class)
public abstract class CarvedPumpkinBlockMixin {
    /** Prevent the creation of snow and iron golems in protected areas. */
    @Inject(method = "trySpawnGolem", at = @At("HEAD"), cancellable = true)
    private void inject$trySpawnEntity(Level W, BlockPos Pos, CallbackInfo CI) {
        if (ProtectionManager.IsProtectedBlock(W, Pos))
            CI.cancel();
    }
}
