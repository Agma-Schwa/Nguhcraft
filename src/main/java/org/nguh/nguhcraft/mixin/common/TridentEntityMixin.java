package org.nguh.nguhcraft.mixin.common;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;
import org.nguh.nguhcraft.TridentUtils;
import org.nguh.nguhcraft.accessors.TridentEntityAccessor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TridentEntity.class)
public abstract class TridentEntityMixin extends PersistentProjectileEntity implements TridentEntityAccessor {
    @Shadow @Final private static TrackedData<Byte> LOYALTY;

    @Shadow private boolean dealtDamage;

    protected TridentEntityMixin(EntityType<? extends PersistentProjectileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Unique boolean Copy = false;

    /** To mark that we have struck lightning so the client can render fire. */
    @Unique private static final TrackedData<Boolean> STRUCK_LIGHTNING
        = DataTracker.registerData(TridentEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    /** Mark this as a copy. */
    @Override
    @Unique
    public void SetCopy() {
        Copy = true;
        pickupType = PickupPermission.CREATIVE_ONLY;
        dataTracker.set(LOYALTY, (byte) 0);
        setOwner((Entity) null);
    }

    /** Mark this as having struck lightning. */
    @Override public void Nguhcraft$SetStruckLightning() {
        dataTracker.set(STRUCK_LIGHTNING, true);
    }

    /** Whether this has dealt damage. */
    @Override public boolean Nguhcraft$DealtDamage() { return dealtDamage; }

    /** If this has struck lightning, render with blue fire. */
    @Override public boolean doesRenderOnFire() { return dataTracker.get(STRUCK_LIGHTNING); }

    /** Initialise data tracker. */
    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void inject$initDataTracker(DataTracker.Builder B, CallbackInfo CI) {
        B.add(STRUCK_LIGHTNING, false);
    }

    /** Implement Channeling II. */
    @Inject(
        method = "onEntityHit(Lnet/minecraft/util/hit/EntityHitResult;)V",
        cancellable = true,
        at = @At(
            value = "INVOKE",
            target = "net/minecraft/entity/projectile/TridentEntity.setVelocity (Lnet/minecraft/util/math/Vec3d;)V",
            ordinal = 0,
            shift = At.Shift.AFTER
        )
    )
    private void inject$onEntityHit(EntityHitResult EHR, CallbackInfo CI) {
        TridentUtils.ActOnEntityHit((TridentEntity) (Object) this, EHR);
        CI.cancel();
    }

    /** Discard copied tridents after 5 seconds. */
    @Inject(
        method = "tick()V",
        cancellable = true,
        at = @At(
            value = "INVOKE",
            target = "net/minecraft/entity/projectile/TridentEntity.getOwner ()Lnet/minecraft/entity/Entity;",
            ordinal = 0
        )
    )
    private void inject$tick(CallbackInfo CI) {
        if (Copy) {
            if (getWorld() instanceof ServerWorld && inGroundTime > 100) discard();
            super.tick();
            CI.cancel();
        }
    }

    /** Implement Channeling II. */
    @Override
    protected void onBlockHit(BlockHitResult BHR) {
        TridentUtils.ActOnBlockHit((TridentEntity) (Object) this, BHR);
    }
}
