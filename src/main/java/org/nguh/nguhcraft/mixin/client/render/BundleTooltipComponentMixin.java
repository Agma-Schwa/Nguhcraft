package org.nguh.nguhcraft.mixin.client.render;

import net.minecraft.client.gui.tooltip.BundleTooltipComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.nguh.nguhcraft.item.KeyChainItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BundleTooltipComponent.class)
public abstract class BundleTooltipComponentMixin {
    /** Render the key/lock’s ID. Do this even for actual bundles. */
    @Redirect(method = "drawSelectedItemTooltip", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/item/ItemStack;getFormattedName()Lnet/minecraft/text/Text;"
    ))
    private Text inject$drawSelectedItemTooltip(ItemStack St) {
        return KeyChainItem.GetKeyTooltip(St);
    }
}
