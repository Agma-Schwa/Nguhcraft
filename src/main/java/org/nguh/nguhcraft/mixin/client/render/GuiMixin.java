package org.nguh.nguhcraft.mixin.client.render;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Gui;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import org.nguh.nguhcraft.Utils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.nguh.nguhcraft.Nguhcraft.Id;

@Mixin(Gui.class)
public abstract class GuiMixin {
    @Unique private static final ResourceLocation ARMOR_GOLD_FULL_TEXTURE = Id("hud/armour_gold_full");
    @Unique private static final ResourceLocation ARMOR_GOLD_HALF_TEXTURE = Id("hud/armour_gold_half");

    /**
    * Don’t render the hunger bar if the saturation enchantment is maxed out.
    * <p>
    * If the player’s total saturation enchantment level is high enough to
    * where they will never lose hunger, and the food level is also full, then
    * there is no point in rendering the hunger bar since it conveys no useful
    * information.
    */
    @Inject(method = "renderFood", at = @At("HEAD"), cancellable = true)
    private void inject$renderFood(GuiGraphics Ctx, Player P, int Top, int Right, CallbackInfo CI) {
        var HM = P.getFoodData();

        // Always render the food level if the bar isn’t full.
        if (HM.needsFood()) return;

        // Do not render it if the saturation level is maxed out.
        if (Utils.CalculateWeightedSaturationEnchantmentValue(P) >= Utils.MAX_SATURATION_ENCHANTMENT_VALUE)
            CI.cancel();
    }

    /** Render more than 20 armour points, up to a maximum of 40. */
    @Inject(method = "renderArmor", at = @At("TAIL"))
    private static void inject$renderArmor(GuiGraphics Ctx, Player P, int Ht, int J, int K, int Wd, CallbackInfo CI) {
        var Armour = P.getArmorValue() - 20;
        if (Armour > 0) {
            var Y = Ht - (J - 1) * K - 10;
            for (var N = 0; N < 10; N++) {
                var X = Wd + N * 8;
                if (N * 2 + 1 < Armour)
                    Ctx.blitSprite(RenderPipelines.GUI_TEXTURED, ARMOR_GOLD_FULL_TEXTURE, X, Y, 9, 9);
                if (N * 2 + 1 == Armour)
                    Ctx.blitSprite(RenderPipelines.GUI_TEXTURED, ARMOR_GOLD_HALF_TEXTURE, X, Y, 9, 9);
            }
        }
    }
}
