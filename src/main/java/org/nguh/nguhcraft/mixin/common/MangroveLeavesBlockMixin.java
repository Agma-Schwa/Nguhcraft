package org.nguh.nguhcraft.mixin.common;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MangroveLeavesBlock;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MangroveLeavesBlock.class)
public abstract class MangroveLeavesBlockMixin {
    /** Allow bonemealing mangrove leaves underwater. */
    @Inject(method = "isValidBonemealTarget", at = @At("HEAD"), cancellable = true)
    private void inject$isFertilizable(LevelReader W, BlockPos Pos, BlockState St, CallbackInfoReturnable<Boolean> CIR) {
        if (W.getBlockState(Pos.below()).is(Blocks.WATER))
            CIR.setReturnValue(true);
    }

    /** And make sure the resulting propagules are waterlogged. */
    @Redirect(
        method = "performBonemeal",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"
        )
    )
    private boolean inject$grow(ServerLevel SW, BlockPos Pos, BlockState St, int I) {
        var Water = SW.getBlockState(Pos).is(Blocks.WATER);
        return SW.setBlock(Pos, St.setValue(BlockStateProperties.WATERLOGGED, Water), I);
    }
}
