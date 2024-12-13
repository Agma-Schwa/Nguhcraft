package org.nguh.nguhcraft.mixin.server;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.nguh.nguhcraft.server.accessors.ServerPlayerAccessor;
import org.nguh.nguhcraft.server.ServerUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(PlayerManager.class)
public abstract class PlayerManagerMixin {
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
        var NSP = ((ServerPlayerAccessor)SP);
        Nbt.ifPresent(NSP::LoadGeneralNguhcraftNbt);
    }

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
