package org.nguh.nguhcraft.mixin.protect.server;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FireBlock.class)
public abstract class FireBlockMixin {
    /**
    * Disable fire tick in regions.
    * <p>
    * We accomplish this by returning false from the fire spread check.
    */
    @WrapOperation(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;canSpreadFireAround(Lnet/minecraft/core/BlockPos;)Z"
        )
    )
    private boolean inject$scheduledTick(
        ServerLevel SL,
        BlockPos FirePos,
        Operation<Boolean> Op,
        BlockState St,
        ServerLevel SW,
        BlockPos Pos
    ) {
        if (ProtectionManager.IsProtectedBlock(SW, Pos)) return false;
        return Op.call(SL, FirePos);
    }
}
