package org.nguh.nguhcraft.mixin.discord.server;

import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ServerPlayer;
import org.nguh.nguhcraft.server.dedicated.Discord;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin extends ServerCommonPacketListenerImpl {
    public ServerGamePacketListenerImplMixin(MinecraftServer server, Connection connection, CommonListenerCookie clientData) {
        super(server, connection, clientData);
    }

    @Shadow public ServerPlayer player;

    /** Forward quit message to Discord. */
    @Inject(method = "removePlayerFromWorld()V", at = @At("HEAD"))
    private void inject$cleanUp(CallbackInfo CI) {
        Discord.BroadcastJoinQuitMessage(player, false);
    }
}

