package org.nguh.nguhcraft.mixin.protect;


import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.level.Explosion;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
* Mixin to make protected entities immune to explosions.
* <p>
* Needs to be added to each class that overrides Entity#isImmuneToExplosion
* since the derived classes unfortunately don’t call the base class method.
*/
@Mixin(value = {Entity.class, ArmorStand.class, Warden.class})
public abstract class Entity_ArmorStand_Warden_IsImmuneToExplosionMixin {
    @Inject(method = "ignoreExplosion", at = @At("HEAD"), cancellable = true)
    private void inject$isImmuneToExplosion(Explosion explosion, CallbackInfoReturnable<Boolean> CIR) {
        // Note that it is the *Entity’s* position that matters, not the explosion’s.
        if (ProtectionManager.IsProtectedEntity((Entity)(Object)this))
            CIR.setReturnValue(true);
    }
}
