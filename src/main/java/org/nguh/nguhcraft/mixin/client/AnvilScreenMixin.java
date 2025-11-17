package org.nguh.nguhcraft.mixin.client;

import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import org.nguh.nguhcraft.Constants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(AnvilScreen.class)
public abstract class AnvilScreenMixin {
    @ModifyConstant(method = "renderLabels", constant = @Constant(intValue = 40, ordinal = 0))
    private int inject$updateResult$1(int i) { return Constants.ANVIL_LIMIT; }
}
