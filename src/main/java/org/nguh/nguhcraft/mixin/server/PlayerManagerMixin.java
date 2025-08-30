package org.nguh.nguhcraft.mixin.server;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.nguh.nguhcraft.server.ServerUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerManager.class)
public abstract class PlayerManagerMixin {
    /** Sync state. */
    @Inject(
        method = "onPlayerConnect(Lnet/minecraft/network/ClientConnection;Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/server/network/ConnectedClientData;)V",
        at = @At("TAIL")
    )
    private void inject$onPlayerConnect$1(
        ClientConnection Connection,
        ServerPlayerEntity SP,
        ConnectedClientData Data,
        CallbackInfo Info
    ) {
        ServerUtils.ActOnPlayerJoin(SP);
    }

    /** Send a join message to Discord. */
    @Redirect(
        method = "onPlayerConnect(Lnet/minecraft/network/ClientConnection;Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/server/network/ConnectedClientData;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/PlayerManager;broadcast(Lnet/minecraft/text/Text;Z)V"
        )
    )
    private void inject$onPlayerConnect$2(
        PlayerManager PM,
        Text Msg,
        boolean Overlay,
        @Local(argsOnly = true) ServerPlayerEntity SP
    ) {
        ServerUtils.SendPlayerJoinQuitMessage(SP, Msg);
    }
}
