package org.nguh.nguhcraft.mixin.client.render;


import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.Component;
import org.nguh.nguhcraft.client.ClientUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Consumer;

@Mixin(ItemLore.class)
public abstract class ItemLoreMixin {
    @Shadow
    @Final
    private List<Component> styledLines;

    /** Respect newline characters in lore text. */
    @Inject(method = "addToTooltip", at = @At("HEAD"), cancellable = true)
    private void inject$appendTooltip(
        Item.TooltipContext Ctx,
        Consumer<Component> TextConsumer,
        TooltipFlag Type,
        DataComponentGetter Components,
        CallbackInfo CI
    ) {
        if (ClientUtils.FormatLoreForTooltip(TextConsumer, styledLines))
            CI.cancel();
    }
}
