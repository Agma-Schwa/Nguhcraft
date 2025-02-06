package org.nguh.nguhcraft.mixin.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.client.network.ClientConnectionState;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;
import org.jetbrains.annotations.NotNull;
import org.nguh.nguhcraft.client.NguhcraftClient;
import org.nguh.nguhcraft.client.accessors.ClientDisplayData;
import org.nguh.nguhcraft.client.accessors.ClientDisplayDataAccessor;
import org.nguh.nguhcraft.network.ServerboundChatPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin extends ClientCommonNetworkHandler implements ClientDisplayDataAccessor {
    protected ClientPlayNetworkHandlerMixin(MinecraftClient client, ClientConnection connection, ClientConnectionState connectionState) {
        super(client, connection, connectionState);
    }

    @Shadow private boolean displayedUnsecureChatWarning;
    @Unique private final ClientDisplayData DisplayData = new ClientDisplayData();

    @Override public @NotNull ClientDisplayData Nguhcraft$GetDisplayData() { return DisplayData; }

    /** Suppress unsecure server toast. */
    @Inject(method = "onGameJoin(Lnet/minecraft/network/packet/s2c/play/GameJoinS2CPacket;)V", at = @At("HEAD"))
    private void inject$onGameJoin(CallbackInfo CI) {
        displayedUnsecureChatWarning = true;
        NguhcraftClient.InHypershotContext = false;
    }


    /**
    * Send a chat message.
    * <p>
    * We use a custom packet for chat messages both to disable chat signing
    * and reporting as well as to circumvent the usual 256-character limit.
    * @author Sirraide
    * @reason See above.
    */
    @Overwrite
    public void sendChatMessage(String message) {
        ClientPlayNetworking.send(new ServerboundChatPacket(message));
    }

    /**
     * Send a chat command.
     * <p>
     * Remove chat signing part of this.
     * @author Sirraide
     * @reason See above.
     */
    @Overwrite
    public void sendChatCommand(String message) {
        sendPacket(new CommandExecutionC2SPacket(message));
    }

    /**
     * Send a command.
     * <p>
     * We always send a command as unsigned; thus, this also always succeeds.
     * @author Sirraide
     * @reason Function body is short enough to where injection is pointless.
     */
    @Overwrite
    public boolean sendCommand(String message) {
        sendPacket(new CommandExecutionC2SPacket(message));
        return true;
    }
}
