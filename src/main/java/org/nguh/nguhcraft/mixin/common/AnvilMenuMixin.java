package org.nguh.nguhcraft.mixin.common;

import net.minecraft.world.inventory.AnvilMenu;
import org.nguh.nguhcraft.Constants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {
    /** The limit is 40, so we need to replace a bunch of 40’s here. */
    @ModifyConstant(method = "createResult", constant = @Constant(intValue = 40, ordinal = 0))
    private int inject$updateResult$0(int i) { return Constants.ANVIL_LIMIT; }

    @ModifyConstant(method = "createResult", constant = @Constant(intValue = 40, ordinal = 2))
    private int inject$updateResult$1(int i) { return Constants.ANVIL_LIMIT; }
}
