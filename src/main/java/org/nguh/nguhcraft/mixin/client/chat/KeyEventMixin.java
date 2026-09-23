package org.nguh.nguhcraft.mixin.client.chat;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(KeyEvent.class)
public abstract class KeyEventMixin implements InputWithModifiers {
    /** Support SHIFT+INSERT as paste. */
    @Override
    public boolean isPaste() {
        if (hasShiftDown() && !hasControlDown() && !hasAltDown() && input() == InputConstants.KEY_INSERT)
            return true;
        return InputWithModifiers.super.isPaste();
    }
}
