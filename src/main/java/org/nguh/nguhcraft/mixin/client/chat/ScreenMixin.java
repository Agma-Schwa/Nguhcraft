package org.nguh.nguhcraft.mixin.client.chat;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Shadow public static boolean hasShiftDown() { return false; }
    @Shadow public static boolean hasControlDown() { return false; }
    @Shadow public static boolean hasAltDown() { return false; }

    /** Support SHIFT+INSERT as paste. */
    @Inject(method = "isPaste", at = @At("HEAD"), cancellable = true)
    private static void inject$isPaste(int K, CallbackInfoReturnable<Boolean> CIR) {
        if (hasShiftDown() && !hasControlDown() && !hasAltDown() && K == InputUtil.GLFW_KEY_INSERT)
            CIR.setReturnValue(true);
    }
}
