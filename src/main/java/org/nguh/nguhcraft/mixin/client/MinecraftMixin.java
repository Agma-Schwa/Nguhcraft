package org.nguh.nguhcraft.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.social.SocialInteractionsScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.jetbrains.annotations.Nullable;
import org.nguh.nguhcraft.client.NguhcraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Shadow @Nullable public LocalPlayer player;
    @Shadow @Nullable public ClientLevel level;
    @Unique static private final Component REPORT_SCREEN_DISABLED =
        Component.literal("Social interactions screen is not supported by Nguhcraft!").withStyle(ChatFormatting.RED);

    /** Prevent using an item while in a hypershot context. */
    @Inject(method = "startUseItem()V", at = @At("HEAD"), cancellable = true)
    private void inject$doItemUse$0(CallbackInfo CI) {
        if (NguhcraftClient.InHypershotContext) CI.cancel();
    }

    /**
    * Disable profile keys.
    *
    * @author Sirraide
    * @reason We don’t do any signing, so this is useless.
    */
    @Overwrite
    public ProfileKeyPairManager getProfileKeyPairManager() { return ProfileKeyPairManager.EMPTY_KEY_MANAGER; }

    /**
    * Also disable telemetry because why not.
    *
    * @author Sirraide
    * @reason No reason to keep this enabled.
    * */
    @Overwrite
    public boolean allowsTelemetry() { return false; }

    /** Prevent the social interactions screen from opening. */
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void inject$setScreen$0(Screen S, CallbackInfo CI) {
        if (S instanceof SocialInteractionsScreen) {
            if (player != null) player.displayClientMessage(REPORT_SCREEN_DISABLED, false);
            CI.cancel();
        }
    }
}
