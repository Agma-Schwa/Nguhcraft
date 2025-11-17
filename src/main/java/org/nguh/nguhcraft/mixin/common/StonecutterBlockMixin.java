package org.nguh.nguhcraft.mixin.common;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.StonecutterBlock;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.nguh.nguhcraft.NguhDamageTypes;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(StonecutterBlock.class)
public abstract class StonecutterBlockMixin extends Block {
    public StonecutterBlockMixin(Properties settings) {
        super(settings);
    }

    @Override
    public void stepOn(Level W, BlockPos Pos, BlockState St, Entity E) {
        if (
            W instanceof ServerLevel SW &&
            E instanceof Player &&
            !E.isSteppingCarefully()
        ) {
            var DS = NguhDamageTypes.Stonecutter(W);
            if (!ProtectionManager.IsProtectedEntity(E, DS))
                E.hurtServer(SW, DS, 1.f);
        }

        super.stepOn(W, Pos, St, E);
    }
}
