package org.nguh.nguhcraft.mixin.common;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.NonNull;
import org.nguh.nguhcraft.TridentUtils;
import org.nguh.nguhcraft.accessors.TridentEntityAccessor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
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

    /** Whether this has struck lightning. */
    @Override public boolean Nguhcraft$GetStruckLightning() { return entityData.get(STRUCK_LIGHTNING); }

    /** If this has struck lightning, render with blue fire. */
    @Override public boolean displayFireAnimation() { return entityData.get(STRUCK_LIGHTNING); }

    /** Initialise data tracker. */
    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void inject$initDataTracker(SynchedEntityData.Builder B, CallbackInfo CI) {
        B.define(STRUCK_LIGHTNING, false);
    }

    /** Discard copied tridents after 5 seconds. */
    @Inject(
        method = "tick()V",
        cancellable = true,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/projectile/arrow/ThrownTrident;getOwner()Lnet/minecraft/world/entity/Entity;",
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

    /** Implement Channelling I and II. */
    @Inject(method = "onHitEntity", at = @At("TAIL"))
    private void inject$onEntityHit$1(EntityHitResult EHR, CallbackInfo CI) {
        TridentUtils.ActOnEntityHit((ThrownTrident)(Object)this, EHR);
    }

    /**
     * Disable the sound effect played when a trident hits an entity.
     * <p>
     * We instead emit the appropriate sound effect in ActOnEntityHit() above.
     */
    @Redirect(
        method = "onHitEntity",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/projectile/arrow/ThrownTrident;playSound(Lnet/minecraft/sounds/SoundEvent;FF)V"
        )
    )
    private void inject$onEntityHit$2(ThrownTrident This, SoundEvent SE, float Volume, float Pitch) {}

    /** Implement Channelling I and II. */
    @Override
    protected void onHitBlock(@NonNull BlockHitResult BHR) {
        // Calling this is fine; we have disabled the normal Channelling behaviour
        // by overwriting 'channeling.json', so this will do everything a projectile
        // is supposed to do except trigger channelling.
        super.onHitBlock(BHR);

        // This handles Channelling I and II.
        TridentUtils.ActOnBlockHit((ThrownTrident)(Object)this, BHR);
    }
}
