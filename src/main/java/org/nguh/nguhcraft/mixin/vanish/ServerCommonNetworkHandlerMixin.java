package org.nguh.nguhcraft.mixin.vanish;


import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import org.nguh.nguhcraft.server.dedicated.Vanish;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class ServerCommonNetworkHandlerMixin {
    @Shadow @Final protected MinecraftServer server;

    /**
    * Remove vanished players from the player list.
    * <p>
    * This is a bit of a hack, but we need to change the information that
    * is sent on a per-player basis anyway, and this is easier than doing
    * that in every place where the player list is sent.
    */
    @Inject(method = "send*", at = @At("HEAD"))
    private void inject$sendPacket(
        Packet<?> OriginalPacket,
        CallbackInfo CI,
        @Local(argsOnly = true) LocalRef<Packet<?>> P
    ) {
        if (OriginalPacket instanceof ClientboundPlayerInfoUpdatePacket PLP)
            P.set(Vanish.FixPlayerListPacket(server, (ServerCommonPacketListenerImpl) (Object) this, PLP));
    }
}
