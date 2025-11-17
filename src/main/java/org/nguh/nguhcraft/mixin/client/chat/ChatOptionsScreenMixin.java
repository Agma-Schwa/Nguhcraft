package org.nguh.nguhcraft.mixin.client.chat;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.ChatOptionsScreen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ChatOptionsScreen.class)
public abstract class ChatOptionsScreenMixin extends OptionsSubScreen {
    public ChatOptionsScreenMixin(Screen parent, Options gameOptions, Component title) {
        super(parent, gameOptions, title);
    }

    /** Disable the ‘Only Show Secure Chat’ option; it doesn’t do anything anyway. */
    @Override
    protected void init() {
        super.init();
        var W = list.findOption(minecraft.options.onlyShowSecureChat());
        if (W != null) W.active = false;
    }
}
