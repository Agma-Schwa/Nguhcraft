package org.nguh.nguhcraft.mixin.client;

import net.minecraft.client.gui.tooltip.BundleTooltipSubmenuHandler;
import net.minecraft.screen.slot.Slot;
import org.nguh.nguhcraft.item.KeyChainItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BundleTooltipSubmenuHandler.class)
public abstract class BundleTooltipSubmenuHandlerMixin {
    @Inject(method = "isApplicableTo", at = @At("HEAD"), cancellable = true)
    private void inject$isApplicableTo(Slot S, CallbackInfoReturnable<Boolean> CIR) {
        if (KeyChainItem.is(S.getStack()))
            CIR.setReturnValue(true);
    }
}
