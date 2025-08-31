package org.nguh.nguhcraft.mixin.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.GhastEntity;
import net.minecraft.entity.projectile.AbstractFireballEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.explosion.ExplosionBehavior;
import org.nguh.nguhcraft.entity.GhastModeAccessor;
import org.nguh.nguhcraft.entity.MachineGunGhastMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FireballEntity.class)
public abstract class FireballEntityMixin extends AbstractFireballEntity {
    public FireballEntityMixin(EntityType<? extends AbstractFireballEntity> entityType, World world) {
        super(entityType, world);
    }

    @Unique
    private MachineGunGhastMode GetMode() {
        var O = getOwner();
        if (O instanceof GhastEntity GH) return ((GhastModeAccessor)GH).Nguhcraft$GetGhastMode();
        return MachineGunGhastMode.NORMAL;
    }

    /** Prevent fireballs from colliding with one another. */
    @Inject(method = "onCollision", at = @At("HEAD"), cancellable = true)
    private void inject$onCollision$1(HitResult HR, CallbackInfo CI) {
        if (HR.getType() == HitResult.Type.ENTITY) {
            var EHR = (EntityHitResult) HR;
            if (EHR.getEntity() instanceof FireballEntity) CI.cancel();
        }
    }

    // TODO: Reduce knockback
    /*
    // Reduce explosion knockback.
    @Redirect(method = "onCollision", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;createExplosion(Lnet/minecraft/entity/Entity;DDDFZLnet/minecraft/world/World$ExplosionSourceType;)V"))
    private void inject$onCollision$2(
        World W,
        Entity E,
        double X,
        double Y,
        double Z,
        float Power,
        boolean CreateFire,
        World.ExplosionSourceType EST
    ) {
        W.createExplosion(
            E,
            Explosion.createDamageSource(W, E),
            MachineGunGhastExplosionBehavior.For(GetMode())
        );
    }*/

    /** Make fireballs more damaging. */
    @Redirect(
        method = "onEntityHit",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;damage(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;F)Z")
    )
    private boolean inject$onEntityHit(Entity E, ServerWorld SW, DamageSource DS, float V) {
        return E.damage(SW, DS, V * Math.max(1, GetMode().ordinal() - 1));
    }
}