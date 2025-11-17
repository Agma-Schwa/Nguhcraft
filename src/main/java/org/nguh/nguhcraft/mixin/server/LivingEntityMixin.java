package org.nguh.nguhcraft.mixin.server;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.nguh.nguhcraft.server.HypershotContext;
import org.nguh.nguhcraft.server.ServerUtils;
import org.nguh.nguhcraft.server.accessors.LivingEntityAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements LivingEntityAccessor {
    public LivingEntityMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    /** The current hypershot context, if any. */
    @Unique @Nullable private HypershotContext HSContext = null;

    /** Accessors for the hypershot context. */
    @Override public void setHypershotContext(HypershotContext context) { HSContext = context; }
    @Override @Nullable public HypershotContext getHypershotContext() { return HSContext; }

    @Unique private LivingEntity This() { return (LivingEntity) (Object) this; }

    /** Checks that need to run when an entity is ticked. */
    @Inject(method = "baseTick()V", at = @At("HEAD"))
    private void inject$baseTick(CallbackInfo CI) {
        ServerUtils.ActOnLivingEntityBaseTick(This());
    }

    /** Tick Hypershot. */
    @Inject(method = "updatingUsingItem()V", at = @At("HEAD"), cancellable = true)
    private void inject$tickActiveItemStack(CallbackInfo CI) {
        if (
            HSContext != null &&
            HSContext.Tick((ServerLevel) level(), This()) != HypershotContext.EXPIRED
        ) CI.cancel();
    }
}
