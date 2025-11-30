package org.nguh.nguhcraft.mixin.entity;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.Fox.FoxEatBerriesGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CaveVines;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.nguh.nguhcraft.block.GrapeCropBlock;
import org.nguh.nguhcraft.block.NguhBlocks;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoxEatBerriesGoal.class)
public class FoxMixin {
    @Shadow @Final
    Fox field_17975;

    /**
     * @author Xhesas
     * @reason Easier to just overwrite. Makes Foxes want to target grape crops, since they like to eat them.
     */
    @Overwrite
    public boolean isValidTarget(LevelReader levelReader, BlockPos blockPos) {
        BlockState blockState = levelReader.getBlockState(blockPos);
        return blockState.is(Blocks.SWEET_BERRY_BUSH) && blockState.getValue(SweetBerryBushBlock.AGE) >= 2 ||
                CaveVines.hasGlowBerries(blockState) ||
                (blockState.is(NguhBlocks.INSTANCE.getGRAPE_CROP()) && blockState.getValue(GrapeCropBlock.Companion.getAGE()) == GrapeCropBlock.MAX_AGE);
    }

    // makes the foxes actually get the grapes
    @Inject(method = "onReachedTarget", at = @At("TAIL"))
    private void inject$onReachedTarget(CallbackInfo ci, @Local BlockState St) {
        if (St.is(NguhBlocks.INSTANCE.getGRAPE_CROP())) {
            PickGrapes(St);
        }
    }

    @Unique
    private void PickGrapes(BlockState St) {
        BlockPos Pos = field_17975.blockPosition();
        Level L = field_17975.level();
        if (!L.getBlockState(Pos).is(NguhBlocks.INSTANCE.getGRAPE_CROP()) && L.getBlockState(Pos.above()).is(NguhBlocks.INSTANCE.getGRAPE_CROP())) { Pos = Pos.above(); }
        else { return; }
        GrapeCropBlock.Companion.use(St, field_17975.level(), Pos, field_17975);
    }
}
