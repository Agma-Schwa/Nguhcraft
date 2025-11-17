package org.nguh.nguhcraft.mixin.protect.server;

import net.minecraft.world.item.ItemStack;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {
    @Shadow protected abstract void debugLogging(BlockPos pos, boolean success, int sequence, String reason);

    @Shadow @Final protected ServerPlayer player;

    @Shadow protected ServerLevel level;

    /** Prevent item use. */
    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void inject$interactItem(
        ServerPlayer SP,
        Level W,
        ItemStack St,
        InteractionHand H,
        CallbackInfoReturnable<InteractionResult> CIR
    ) {
        if (!ProtectionManager.AllowItemUse(SP, W, St))
            CIR.setReturnValue(InteractionResult.PASS);
    }

    /** Prevent block modification. */
    @Inject(method = "handleBlockBreakAction", at = @At("HEAD"), cancellable = true)
    private void inject$processBlockBreakingAction(
        BlockPos Pos,
        ServerboundPlayerActionPacket.Action A,
        Direction Dir,
        int WH,
        int Seq,
        CallbackInfo CI
    ) {
        if (!ProtectionManager.AllowBlockModify(player, this.level, Pos)) {
            player.connection.send(new ClientboundBlockUpdatePacket(Pos, this.level.getBlockState(Pos)));
            debugLogging(Pos, false, Seq, "disallowed");
            CI.cancel();
        }
    }
}
