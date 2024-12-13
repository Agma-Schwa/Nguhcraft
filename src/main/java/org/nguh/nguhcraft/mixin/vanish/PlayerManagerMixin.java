package org.nguh.nguhcraft.mixin.vanish;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.nguh.nguhcraft.server.dedicated.Vanish;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerManager.class)
public abstract class PlayerManagerMixin {
    /** Do not send an entry packet when a vanished player joins. */
    @Redirect(
        method = "onPlayerConnect",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/PlayerManager;sendToAll(Lnet/minecraft/network/packet/Packet;)V"
        )
    )
    private void inject$onPlayerConnect(
        PlayerManager PM,
        Packet<?> P,
        @Local(argsOnly = true) ServerPlayerEntity SP
    ) {
        Vanish.BroadcastIfNotVanished(SP, P);
    }
}
