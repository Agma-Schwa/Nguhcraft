package org.nguh.nguhcraft.mixin.common;

import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.util.math.Direction;
import org.nguh.nguhcraft.accessors.ChestBlockEntityAccessor;
import org.nguh.nguhcraft.block.NguhBlocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChestBlock.class)
public abstract class ChestBlockMixin {
    /** Prevent different chest variants from merging. */
    @Inject(method = "getNeighborChestDirection", at = @At("HEAD"), cancellable = true)
    private void inject$getNeighborChestDirection(
        ItemPlacementContext Ctx,
        Direction D,
        CallbackInfoReturnable<Direction> CIR
    ) {
        var Variant = Ctx.getStack().get(NguhBlocks.CHEST_VARIANT_COMPONENT);
        var BE = Ctx.getWorld().getBlockEntity(Ctx.getBlockPos().offset(D));
        if (BE instanceof ChestBlockEntity CBE) {
            var OtherVariant = ((ChestBlockEntityAccessor)CBE).Nguhcraft$GetChestVariant();
            if (OtherVariant != Variant) CIR.setReturnValue(null);
        }
    }
}
