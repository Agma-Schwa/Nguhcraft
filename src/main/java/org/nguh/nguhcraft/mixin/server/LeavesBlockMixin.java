package org.nguh.nguhcraft.mixin.server;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LeavesBlock.class)
public abstract class LeavesBlockMixin {
    @Shadow @Final public static IntegerProperty DISTANCE;
    @Shadow @Final public static int DECAY_DISTANCE;
    @Shadow @Final public static BooleanProperty PERSISTENT;

    @Shadow protected abstract void randomTick(
        BlockState St,
        ServerLevel W,
        BlockPos Pos,
        RandomSource R
    );

    /** Fast leaf decay. */
    @Inject(method = "tick", at = @At("TAIL"))
    private void inject$scheduledTick(
        BlockState St,
        ServerLevel W,
        BlockPos Pos,
        RandomSource R,
        CallbackInfo CI
    ) {
        if (St.getValue(PERSISTENT)) return;

        // If the distance before this function was called was already the maximum
        // distance, then this is the tick we scheduled below; remove it.
        if (St.getValue(DISTANCE) == DECAY_DISTANCE) randomTick(St, W, Pos, R);

        // Otherwise, schedule another tick to remove the block if this tick just
        // set the distance to 7. This is called from vanilla code during neighbour
        // updates.
        else if (W.getBlockState(Pos).getValue(DISTANCE) == DECAY_DISTANCE) W.scheduleTick(
            Pos,
            (LeavesBlock) (Object) this,
            R.nextIntBetweenInclusive(1, 15)
        );
    }
}
