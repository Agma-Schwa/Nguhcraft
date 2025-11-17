package org.nguh.nguhcraft.mixin.client;

import net.minecraft.client.gui.BundleMouseActions;
import net.minecraft.world.inventory.Slot;
import org.nguh.nguhcraft.item.KeyChainItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BundleMouseActions.class)
public abstract class BundleMouseActionsMixin {
    @Inject(method = "matches", at = @At("HEAD"), cancellable = true)
    private void inject$isApplicableTo(Slot S, CallbackInfoReturnable<Boolean> CIR) {
        if (KeyChainItem.is(S.getItem()))
            CIR.setReturnValue(true);
    }
}
