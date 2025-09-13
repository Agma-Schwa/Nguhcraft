package org.nguh.nguhcraft.mixin.protect.server;

import net.minecraft.block.BlockState;
import net.minecraft.block.EndPortalBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCollisionHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.nguh.nguhcraft.SyncedGameRule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EndPortalBlock.class)
public class EndPortalBlockMixin {
    @Inject(
        method = "onEntityCollision",
        at = @At("HEAD"),
        cancellable = true
    )
    private void inject$onEntityCollision(BlockState St, World W, BlockPos Pos, Entity E, EntityCollisionHandler H, CallbackInfo CI) {
        if (!SyncedGameRule.END_ENABLED.IsSet()) CI.cancel();
    }
}
