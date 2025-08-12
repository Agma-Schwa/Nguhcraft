package org.nguh.nguhcraft.mixin.server;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import org.nguh.nguhcraft.entity.EntitySpawnManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin implements EntitySpawnManager.EntityAccess {
    @Shadow public int timeUntilRegen;

    @Unique private static final String TAG_MANAGED_BY_SPAWN_POS = "NguhcraftManagedBySpawnPos";
    @Unique private boolean ManagedBySpawnPos = false;

    @Unique private Entity This() { return (Entity) (Object) this; }
    @Override public void Nguhcraft$SetManagedBySpawnPos() { ManagedBySpawnPos = true; }

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
        if (ManagedBySpawnPos) CIR.setReturnValue(false);
    }

    @Inject(method = "readNbt", at = @At("HEAD"))
    private void inject$readNbt(NbtCompound Tag, CallbackInfo CI) {
        ManagedBySpawnPos = Tag.getBoolean(TAG_MANAGED_BY_SPAWN_POS);
    }

    @Inject(method = "writeNbt", at = @At("HEAD"))
    private void inject$writeNbt(NbtCompound Tag, CallbackInfoReturnable<NbtCompound> CIR) {
        if (ManagedBySpawnPos) Tag.putBoolean(TAG_MANAGED_BY_SPAWN_POS, true);
    }
}
