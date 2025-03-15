package org.nguh.nguhcraft.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.SocialInteractionsScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.session.ProfileKeys;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;
import org.nguh.nguhcraft.client.NguhcraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Shadow @Nullable public ClientPlayerEntity player;
    @Shadow @Nullable public ClientWorld world;
    @Unique static private final Text REPORT_SCREEN_DISABLED =
        Text.literal("Social interactions screen is not supported by Nguhcraft!").formatted(Formatting.RED);

    /** Prevent using an item while in a hypershot context. */
    @Inject(method = "doItemUse()V", at = @At("HEAD"), cancellable = true)
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
    public ProfileKeys getProfileKeys() { return ProfileKeys.MISSING; }

    /**
    * Also disable telemetry because why not.
    *
    * @author Sirraide
    * @reason No reason to keep this enabled.
    * */
    @Overwrite
    public boolean isTelemetryEnabledByApi() { return false; }

    /** Prevent the social interactions screen from opening. */
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void inject$setScreen$0(Screen S, CallbackInfo CI) {
        if (S instanceof SocialInteractionsScreen) {
            if (player != null) player.sendMessage(REPORT_SCREEN_DISABLED, false);
            CI.cancel();
        }
    }
}
