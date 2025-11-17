package org.nguh.nguhcraft.mixin.vanish;

import com.mojang.authlib.GameProfile;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.nguh.nguhcraft.server.dedicated.Vanish;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends Player {
    public ServerPlayerMixin(Level world, GameProfile profile) {
        super(world, profile);
    }

    @Unique private boolean Vanished() { return Vanish.IsVanished((ServerPlayer)(Object)this); }

    /** Vanished players should not block projectiles. */
    @Override
    public boolean canBeHitByProjectile() {
        return !Vanished() && super.canBeHitByProjectile();
    }

    /** This prevents entity tracking of vanished players. */
    @Inject(method = "broadcastToPlayer", at = @At("HEAD"), cancellable = true)
    void inject$canBeSpectated(ServerPlayer Spectator, CallbackInfoReturnable<Boolean> CIR) {
        if (Vanished()) CIR.setReturnValue(false);
    }

    /** Make vanished players invisible. */
    @Override
    public boolean isInvisibleTo(Player PE) {
        return Vanished() || super.isInvisibleTo(PE);
    }

    /** Make vanished players silent. */
    @Override
    public boolean isSilent() {
        return Vanished() || super.isSilent();
    }
}
