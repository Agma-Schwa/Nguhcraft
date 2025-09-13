package org.nguh.nguhcraft.mixin.common;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.StonecutterBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.nguh.nguhcraft.NguhDamageTypes;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(StonecutterBlock.class)
public abstract class StonecutterBlockMixin extends Block {
    public StonecutterBlockMixin(Settings settings) {
        super(settings);
    }

    @Override
    public void onSteppedOn(World W, BlockPos Pos, BlockState St, Entity E) {
        if (
            W instanceof ServerWorld SW &&
            E instanceof PlayerEntity &&
            !E.bypassesSteppingEffects()
        ) {
            var DS = NguhDamageTypes.Stonecutter(W);
            if (!ProtectionManager.IsProtectedEntity(E, DS))
                E.damage(SW, DS, 1.f);
        }

        super.onSteppedOn(W, Pos, St, E);
    }
}
