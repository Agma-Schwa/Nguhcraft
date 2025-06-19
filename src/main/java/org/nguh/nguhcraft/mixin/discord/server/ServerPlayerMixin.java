package org.nguh.nguhcraft.mixin.discord.server;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.damage.DamageTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.nguh.nguhcraft.server.dedicated.Discord;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerMixin extends PlayerEntity {
    public ServerPlayerMixin(World world, GameProfile profile) {
        super(world, profile);
    }

    /** Inject code to send a death message to discord (and for custom death messages). */
    @Redirect(
        method = "onDeath(Lnet/minecraft/entity/damage/DamageSource;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/damage/DamageTracker;getDeathMessage()Lnet/minecraft/text/Text;"
        )
    )
    private Text inject$onDeath(DamageTracker I) {
        var DeathMessage = I.getDeathMessage();
        Discord.BroadcastDeathMessage((ServerPlayerEntity) (Object) this, DeathMessage);
        return DeathMessage;
    }

    /** Put player in adventure mode if they are unlinked. */
    @SuppressWarnings("UnreachableCode")
    @Inject(method = "tick()V", at = @At("HEAD"))
    private void inject$tick(CallbackInfo ci) {
        Discord.TickPlayer((ServerPlayerEntity)(Object)this);
    }
}
