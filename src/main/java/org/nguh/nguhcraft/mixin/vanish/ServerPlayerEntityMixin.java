package org.nguh.nguhcraft.mixin.vanish;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.nguh.nguhcraft.server.dedicated.Vanish;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin  extends PlayerEntity {
    public ServerPlayerEntityMixin(World world, GameProfile profile) {
        super(world, profile);
    }

    @Unique private boolean Vanished() { return Vanish.IsVanished((ServerPlayerEntity)(Object)this); }

    /** Vanished players should not block projectiles. */
    @Override
    public boolean canBeHitByProjectile() {
        return !Vanished() && super.canBeHitByProjectile();
    }

    /** This prevents entity tracking of vanished players. */
    @Inject(method = "canBeSpectated", at = @At("HEAD"), cancellable = true)
    void inject$canBeSpectated(ServerPlayerEntity Spectator, CallbackInfoReturnable<Boolean> CIR) {
        if (Vanished()) CIR.setReturnValue(false);
    }

    /** Make vanished players invisible. */
    @Override
    public boolean isInvisibleTo(PlayerEntity PE) {
        return Vanished() || super.isInvisibleTo(PE);
    }

    /** Make vanished players silent. */
    @Override
    public boolean isSilent() {
        return Vanished() || super.isSilent();
    }
}
