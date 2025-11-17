package org.nguh.nguhcraft.mixin.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.projectile.Fireball;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.Level;
import org.nguh.nguhcraft.entity.GhastModeAccessor;
import org.nguh.nguhcraft.entity.MachineGunGhastMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LargeFireball.class)
public abstract class LargeFireballMixin extends Fireball {
    public LargeFireballMixin(EntityType<? extends Fireball> entityType, Level world) {
        super(entityType, world);
    }

    @Unique
    private MachineGunGhastMode GetMode() {
        var O = getOwner();
        if (O instanceof Ghast GH) return ((GhastModeAccessor)GH).Nguhcraft$GetGhastMode();
        return MachineGunGhastMode.NORMAL;
    }

    /** Prevent fireballs from colliding with one another. */
    @Inject(method = "onHit", at = @At("HEAD"), cancellable = true)
    private void inject$onCollision$1(HitResult HR, CallbackInfo CI) {
        if (HR.getType() == HitResult.Type.ENTITY) {
            var EHR = (EntityHitResult) HR;
            if (EHR.getEntity() instanceof LargeFireball) CI.cancel();
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
        method = "onHitEntity",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z")
    )
    private boolean inject$onEntityHit(Entity E, ServerLevel SW, DamageSource DS, float V) {
        return E.hurtServer(SW, DS, V * Math.max(1, GetMode().ordinal() - 1));
    }
}