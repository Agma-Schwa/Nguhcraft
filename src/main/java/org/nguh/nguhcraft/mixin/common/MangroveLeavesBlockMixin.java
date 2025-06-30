package org.nguh.nguhcraft.mixin.common;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.MangroveLeavesBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MangroveLeavesBlock.class)
public abstract class MangroveLeavesBlockMixin {
    /** Allow bonemealing mangrove leaves underwater. */
    @Inject(method = "isFertilizable", at = @At("HEAD"), cancellable = true)
    private void inject$isFertilizable(WorldView W, BlockPos Pos, BlockState St, CallbackInfoReturnable<Boolean> CIR) {
        if (W.getBlockState(Pos.down()).isOf(Blocks.WATER))
            CIR.setReturnValue(true);
    }

    /** And make sure the resulting propagules are waterlogged. */
    @Redirect(
        method = "grow",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/world/ServerWorld;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"
        )
    )
    private boolean inject$grow(ServerWorld SW, BlockPos Pos, BlockState St, int I) {
        var Water = SW.getBlockState(Pos).isOf(Blocks.WATER);
        return SW.setBlockState(Pos, St.with(Properties.WATERLOGGED, Water), I);
    }
}
