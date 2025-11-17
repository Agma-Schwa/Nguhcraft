package org.nguh.nguhcraft.mixin.protect.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.BlockHitResult;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow @Final Minecraft minecraft;

    /** Don’t highlight blocks if we can’t modify them anyway. */
    @Inject(
        method = "shouldRenderBlockOutline()Z",
        cancellable = true,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/ClientLevel;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"
        )
    )
    private void inject$shouldRenderBlockOutline(CallbackInfoReturnable<Boolean> CI) {
        // TODO: Test if this actually works
        if (!ProtectionManager.AllowBlockModify(
            minecraft.player,
            minecraft.level,
            ((BlockHitResult) minecraft.hitResult).getBlockPos())
        ) CI.setReturnValue(false);
    }
}
