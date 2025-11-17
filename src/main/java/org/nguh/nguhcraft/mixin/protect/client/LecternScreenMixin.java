package org.nguh.nguhcraft.mixin.protect.client;

import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.gui.screens.inventory.LecternScreen;
import org.nguh.nguhcraft.client.NguhcraftClient;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LecternScreen.class)
public abstract class LecternScreenMixin extends BookViewScreen {
    /** Do not show the ‘Take Book’ button if the lectern is protected. */
    @Inject(method = "createMenuControls", at = @At("HEAD"), cancellable = true)
    private void inject$addCloseButton(CallbackInfo CI) {
        var Pos = NguhcraftClient.LastInteractedLecternPos;
        if (!ProtectionManager.AllowBlockModify(
            minecraft.player,
            minecraft.level,
            NguhcraftClient.LastInteractedLecternPos
        )) {
            super.createMenuControls();
            CI.cancel();
        }
    }
}
