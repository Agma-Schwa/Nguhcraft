package org.nguh.nguhcraft.mixin.discord.server;

import com.mojang.authlib.GameProfile;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import org.nguh.nguhcraft.server.dedicated.Discord;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends Player {
    public ServerPlayerMixin(Level world, GameProfile profile) {
        super(world, profile);
    }

    /** Inject code to send a death message to discord (and for custom death messages). */
    @Redirect(
        method = "die(Lnet/minecraft/world/damagesource/DamageSource;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/damagesource/CombatTracker;getDeathMessage()Lnet/minecraft/network/chat/Component;"
        )
    )
    private Component inject$onDeath(CombatTracker I) {
        var DeathMessage = I.getDeathMessage();
        Discord.BroadcastDeathMessage((ServerPlayer) (Object) this, DeathMessage);
        return DeathMessage;
    }

    /** Put player in adventure mode if they are unlinked. */
    @SuppressWarnings("UnreachableCode")
    @Inject(method = "tick()V", at = @At("HEAD"))
    private void inject$tick(CallbackInfo ci) {
        Discord.TickPlayer((ServerPlayer)(Object)this);
    }
}
