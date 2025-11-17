package org.nguh.nguhcraft.mixin.protect;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.ServerExplosion;
import org.nguh.nguhcraft.Constants;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;

@Mixin(ServerExplosion.class)
public abstract class ServerExplosionMixin {
    /** Disable exploding protected blocks. */
    @Redirect(
        method = "calculateExplodedPositions",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/ExplosionDamageCalculator;getBlockExplosionResistance(Lnet/minecraft/world/level/Explosion;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/FluidState;)Ljava/util/Optional;",
            ordinal = 0
        )
    )
    private Optional<Float> redirect$collectBlocksAndDamageEntities(
        ExplosionDamageCalculator EB,
        Explosion E,
        BlockGetter View,
        BlockPos Pos,
        BlockState St,
        FluidState FSt
    ) {
        // Setting the blast resistance to infinity will prevent the block from being destroyed.
        if (View instanceof Level W && ProtectionManager.IsProtectedBlock(W, Pos))
            return Optional.of(Constants.BIG_VALUE_FLOAT);
        return EB.getBlockExplosionResistance(E, View, Pos, St, FSt);
    }
}
