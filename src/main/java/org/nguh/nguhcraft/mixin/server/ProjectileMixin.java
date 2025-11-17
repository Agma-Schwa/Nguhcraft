package org.nguh.nguhcraft.mixin.server;

import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.nguh.nguhcraft.accessors.ProjectileEntityAccessor;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.nguh.nguhcraft.Constants.MAX_HOMING_DISTANCE;

@Mixin(Projectile.class)
public abstract class ProjectileMixin extends Entity implements ProjectileEntityAccessor {
    public ProjectileMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    /** Maximum ticks before we give up. */
    @Unique static private final int MAX_HOMING_TICKS = 60 * 20;

    /** Vertical offset added to the movement vector to avoid collisions. */
    @Unique static private final Vec3 COLLISION_OFFSET = new Vec3(0, 1.5, 0);

    /** The entity we’re homing in on. */
    @Unique @Nullable public Entity Target;

    /** How long we’ve been following the entity. */
    @Unique private int HomingTicks;

    /**
     * Whether this is a hypershot arrow.
     * <p>
     * To make hypershot actually work, we need to cancel an entity’s
     * invulnerability time if it is hit by an arrow that was shot
     * from a hypershot bow. That includes the first arrow shot from
     * the bow as well.
     */
    @Unique private boolean IsHypershotArrow = false;

    /** Turn this into a hypershot arrow. */
    @Override public void MakeHypershotProjectile() { IsHypershotArrow = true; }

    /** Set the homing target. */
    @Override public void SetHomingTarget(LivingEntity Target) {
        this.Target = Target;
        this.HomingTicks = 0;
    }

    /** Disable damage cooldown if an entity is hit with a hypershot arrow. */
    @Inject(method = "onHitEntity", at = @At("TAIL"))
    private void inject$onHit(EntityHitResult EHR, CallbackInfo CI) {
        // FIXME: Could use DamageTags.BYPASSES_COOLDOWN for this.
        if (IsHypershotArrow) EHR.getEntity().invulnerableTime = 0;
    }

    /** Implement the homing enchantment. */
    @Inject(method = "tick()V", at = @At("TAIL"))
    private void inject$tick(CallbackInfo CI) {
        // Not a homing projectile.
        if (Target == null) return;

        // Stop if we’ve been going for too long or the target is gone.
        if (
            HomingTicks++ >= MAX_HOMING_TICKS ||
            isRemoved() ||
            !Target.isAlive() ||
            Target.isRemoved()
        ) {
            Target = null;
            return;
        }

        // Calculate the distance vector to the target.
        var TPos = Target.position();
        var PPos = position();
        var DVec = TPos.subtract(PPos).add(0, Target.getBbHeight() / 2., 0);
        var Dist = DVec.length();
        if (Dist > MAX_HOMING_DISTANCE) {
            Target = null;
            return;
        }

        // If we would collide with an object, jump up.
        if (level().clip(new ClipContext(
            PPos,
            TPos,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            this
        )).isInside()) DVec.add(COLLISION_OFFSET);

        // Adjust movement vector towards the target.
        setDeltaMovement(DVec.normalize());
    }
}
