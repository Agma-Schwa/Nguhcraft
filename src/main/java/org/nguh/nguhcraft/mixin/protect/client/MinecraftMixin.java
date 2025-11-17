package org.nguh.nguhcraft.mixin.protect.client;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.nguh.nguhcraft.client.NguhcraftClient;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Shadow @Nullable public LocalPlayer player;
    @Shadow @Nullable public ClientLevel level;


    /** Prevent breaking blocks in a protected region. */
    @Inject(
        method = "startAttack()Z",
        cancellable = true,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;startDestroyBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Z"
        )
    )
    private void inject$doAttack(CallbackInfoReturnable<Boolean> CI, @Local BlockPos Pos) {
        if (!ProtectionManager.AllowBlockModify(player, level, Pos)) {
            // Returning true will make the client hallucinate that we did actually
            // break something and stop further processing of this event, without
            // in fact breaking anything.
            player.swing(InteractionHand.MAIN_HAND);
            CI.setReturnValue(true);
        }
    }

    /**
     * Prevent interactions with blocks within regions.
     * <p>
     * Rewrite them to item uses instead.
     */
    @Redirect(
        method = "startUseItem()V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;useItemOn(Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"
        )
    )
    private InteractionResult inject$doItemUse(
        MultiPlayerGameMode Mgr,
        LocalPlayer CPE,
        InteractionHand H,
        BlockHitResult BHR
    ) {
        // Horrible hack: remember this as the last position we interacted
        // with so we can subsequently disable the ‘Take Book’ button when
        // opening a lectern screen.
        //
        // There doesn’t seem to be a good way of doing this properly since
        // the block position of the lectern is never actually sent to the
        // client...
        //
        // This means this value will contain garbage half of the time, but
        // the only thing that matters is that it doesn’t when we actually
        // manage to open a lectern...
        NguhcraftClient.LastInteractedLecternPos = BHR.getBlockPos();
        var Res = ProtectionManager.HandleBlockInteract(
                CPE,
                CPE.clientLevel,
                BHR.getBlockPos(),
                CPE.getItemInHand(H)
        );

        if (!Res.consumesAction()) return Res;
        return Mgr.useItemOn(CPE, H, BHR);
    }
}

