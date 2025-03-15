package org.nguh.nguhcraft.mixin.protect;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.block.BlockState;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WitherEntity.class)
public abstract class WitherEntityMixin {
    /**
     * Prevent withers from destroying protected blocks during ticking.
     * <p>
     * This does not use explosions but instead destroys blocks directly,
     * so we need to handle it separately.
     */
    @Redirect(
        method = "mobTick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/boss/WitherEntity;canDestroy(Lnet/minecraft/block/BlockState;)Z"
        )
    )
    private boolean inject$mobTick$canDestroy(BlockState St, ServerWorld SW, @Local BlockPos Pos) {
        return !ProtectionManager.IsProtectedBlock(SW, Pos) && WitherEntity.canDestroy(St);
    }
}
