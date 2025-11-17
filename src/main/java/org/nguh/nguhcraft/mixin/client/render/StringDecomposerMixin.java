package org.nguh.nguhcraft.mixin.client.render;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.network.chat.Style;
import net.minecraft.util.StringDecomposer;
import org.nguh.nguhcraft.client.ClientUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StringDecomposer.class)
public abstract class StringDecomposerMixin {
    /**
     * Normalise text before rendering so combining characters work.
     */
    @Inject(
        method = "iterateFormatted(Ljava/lang/String;ILnet/minecraft/network/chat/Style;Lnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z",
        at = @At("HEAD")
    )
    private static void inject$visitFormatted(
            String T,
            int SI,
            Style SS,
            Style RS,
            FormattedCharSink Vis,
            CallbackInfoReturnable<Boolean> CIR,
            @Local(argsOnly = true) LocalRef<String> Text
    ) {
        Text.set(ClientUtils.RenderText(Text.get()));
    }
}
