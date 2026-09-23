package org.nguh.nguhcraft.mixin.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.social.SocialInteractionsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {
    @Unique static private final Component REPORT_SCREEN_DISABLED =
        Component.literal("Social interactions screen is not supported by Nguhcraft!").withStyle(ChatFormatting.RED);

    /** Prevent the social interactions screen from opening. */
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void inject$setScreen$0(Screen S, CallbackInfo CI) {
        if (S instanceof SocialInteractionsScreen) {
            var Player = Minecraft.getInstance().player;
            if (Player != null) Player.sendSystemMessage(REPORT_SCREEN_DISABLED);
            CI.cancel();
        }
    }
}
