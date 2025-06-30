package org.nguh.nguhcraft.mixin.common;

import net.minecraft.block.Block;
import net.minecraft.item.BoneMealItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.nguh.nguhcraft.block.NguhBlocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BoneMealItem.class)
public abstract class BoneMealItemMixin {
    /** Allow duplicating flowers by bonemealing them. */
    @Inject(method = "useOnFertilizable", at = @At("HEAD"), cancellable = true)
    static private void inject$useOnFertilizable(ItemStack S, World W, BlockPos Pos, CallbackInfoReturnable<Boolean> CIR) {
        var St = W.getBlockState(Pos);
        if (St.isIn(NguhBlocks.CAN_DUPLICATE_WITH_BONEMEAL)) {
            if (W instanceof ServerWorld SW) {
                Block.dropStack(SW, Pos, new ItemStack(St.getBlock()));
                S.decrement(1);
            }

            CIR.setReturnValue(true);
        }
    }
}
