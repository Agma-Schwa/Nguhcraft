package org.nguh.nguhcraft.mixin.client;

import net.minecraft.client.KeyboardHandler;
import org.nguh.nguhcraft.client.NguhcraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
    @Inject(method = "handleDebugKeys", at = @At("HEAD"), cancellable = true)
    private void inject$handleDebugKeys(int K, CallbackInfoReturnable<Boolean> CIR) {
        if (NguhcraftClient.ProcessF3(K)) CIR.setReturnValue(true);
    }
}
