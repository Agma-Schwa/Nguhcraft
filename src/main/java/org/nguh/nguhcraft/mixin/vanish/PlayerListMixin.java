package org.nguh.nguhcraft.mixin.vanish;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.level.ServerPlayer;
import org.nguh.nguhcraft.server.dedicated.Vanish;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
    /** Do not send an entry packet when a vanished player joins. */
    @Redirect(
        method = "placeNewPlayer",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/players/PlayerList;broadcastAll(Lnet/minecraft/network/protocol/Packet;)V"
        )
    )
    private void inject$onPlayerConnect(
        PlayerList PM,
        Packet<?> P,
        @Local(argsOnly = true) ServerPlayer SP
    ) {
        Vanish.BroadcastIfNotVanished(SP, P);
    }
}
