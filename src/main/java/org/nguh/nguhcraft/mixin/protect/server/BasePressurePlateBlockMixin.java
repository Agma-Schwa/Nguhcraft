package org.nguh.nguhcraft.mixin.protect.server;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BasePressurePlateBlock.class)
public abstract class BasePressurePlateBlockMixin {
    /** Return a redstone signal of 0 if this pressure plate is disabled. */
    @WrapOperation(
        method = "checkPressed",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/BasePressurePlateBlock;getSignalStrength(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)I"
        )
    )
    private int inject$updatePlateState(
        BasePressurePlateBlock Instance,
        Level W,
        BlockPos Pos,
        Operation<Integer> Op
    ) {
        return ProtectionManager.IsPressurePlateEnabled(W, Pos)
            ? Op.call(Instance, W, Pos)
            : 0;
    }
}
