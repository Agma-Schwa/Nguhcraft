package org.nguh.nguhcraft.mixin.server.dedicated;

import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import org.nguh.nguhcraft.server.dedicated.Discord;
import org.nguh.nguhcraft.server.dedicated.Vanish;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerAdvancements.class)
public abstract class PlayerAdvancementsMixin {
    @Shadow private ServerPlayer player;

    /**
     * Grab advancement message.
     * <p>
     * The advancement message is computed in a method call to PlayerList::broadcast;
     * save it for forwarding to Discord. The call to broadcast also happens in a lambda,
     * which makes this even more annoying to intercept.
     */
    @Redirect(
        method = "method_53637",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V",
            ordinal = 0
        )
    )
    private void inject$grantCriterion$lambda0$0(
        PlayerList PM,
        Component Msg,
        boolean Overlay
    ) {
        if (!Vanish.IsVanished(player)) PM.broadcastSystemMessage(Msg, Overlay);
        Discord.BroadcastAdvancement(player, Msg);
    }
}
