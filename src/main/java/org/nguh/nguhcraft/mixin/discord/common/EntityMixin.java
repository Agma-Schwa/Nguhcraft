package org.nguh.nguhcraft.mixin.discord.common;

import net.minecraft.world.entity.Entity;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
    /**
    * Disable inclusion of teams in the display name since that
    * messes with our Discord integration (for wolf pet names, the
    * player name is included in the display name by this, which is
    * not what we want.
    */
    @Inject(method = "getDisplayName", at = @At("HEAD"), cancellable = true)
    private void inject$getDisplayName(CallbackInfoReturnable<Component> CIR) {
        CIR.setReturnValue(((Entity) (Object) this).getName());
    }
}
