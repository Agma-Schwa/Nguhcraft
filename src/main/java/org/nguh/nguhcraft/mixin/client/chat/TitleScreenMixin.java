package org.nguh.nguhcraft.mixin.client.chat;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    protected TitleScreenMixin(Component title) { super(title); }

    /** Disable the 'Minecraft Realms' button; it wouldn’t work anyway. */
    @Inject(method = "createNormalMenuOptions", at = @At("RETURN"))
    private void inject$initWidgetsNormal(int y, int spacingY, CallbackInfoReturnable<Integer> cir) {
        ((Button)children().getLast()).active = false;
    }
}
