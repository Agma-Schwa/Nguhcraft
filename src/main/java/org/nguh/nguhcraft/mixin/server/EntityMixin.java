package org.nguh.nguhcraft.mixin.server;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
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
    @Shadow public int timeUntilRegen;

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
    @Inject(method = "onStruckByLightning", at = @At("HEAD"), cancellable = true)
    private void inject$onStruckByLightning(ServerWorld SW, LightningEntity LE, CallbackInfo ci) {
        if (LE.getStruckEntities().anyMatch(E -> E == This())) ci.cancel();
        timeUntilRegen = 0;
    }

    /** Prevent managed entities from travelling through portals. */
    @Inject(method = "canUsePortals", at = @At("HEAD"), cancellable = true)
    private void inject$onCanUsePortals(boolean AllowVehicles, CallbackInfoReturnable<Boolean> CIR) {
        if (Data.getManagedBySpawnPos()) CIR.setReturnValue(false);
    }

    @Inject(
        method = "readData",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;readCustomData(Lnet/minecraft/storage/ReadView;)V"
        )
    )
    private void inject$readData(ReadView RV, CallbackInfo CI) {
        RV.read(NguhcraftEntityData.TAG_ROOT, NguhcraftEntityData.CODEC).ifPresent(D -> Data = D);
    }

    @Inject(
        method = "writeData",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;writeCustomData(Lnet/minecraft/storage/WriteView;)V"
        )
    )
    private void inject$writeData(WriteView WV, CallbackInfo CI) {
        WV.put(NguhcraftEntityData.TAG_ROOT, NguhcraftEntityData.CODEC, Data);
    }
}
