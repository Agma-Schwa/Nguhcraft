package org.nguh.nguhcraft.mixin.protect.server;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.level.Level;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ThrownEnderpearl.class)
public abstract class ThrownEnderpearlMixin extends ThrowableItemProjectile {
    private ThrownEnderpearlMixin(EntityType<? extends ThrowableItemProjectile> entityType, Level world) {
        super(entityType, world);
    }

    @Shadow private static boolean isAllowedToTeleportOwner(Entity entity, Level world) { return false; }

/** Prevent teleportation into protected areas. */
    @Redirect(
        method = "onHit",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/projectile/ThrownEnderpearl;isAllowedToTeleportOwner(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/Level;)Z",
            ordinal = 0
        )
    )
    private boolean inject$onCollision$0(Entity Owner, Level W) {
        if (!ProtectionManager.AllowTeleport(Owner, W, this.blockPosition())) return false;
        return isAllowedToTeleportOwner(Owner, W);
    }
}
