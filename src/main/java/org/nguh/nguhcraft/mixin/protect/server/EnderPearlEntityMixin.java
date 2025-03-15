package org.nguh.nguhcraft.mixin.protect.server;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.world.World;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EnderPearlEntity.class)
public abstract class EnderPearlEntityMixin extends ThrownItemEntity  {
    private EnderPearlEntityMixin(EntityType<? extends ThrownItemEntity> entityType, World world) {
        super(entityType, world);
    }

    @Shadow private static boolean canTeleportEntityTo(Entity entity, World world) { return false; }

    /** Prevent teleportation into protected areas. */
    @Redirect(
        method = "onCollision",
        at = @At(
            value = "INVOKE",
            target = "net/minecraft/entity/projectile/thrown/EnderPearlEntity.canTeleportEntityTo (Lnet/minecraft/entity/Entity;Lnet/minecraft/world/World;)Z",
            ordinal = 0
        )
    )
    private boolean inject$onCollision$0(Entity Owner, World W) {
        if (!ProtectionManager.AllowTeleport(Owner, W, this.getBlockPos())) return false;
        return canTeleportEntityTo(Owner, W);
    }
}
