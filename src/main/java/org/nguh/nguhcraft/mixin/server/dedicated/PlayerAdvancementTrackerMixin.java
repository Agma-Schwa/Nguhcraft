package org.nguh.nguhcraft.mixin.server.dedicated;

import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.nguh.nguhcraft.server.dedicated.Discord;
import org.nguh.nguhcraft.server.dedicated.Vanish;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerAdvancementTracker.class)
public abstract class PlayerAdvancementTrackerMixin {
    @Shadow private ServerPlayerEntity owner;

    /**
     * Grab advancement message.
     * <p>
     * The advancement message is computed in a method call to PlayerList::broadcast;
     * save it for forwarding to Discord. The call to broadcast also happens in a lambda,
     * which makes this even more annoying to intercept.
     */
    @Redirect(
        method = "method_53637(Lnet/minecraft/advancement/AdvancementEntry;Lnet/minecraft/advancement/AdvancementDisplay;)V",
        at = @At(
            value = "INVOKE",
            target = "net/minecraft/server/PlayerManager.broadcast(Lnet/minecraft/text/Text;Z)V",
            ordinal = 0
        )
    )
    private void inject$grantCriterion$lambda0$0(
        PlayerManager PM,
        Text Msg,
        boolean Overlay
    ) {
        if (!Vanish.IsVanished(owner)) PM.broadcast(Msg, Overlay);
        Discord.BroadcastAdvancement(owner, Msg);
    }
}
