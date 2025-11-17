package org.nguh.nguhcraft.mixin.protect.server;

import net.minecraft.world.level.block.WitherSkullBlock;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WitherSkullBlock.class)
public abstract class WitherSkullBlockMixin {
    /** Prevent spawning withers in protected areas. */
    @Inject(
        method = "checkSpawn(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/SkullBlockEntity;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void inject$onPlaced(Level W, BlockPos Pos, SkullBlockEntity BE, CallbackInfo CI) {
        if (ProtectionManager.IsProtectedBlock(W, Pos))
            CI.cancel();
    }
}
