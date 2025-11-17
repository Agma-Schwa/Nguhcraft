package org.nguh.nguhcraft.mixin.server;

import net.minecraft.world.food.FoodData;
import net.minecraft.server.level.ServerPlayer;
import org.nguh.nguhcraft.Utils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodData.class)
public abstract class FoodDataMixin {
    @Shadow private float exhaustionLevel;

    /** Implement saturation enchantment */
    @Inject(method = "tick", at = @At("HEAD"))
    private void inject$update(ServerPlayer P, CallbackInfo CI) {
        // If the player’s exhaustion is not yet at the point where they
        // would start losing hunger, don’t bother checking anything else.
        if (exhaustionLevel < 4F) return;

        // Prevent hunger loss with a probability proportional to the total
        // weighted saturation level. Don’t bother rolling if the total is 0
        // or 8 (= 100%).
        var Total = Utils.CalculateWeightedSaturationEnchantmentValue(P);
        if (Total == 0) return;
        if (
            Total >= Utils.MAX_SATURATION_ENCHANTMENT_VALUE ||
            P.level().random.nextFloat() < Total * .125F
        ) exhaustionLevel = 0.F;
    }
}
