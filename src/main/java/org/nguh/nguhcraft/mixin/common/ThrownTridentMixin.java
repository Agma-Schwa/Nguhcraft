package org.nguh.nguhcraft.mixin.common;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.level.Level;
import org.nguh.nguhcraft.TridentUtils;
import org.nguh.nguhcraft.accessors.TridentEntityAccessor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ThrownTrident.class)
public abstract class ThrownTridentMixin extends AbstractArrow implements TridentEntityAccessor {
    @Shadow @Final private static EntityDataAccessor<Byte> ID_LOYALTY;
    @Unique static private final String COPY_KEY = "NguhcraftCopy";

    @Shadow private boolean dealtDamage;

    protected ThrownTridentMixin(EntityType<? extends AbstractArrow> entityType, Level world) {
        super(entityType, world);
    }

    /** Whether this is a copy and not a real trident. ALWAYS use SetCopy() instead of assigning to this. */
    @Unique boolean Copy = false;

    /** To mark that we have struck lightning so the client can render fire. */
    @Unique private static final EntityDataAccessor<Boolean> STRUCK_LIGHTNING
        = SynchedEntityData.defineId(ThrownTrident.class, EntityDataSerializers.BOOLEAN);

    /**
     * Mark this as a copy.
     * <p>
     * ALWAYS use this instead of assigning to `Copy` directly.
     */
    @Override
    @Unique
    public void Nguhcraft$SetCopy() {
        Copy = true;
        pickup = Pickup.CREATIVE_ONLY;
        entityData.set(ID_LOYALTY, (byte) 0);
        setOwner((Entity) null);
    }

    /** Mark this as having struck lightning. */
    @Override public void Nguhcraft$SetStruckLightning() {
        entityData.set(STRUCK_LIGHTNING, true);
    }

    /** Whether this has dealt damage. */
    @Override public boolean Nguhcraft$DealtDamage() { return dealtDamage; }

    /** If this has struck lightning, render with blue fire. */
    @Override public boolean displayFireAnimation() { return entityData.get(STRUCK_LIGHTNING); }

    /** Initialise data tracker. */
    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void inject$initDataTracker(SynchedEntityData.Builder B, CallbackInfo CI) {
        B.define(STRUCK_LIGHTNING, false);
    }

    /** Implement Channeling II. */
    @Inject(
        method = "onHitEntity(Lnet/minecraft/world/phys/EntityHitResult;)V",
        cancellable = true,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/projectile/ThrownTrident;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V",
            ordinal = 0,
            shift = At.Shift.AFTER
        )
    )
    private void inject$onEntityHit(EntityHitResult EHR, CallbackInfo CI) {
        TridentUtils.ActOnEntityHit((ThrownTrident) (Object) this, EHR);
        CI.cancel();
    }


    /** Discard copied tridents after 5 seconds. */
    @Inject(
        method = "tick()V",
        cancellable = true,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/projectile/ThrownTrident;getOwner()Lnet/minecraft/world/entity/Entity;",
            ordinal = 0
        )
    )
    private void inject$tick(CallbackInfo CI) {
        if (Copy) {
            if (level() instanceof ServerLevel && inGroundTime > 100) discard();
            super.tick();
            CI.cancel();
        }
    }

    /** Load whether this is a copy. */
    @Inject(
        method = "readAdditionalSaveData",
        at = @At("TAIL")
    )
    private void inject$readCustomData(ValueInput RV, CallbackInfo CI) {
        // It does *not* suffice to simply set `Copy` to true here; we *must* also
        // e.g. reset the Loyalty data tracker to 0 etc. for this to behave properly,
        // so make sure to call `SetCopy()` instead.
        if (RV.getBooleanOr(COPY_KEY, false)) Nguhcraft$SetCopy();
    }

    /**
     * Save whether this is a copy.
     * <p>
     * This is to fix an edge case that involves the server being stopped right
     * after a hypershot or multishot trident is thrown; without this, the tridents
     * would be unloaded as regular tridents with creative pickup only and likely
     * hang around for ever (or at least a pretty long time), lagging the server.
     */
    @Inject(
        method = "addAdditionalSaveData",
        at = @At("TAIL")
    )
    private void inject$writeCustomData(ValueOutput WV, CallbackInfo CI) {
        if (Copy) WV.putBoolean(COPY_KEY, true);
    }

    /** Implement Channeling II. */
    @Override
    protected void onHitBlock(BlockHitResult BHR) {
        TridentUtils.ActOnBlockHit((ThrownTrident) (Object) this, BHR);
    }
}
