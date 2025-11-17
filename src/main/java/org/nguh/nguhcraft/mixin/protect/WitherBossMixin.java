package org.nguh.nguhcraft.mixin.protect;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WitherBoss.class)
public abstract class WitherBossMixin {
    /**
     * Prevent withers from destroying protected blocks during ticking.
     * <p>
     * This does not use explosions but instead destroys blocks directly,
     * so we need to handle it separately.
     */
    @Redirect(
        method = "customServerAiStep",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/boss/wither/WitherBoss;canDestroy(Lnet/minecraft/world/level/block/state/BlockState;)Z"
        )
    )
    private boolean inject$mobTick$canDestroy(BlockState St, ServerLevel SW, @Local BlockPos Pos) {
        return !ProtectionManager.IsProtectedBlock(SW, Pos) && WitherBoss.canDestroy(St);
    }
}
