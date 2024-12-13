package org.nguh.nguhcraft.mixin.server;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Shadow public int timeUntilRegen;

    @Unique private Entity This() { return (Entity) (Object) this; }

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
}
