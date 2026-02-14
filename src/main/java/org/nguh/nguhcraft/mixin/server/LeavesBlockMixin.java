package org.nguh.nguhcraft.mixin.server;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.nguh.nguhcraft.block.BuddingLeavesBlock;
import org.nguh.nguhcraft.block.NguhBlocks;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

import static net.minecraft.world.level.block.Block.UPDATE_CLIENTS;

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

    /**
     * We can't just use the lookup table in `NguhBlocks` because
     * this is called during bootstrap (via isRandomlyTicking(), and
     * initialising `NguhBlocks` early leads to arcane data corruption
     * issues, so we need to duplicate this information here.
     **/
    @Unique static private boolean IsBuddingLeavesBlock(BlockState St) {
        return St.is(Blocks.OAK_LEAVES) ||
               St.is(Blocks.DARK_OAK_LEAVES) ||
               St.is(Blocks.CHERRY_LEAVES);
    }

    @Unique
    private Optional<Block> getBuddingLeavesBlock(BlockState St) {
        return Optional.ofNullable(NguhBlocks.LEAVES_TO_BUDDING_LEAVES.get(St.getBlock()));
    }

    @Inject(method = "isRandomlyTicking", at = @At("HEAD"), cancellable = true)
    private void inject$isRandomlyTicking(
        BlockState St,
        CallbackInfoReturnable<Boolean> CIR
    ) {
        if (IsBuddingLeavesBlock(St) & !St.getValue(PERSISTENT))
            CIR.setReturnValue(true);
    }

    @Inject(method = "randomTick", at = @At("TAIL"))
    private void inject$randomTick(
        BlockState St,
        ServerLevel W,
        BlockPos Pos,
        RandomSource R,
        CallbackInfo CI
    ) {
        if (ProtectionManager.IsProtectedBlock(W, Pos)) return;

        // If this block doesn't have a corresponding budding block, give up.
        Optional<Block> BuddingBlock = getBuddingLeavesBlock(St);
        if (BuddingBlock.isEmpty()) return;

        // We don't want every leaves block to turn into a budding block, so
        // check how many of our neighbours are already budding blocks.
        int Neighbours = CountBuddingNeighbors(W, Pos);
        if (Neighbours >= R.nextIntBetweenInclusive(1, 3)) return;

        // Ok, turn this into a budding block but preserve the rest of the block state.
        var NewState = BuddingLeavesBlock.CopySharedProperties(
            BuddingBlock.get().defaultBlockState(),
            St
        );

        W.setBlock(Pos, NewState, UPDATE_CLIENTS);
    }

    @Unique private int CountBuddingNeighbors(ServerLevel SW, BlockPos Pos) {
        var Count = 0;
        for (var Neighbour : BlockPos.withinManhattan(Pos, 1, 1, 1)) {
            if (Pos.equals(Neighbour)) continue;
            if (SW.getBlockState(Neighbour).getBlock() instanceof BuddingLeavesBlock) Count++;
        }
        return Count;
    }
}
