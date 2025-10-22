package org.nguh.nguhcraft.mixin.server.dedicated;

import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import net.minecraft.server.rcon.RconClient;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RconClient.class)
public abstract class RConClientMixin {
    @Shadow @Final private DedicatedServer server;
    @Unique private static final Text RCON_COMPONENT = Text.of("[RCON Response] ");

    /** Log RCON command responses. */
    @Inject(method = "respond(ILjava/lang/String;)V", at = @At("HEAD"))
    private void respond(int Token, String Message, CallbackInfo CI) {
        if (Message.trim().isEmpty()) return;
        var S = ((MinecraftDedicatedServer)server);
        ((MinecraftDedicatedServer)server).execute(() -> S.sendMessage(Text.empty()
            .append(RCON_COMPONENT)
            .append(Message)
        ));
    }
}
