package org.nguh.nguhcraft.mixin.protect.server;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.enchantment.effects.ReplaceDisk;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ReplaceDisk.class)
public abstract class ReplaceDiskMixin {
    /** Disable replacement effects such as frost walker in protected regions. */
    @WrapOperation(
        method = "apply",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"
        )
    )
    private boolean inject$apply(
        ServerLevel Instance,
        BlockPos Pos,
        BlockState St,
        Operation<Boolean> SetBlockState
    ) {
        if (ProtectionManager.IsProtectedBlock(Instance, Pos)) return false;
        return SetBlockState.call(Instance, Pos, St);
    }
}
