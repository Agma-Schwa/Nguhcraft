package org.nguh.nguhcraft.mixin.protect;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(WitherSkull.class)
public abstract class WitherSkullMixin {
    /**
    * @author Sirraide
    * @reason Disabled entirely to prevent them from destroying protected blocks.
    */
    @Overwrite
    public float getBlockExplosionResistance(
        Explosion explosion,
        BlockGetter world,
        BlockPos pos,
        BlockState blockState,
        FluidState fluidState,
        float max
    ) { return max; }
}
