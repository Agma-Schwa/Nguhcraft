package org.nguh.nguhcraft.mixin.protect.server;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameRules;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FireBlock.class)
public abstract class FireBlockMixin {
    /**
    * Disable fire tick in regions.
    * <p>
    * We accomplish this by returning false from the game rule check.
    */
    @WrapOperation(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/GameRules;getBoolean(Lnet/minecraft/world/level/GameRules$Key;)Z"
        )
    )
    private boolean inject$scheduledTick(
        GameRules GR,
        GameRules.Key<GameRules.BooleanValue> R,
        Operation<Boolean> Op,
        BlockState St,
        ServerLevel SW,
        BlockPos Pos
    ) {
        if (ProtectionManager.IsProtectedBlock(SW, Pos)) return false;
        return Op.call(GR, R);
    }
}
