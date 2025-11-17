package org.nguh.nguhcraft.mixin.server;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.network.Connection;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import org.nguh.nguhcraft.server.ServerUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
    /** Sync state. */
    @Inject(
        method = "placeNewPlayer(Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/server/network/CommonListenerCookie;)V",
        at = @At("TAIL")
    )
    private void inject$onPlayerConnect$1(
        Connection Connection,
        ServerPlayer SP,
        CommonListenerCookie Data,
        CallbackInfo Info
    ) {
        ServerUtils.ActOnPlayerJoin(SP);
    }

    /** Send a join message to Discord. */
    @Redirect(
        method = "placeNewPlayer(Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/server/network/CommonListenerCookie;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"
        )
    )
    private void inject$onPlayerConnect$2(
        PlayerList PM,
        Component Msg,
        boolean Overlay,
        @Local(argsOnly = true) ServerPlayer SP
    ) {
        ServerUtils.SendPlayerJoinQuitMessage(SP, Msg);
    }
}
