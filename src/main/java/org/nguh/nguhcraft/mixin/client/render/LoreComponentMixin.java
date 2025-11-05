package org.nguh.nguhcraft.mixin.client.render;


import net.minecraft.component.ComponentsAccess;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.Item;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import org.nguh.nguhcraft.client.ClientUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Consumer;

@Mixin(LoreComponent.class)
public abstract class LoreComponentMixin {
    @Shadow
    @Final
    private List<Text> styledLines;

    /** Respect newline characters in lore text. */
    @Inject(method = "appendTooltip", at = @At("HEAD"), cancellable = true)
    private void inject$appendTooltip(
        Item.TooltipContext Ctx,
        Consumer<Text> TextConsumer,
        TooltipType Type,
        ComponentsAccess Components,
        CallbackInfo CI
    ) {
        if (ClientUtils.FormatLoreForTooltip(TextConsumer, styledLines))
            CI.cancel();
    }
}
