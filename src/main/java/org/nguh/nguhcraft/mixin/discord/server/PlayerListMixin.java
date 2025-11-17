package org.nguh.nguhcraft.mixin.discord.server;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.Connection;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import org.nguh.nguhcraft.server.dedicated.Discord;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.SocketAddress;

@Mixin(value = PlayerList.class, priority = 1)
public abstract class PlayerListMixin {
    /** Treat muted users as if they were banned. */
    @Inject(method = "canPlayerLogin", at = @At("HEAD"), cancellable = true)
    private void inject$checkCanJoin(SocketAddress SA, GameProfile GP, CallbackInfoReturnable<Component> CIR) {
        var Message = Discord.CheckCanJoin(GP);
        if (Message != null) CIR.setReturnValue(Message);
    }


    /** Update the discord player name early. */
    @Inject(
        method = "placeNewPlayer(Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/server/network/CommonListenerCookie;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;setServerLevel(Lnet/minecraft/server/level/ServerLevel;)V"
        )
    )
    private void inject$onPlayerConnect$0(
        Connection Connection,
        ServerPlayer SP,
        CommonListenerCookie Data,
        CallbackInfo Info
    ) {
        // We used to have to load player data early here, but Mojang for
        // once did something sensible and moved data loading up to before
        // this, so we can assume that everything is already loaded.
        Discord.UpdatePlayerName(SP);
    }

    /** Send a join message to Discord. */
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
        // Re-fetch account data from Discord in the background to
        // make sure they’re still linked.
        Discord.UpdatePlayerOnJoin(SP);
        Discord.BroadcastClientStateOnJoin(SP);
    }
}
