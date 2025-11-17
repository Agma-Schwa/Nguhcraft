package org.nguh.nguhcraft.mixin.server;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.DetectorRailBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(DetectorRailBlock.class)
public interface DetectorRailBlockAccessor {
    @Invoker("checkPressed")
    void Nguhcraft$UpdatePoweredStatus(Level world, BlockPos pos, BlockState state);
}
