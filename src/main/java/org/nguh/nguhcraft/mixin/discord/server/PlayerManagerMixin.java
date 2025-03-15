package org.nguh.nguhcraft.mixin.discord.server;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.nguh.nguhcraft.server.dedicated.Discord;
import org.nguh.nguhcraft.server.accessors.ServerPlayerDiscordAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.SocketAddress;
import java.util.Optional;

@Mixin(value = PlayerManager.class, priority = 1)
public abstract class PlayerManagerMixin {
    /** Treat muted users as if they were banned. */
    @Inject(method = "checkCanJoin", at = @At("HEAD"), cancellable = true)
    private void inject$checkCanJoin(SocketAddress SA, GameProfile GP, CallbackInfoReturnable<Text> CIR) {
        var Message = Discord.CheckCanJoin(GP);
        if (Message != null) CIR.setReturnValue(Message);
    }

    /** Load custom player data early. */
    @Inject(
        method = "onPlayerConnect(Lnet/minecraft/network/ClientConnection;Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/server/network/ConnectedClientData;)V",
        at = @At(
            value = "INVOKE",
            target = "net/minecraft/server/network/ServerPlayerEntity.setServerWorld (Lnet/minecraft/server/world/ServerWorld;)V",
            ordinal = 0
        )
    )
    private void inject$onPlayerConnect$0(
        ClientConnection Connection,
        ServerPlayerEntity SP,
        ConnectedClientData Data,
        CallbackInfo Info,
        @Local Optional<NbtCompound> Nbt
    ) {
        var NSP = ((ServerPlayerDiscordAccessor)SP);
        Nbt.ifPresent(NSP::LoadDiscordNguhcraftNbt);
        Discord.UpdatePlayerName(SP);
    }

    /** Send a join message to Discord. */
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
        // Re-fetch account data from Discord in the background to
        // make sure they’re still linked.
        Discord.UpdatePlayerOnJoin(SP);
        Discord.BroadcastClientStateOnJoin(SP);
    }
}
