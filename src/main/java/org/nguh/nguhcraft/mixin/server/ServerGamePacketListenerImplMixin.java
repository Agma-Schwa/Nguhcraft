package org.nguh.nguhcraft.mixin.server;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.nguh.nguhcraft.server.ServerNetworkHandler;
import org.nguh.nguhcraft.server.ServerUtils;
import org.nguh.nguhcraft.server.accessors.LivingEntityAccessor;
import org.nguh.nguhcraft.server.accessors.PlayerInteractEntityC2SPacketAccessor;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin extends ServerCommonPacketListenerImpl {
    public ServerGamePacketListenerImplMixin(MinecraftServer server, Connection connection, CommonListenerCookie clientData) {
        super(server, connection, clientData);
    }

    @Unique static private final Component NEEDS_CLIENT_MOD
        = Component.literal("Please install the Nguhcraft client-side mod to play on this server");

    @Shadow public ServerPlayer player;

    @Shadow @Final static Logger LOGGER;

    /** Hide quit message if the player is vanished. */
    @Redirect(
        method = "removePlayerFromWorld",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"
        )
    )
    private void inject$cleanUp(PlayerList PM, Component Msg, boolean Overlay) {
        ServerUtils.ActOnPlayerQuit(player, Msg);
    }


    /**
    * Prevent players in a hypershot context from using weapons.
    * <p>
    * This should already be prevented client-side, but we check for this
    * here as well just in case.
    */
    @Inject(
        method = "handleUseItem",
        cancellable = true,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;resetLastActionTime()V",
            ordinal = 0,
            shift = At.Shift.AFTER
        )
    )
    private void inject$onPlayerInteractItem(ServerboundUseItemPacket Packet, CallbackInfo CI) {
        if (((LivingEntityAccessor)player).getHypershotContext() != null) {
            LOGGER.warn("Player {} tried to use an item while in hypershot context", player.getDisplayName());
            CI.cancel();
        }
    }


    /** Prevent interactions within a region. */
    @Inject(
        method = "handleInteract",
        cancellable = true,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;canInteractWithEntity(Lnet/minecraft/world/phys/AABB;D)Z",
            ordinal = 0
        )
    )
    private void inject$onPlayerInteractEntity(ServerboundInteractPacket Packet, CallbackInfo CI, @Local Entity E) {
        // Attack.
        if (((PlayerInteractEntityC2SPacketAccessor) Packet).IsAttack()) {
            if (!ProtectionManager.AllowEntityAttack(player, E))
                CI.cancel();
        }

        // Interaction.
        else {
            if (!ProtectionManager.AllowEntityInteract(player, E))
                CI.cancel();
        }
    }

    /**
    * Initial player tick.
    * <p>
    * This is called before doing any other processing on the player. Despite
    * the fact that this is part of the network handler, this is still called
    * on the tick thread.
    */
    @Inject(method = "tick()V", at = @At("HEAD"))
    private void inject$tick(CallbackInfo CI) { ServerUtils.ActOnPlayerTick(player); }

    /**
     * Disconnect on incoming signed chat messages.
     *
     * @author Sirraide
     * @reason The client is patched to never send these.
     */
    @Overwrite
    public void handleChat(ServerboundChatPacket Packet) {
        disconnect(NEEDS_CLIENT_MOD);
    }

    /**
     * Disconnect on incoming signed commands.
     *
     * @author Sirraide
     * @reason The client is patched to never send these.
     */
    @Overwrite
    public void handleSignedChatCommand(ServerboundChatCommandSignedPacket Packet) {
        disconnect(NEEDS_CLIENT_MOD);
    }

    /**
     * Handle incoming commands.
     * <p>
     * We need to hijack the command handling logic a bit since an unlinked
     * player should *never* be able to execute commands.
     *
     * @author Sirraide
     * @reason See above.
     */
    @Overwrite
    public void handleChatCommand(@NotNull ServerboundChatCommandPacket Packet) {
        ServerNetworkHandler.HandleCommand((ServerGamePacketListenerImpl) (Object) this, Packet.command());
    }

    /**
     * Disconnect if the client tries to establish a session.
     *
     * @author Sirraide
     * @reason The client is patched to never send these.
     */
    @Overwrite
    public void handleChatSessionUpdate(ServerboundChatSessionUpdatePacket packet) {
        disconnect(NEEDS_CLIENT_MOD);
    }
}
