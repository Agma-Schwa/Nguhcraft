package org.nguh.nguhcraft.mixin.server.dedicated;

import net.minecraft.server.ServerInterface;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.rcon.thread.RconClient;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RconClient.class)
public abstract class RconClientMixin {
    @Shadow @Final private ServerInterface serverInterface;
    @Unique private static final Component RCON_COMPONENT = Component.nullToEmpty("[RCON Response] ");

    /** Log RCON command responses. */
    @Inject(method = "sendCmdResponse(ILjava/lang/String;)V", at = @At("HEAD"))
    private void respond(int Token, String Message, CallbackInfo CI) {
        if (Message.trim().isEmpty()) return;
        var S = ((DedicatedServer) serverInterface);
        ((DedicatedServer) serverInterface).execute(() -> S.sendSystemMessage(Component.empty()
            .append(RCON_COMPONENT)
            .append(Message)
        ));
    }
}
