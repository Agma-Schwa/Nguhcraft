package org.nguh.nguhcraft.mixin.common;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.nguh.nguhcraft.block.NguhBlocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BoneMealItem.class)
public abstract class BoneMealItemMixin {
    /** Allow duplicating flowers by bonemealing them. */
    @Inject(method = "growCrop", at = @At("HEAD"), cancellable = true)
    static private void inject$useOnFertilizable(ItemStack S, Level W, BlockPos Pos, CallbackInfoReturnable<Boolean> CIR) {
        var St = W.getBlockState(Pos);
        if (St.is(NguhBlocks.CAN_DUPLICATE_WITH_BONEMEAL)) {
            if (W instanceof ServerLevel SW) {
                Block.popResource(SW, Pos, new ItemStack(St.getBlock()));
                S.shrink(1);
            }

            CIR.setReturnValue(true);
        }
    }
}
