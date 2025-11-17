package org.nguh.nguhcraft.mixin.protect.server;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.consume_effects.TeleportRandomlyConsumeEffect;
import net.minecraft.core.BlockPos;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TeleportRandomlyConsumeEffect.class)
public abstract class TeleportRandomlyConsumeEffectMixin {
    /** Prevent chorus fruit teleporting into protected regions. */
    @Redirect(
        method = "apply",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;randomTeleport(DDDZ)Z",
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
        if (!ProtectionManager.AllowTeleport(LE, LE.level(), To)) return false;
        return LE.randomTeleport(X, Y, Z, ParticleEffects);
    }
}
