package org.nguh.nguhcraft.mixin.client.render;

import net.minecraft.client.gui.screens.inventory.tooltip.ClientBundleTooltip;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import org.nguh.nguhcraft.item.KeyChainItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientBundleTooltip.class)
public abstract class ClientBundleTooltipMixin {
    /** Render the key/lock’s ID. Do this even for actual bundles. */
    @Redirect(method = "drawSelectedItemTooltip", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/world/item/ItemStack;getStyledHoverName()Lnet/minecraft/network/chat/Component;"
    ))
    private Component inject$drawSelectedItemTooltip(ItemStack St) {
        return KeyChainItem.GetKeyTooltip(St);
    }
}
