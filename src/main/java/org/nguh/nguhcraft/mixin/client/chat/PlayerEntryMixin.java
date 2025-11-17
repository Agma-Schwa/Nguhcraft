package org.nguh.nguhcraft.mixin.client.chat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.social.PlayerEntry;
import net.minecraft.client.gui.screens.social.SocialInteractionsScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.resources.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;
import java.util.function.Supplier;

@Mixin(PlayerEntry.class)
public abstract class PlayerEntryMixin {
    @Shadow private Button reportButton;

    /** Disable report button. */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void inject$init(
            Minecraft client,
            SocialInteractionsScreen parent,
            UUID uuid,
            String name,
            Supplier<PlayerSkin> skinTexture,
            boolean reportable,
            CallbackInfo ci
    ) {
        reportButton.active = false;
    }
}
