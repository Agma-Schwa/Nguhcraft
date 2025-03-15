package org.nguh.nguhcraft.mixin.protect.server;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.consume.TeleportRandomlyConsumeEffect;
import net.minecraft.util.math.BlockPos;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TeleportRandomlyConsumeEffect.class)
public abstract class TeleportRandomlyConsumeEffectMixin {
    /** Prevent chorus fruit teleporting into protected regions. */
    @Redirect(
        method = "onConsume",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/LivingEntity;teleport(DDDZ)Z",
            ordinal = 0
        )
    )
    private boolean inject$finishUsing(
        LivingEntity LE,
        double X,
        double Y,
        double Z,
        boolean ParticleEffects
    ) {
        var To = new BlockPos((int) X, (int) Y, (int) Z);
        if (!ProtectionManager.AllowTeleport(LE, LE.getWorld(), To)) return false;
        return LE.teleport(X, Y, Z, ParticleEffects);
    }
}
