package org.nguh.nguhcraft.mixin.protect.server;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.nguh.nguhcraft.SyncedGameRule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EndPortalBlock.class)
public class EndPortalBlockMixin {
    @Inject(
        method = "entityInside",
        at = @At("HEAD"),
        cancellable = true
    )
    private void inject$onEntityCollision(BlockState St, Level W, BlockPos Pos, Entity E, InsideBlockEffectApplier H, CallbackInfo CI) {
        if (!SyncedGameRule.END_ENABLED.IsSet()) CI.cancel();
    }
}
