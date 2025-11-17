package org.nguh.nguhcraft.mixin.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.LastSeenMessagesTracker;
import net.minecraft.network.chat.MessageSignatureCache;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.ClientboundDeleteChatPacket;
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

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin extends ClientCommonPacketListenerImpl implements ClientDisplayDataAccessor {
    protected ClientPacketListenerMixin(Minecraft client, Connection connection, CommonListenerCookie connectionState) {
        super(client, connection, connectionState);
    }

    @Shadow private boolean seenInsecureChatWarning;
    @Shadow private LastSeenMessagesTracker lastSeenMessages;
    @Shadow private MessageSignatureCache messageSignatureCache;
    @Unique private final ClientDisplayData DisplayData = new ClientDisplayData();

    @Override public @NotNull ClientDisplayData Nguhcraft$GetDisplayData() { return DisplayData; }

    /** Remove any data related to chat signing. */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void inject$ctor(Minecraft C, Connection CC, CommonListenerCookie CCS, CallbackInfo CI) {
        // Ensure that we crash if we try to do anything related to chat signing.
        lastSeenMessages = null;
        messageSignatureCache = null;
    }

    /** Suppress unsecure server toast. */
    @Inject(method = "handleLogin(Lnet/minecraft/network/protocol/game/ClientboundLoginPacket;)V", at = @At("HEAD"))
    private void inject$onGameJoin(CallbackInfo CI) {
        seenInsecureChatWarning = true;
        NguhcraftClient.InHypershotContext = false;
    }

    /**
     * We can ignore this packet as we never remove messages.
     * @author Sirraide
     * @reason See above.
     */
    @Overwrite
    public void handleDeleteChat(ClientboundDeleteChatPacket packet) {}

    /**
     * We completely replace this packet with something else.
     * @author Sirraide
     * @reason See above.
     */
    @Overwrite
    public void handlePlayerChat(ClientboundPlayerChatPacket packet) {
        throw new IllegalStateException("Should never be sent by the Nguhcraft server");
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
    public void sendChat(String message) {
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
    public void sendCommand(String message) {
        send(new ServerboundChatCommandPacket(message));
    }
}
