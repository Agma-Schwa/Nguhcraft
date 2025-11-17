package org.nguh.nguhcraft.mixin.server;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.NotNull;
import org.nguh.nguhcraft.server.NguhcraftEntityData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin implements NguhcraftEntityData.Access {
    @Shadow public int invulnerableTime;

    @Unique private NguhcraftEntityData Data = new NguhcraftEntityData();
    @Unique private Entity This() { return (Entity) (Object) this; }

    @Override public @NotNull NguhcraftEntityData Nguhcraft$GetEntityData() {
        return Data;
    }

    /**
    * Make it so lightning ignores damage cooldown.
    * <p>
    * This allows multishot Channeling tridents to function properly; at
    * the same time, we don’t want entities to be struck by the same lightning
    * bolt more than once, so also check if an entity has already been damaged
    * once before trying to damage it again.
    */
    @Inject(method = "thunderHit", at = @At("HEAD"), cancellable = true)
    private void inject$onStruckByLightning(ServerLevel SW, LightningBolt LE, CallbackInfo ci) {
        if (LE.getHitEntities().anyMatch(E -> E == This())) ci.cancel();
        invulnerableTime = 0;
    }

    /** Prevent managed entities from travelling through portals. */
    @Inject(method = "canUsePortal", at = @At("HEAD"), cancellable = true)
    private void inject$onCanUsePortals(boolean AllowVehicles, CallbackInfoReturnable<Boolean> CIR) {
        if (Data.getManagedBySpawnPos()) CIR.setReturnValue(false);
    }

    @Inject(
        method = "load",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;readAdditionalSaveData(Lnet/minecraft/world/level/storage/ValueInput;)V"
        )
    )
    private void inject$readData(ValueInput RV, CallbackInfo CI) {
        RV.read(NguhcraftEntityData.TAG_ROOT, NguhcraftEntityData.CODEC).ifPresent(D -> Data = D);
    }

    @Inject(
        method = "saveWithoutId",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;addAdditionalSaveData(Lnet/minecraft/world/level/storage/ValueOutput;)V"
        )
    )
    private void inject$writeData(ValueOutput WV, CallbackInfo CI) {
        WV.store(NguhcraftEntityData.TAG_ROOT, NguhcraftEntityData.CODEC, Data);
    }
}
