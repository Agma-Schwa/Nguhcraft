package org.nguh.nguhcraft.mixin.common;

import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.core.Direction;
import org.nguh.nguhcraft.accessors.ChestBlockEntityAccessor;
import org.nguh.nguhcraft.block.NguhBlocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChestBlock.class)
public abstract class ChestBlockMixin {
    /** Prevent different chest variants from merging. */
    @Inject(method = "candidatePartnerFacing", at = @At("HEAD"), cancellable = true)
    private void inject$getNeighborChestDirection(
        BlockPlaceContext Ctx,
        Direction D,
        CallbackInfoReturnable<Direction> CIR
    ) {
        var Variant = Ctx.getItemInHand().get(NguhBlocks.CHEST_VARIANT_COMPONENT);
        var BE = Ctx.getLevel().getBlockEntity(Ctx.getClickedPos().relative(D));
        if (BE instanceof ChestBlockEntity CBE) {
            var OtherVariant = ((ChestBlockEntityAccessor)CBE).Nguhcraft$GetChestVariant();
            if (OtherVariant != Variant) CIR.setReturnValue(null);
        }
    }
}
