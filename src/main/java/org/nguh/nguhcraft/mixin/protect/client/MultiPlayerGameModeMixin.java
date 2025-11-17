package org.nguh.nguhcraft.mixin.protect.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.nguh.nguhcraft.client.NguhcraftClient;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void inject$interactItem(Player Player, InteractionHand Hand, CallbackInfoReturnable<InteractionResult> CIR) {
        if (NguhcraftClient.InHypershotContext) CIR.setReturnValue(InteractionResult.PASS);
    }

    /** Prevent block breaking in a region to avoid desync issues. */
    @Inject(method = "continueDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void inject$updateBlockBreakingProgress(BlockPos Pos, Direction Dir, CallbackInfoReturnable<Boolean> CIR) {
        if (!ProtectionManager.AllowBlockModify(minecraft.player, minecraft.level, Pos))
            CIR.setReturnValue(false);
    }
}
