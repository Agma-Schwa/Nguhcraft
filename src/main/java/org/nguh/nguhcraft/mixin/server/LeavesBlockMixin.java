package org.nguh.nguhcraft.mixin.server;

import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LeavesBlock.class)
public abstract class LeavesBlockMixin {
    @Shadow @Final public static IntProperty DISTANCE;
    @Shadow @Final public static int MAX_DISTANCE;
    @Shadow @Final public static BooleanProperty PERSISTENT;

    @Shadow protected abstract void randomTick(
        BlockState St,
        ServerWorld W,
        BlockPos Pos,
        Random R
    );

    /** Fast leaf decay. */
    @Inject(method = "scheduledTick", at = @At("TAIL"))
    private void inject$scheduledTick(
        BlockState St,
        ServerWorld W,
        BlockPos Pos,
        Random R,
        CallbackInfo CI
    ) {
        if (St.get(PERSISTENT)) return;

        // If the distance before this function was called was already the maximum
        // distance, then this is the tick we scheduled below; remove it.
        if (St.get(DISTANCE) == MAX_DISTANCE) randomTick(St, W, Pos, R);

        // Otherwise, schedule another tick to remove the block if this tick just
        // set the distance to 7. This is called from vanilla code during neighbour
        // updates.
        else if (W.getBlockState(Pos).get(DISTANCE) == MAX_DISTANCE) W.scheduleBlockTick(
            Pos,
            (LeavesBlock) (Object) this,
            R.nextBetween(1, 15)
        );
    }
}
