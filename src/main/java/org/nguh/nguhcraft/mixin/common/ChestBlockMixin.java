package org.nguh.nguhcraft.mixin.common;

import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.nguh.nguhcraft.accessors.ChestBlockEntityAccessor;
import org.nguh.nguhcraft.block.NguhBlocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChestBlock.class)
public abstract class ChestBlockMixin {
    /** The item stack being placed while in getStateForPlacement(). */
    @Unique private static final ThreadLocal<ItemStack> PlacingStack = new ThreadLocal<>();

    @Inject(method = "getStateForPlacement", at = @At("HEAD"))
    private void inject$getStateForPlacement$0(BlockPlaceContext Ctx, CallbackInfoReturnable<BlockState> CIR) {
        PlacingStack.set(Ctx.getItemInHand());
    }

    @Inject(method = "getStateForPlacement", at = @At("RETURN"))
    private void inject$getStateForPlacement$1(BlockPlaceContext Ctx, CallbackInfoReturnable<BlockState> CIR) {
        PlacingStack.remove();
    }

    /** Prevent different chest variants from merging. */
    @Inject(method = "candidatePartnerFacing", at = @At("HEAD"), cancellable = true)
    private void inject$getNeighborChestDirection(
        Level L,
        BlockPos Pos,
        Direction D,
        CallbackInfoReturnable<Direction> CIR
    ) {
        var St = PlacingStack.get();
        if (St == null) return;
        var Variant = St.get(NguhBlocks.CHEST_VARIANT_COMPONENT);
        var BE = L.getBlockEntity(Pos.relative(D));
        if (BE instanceof ChestBlockEntity CBE) {
            var OtherVariant = ((ChestBlockEntityAccessor)CBE).Nguhcraft$GetChestVariant();
            if (OtherVariant != Variant) CIR.setReturnValue(null);
        }
    }
}
