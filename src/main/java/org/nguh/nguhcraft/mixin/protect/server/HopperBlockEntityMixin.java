package org.nguh.nguhcraft.mixin.protect.server;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.BooleanSupplier;

@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin {
    /** As below. Used by hopper minecarts. */
    @Inject(
        method = "suckInItems(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/entity/Hopper;)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void inject$extract(Level W, Hopper H, CallbackInfoReturnable<Boolean> CIR) {
        BlockPos Pos = BlockPos.containing(H.getLevelX(), H.getLevelY() + 1.0, H.getLevelZ());
        if (ProtectionManager.IsProtectedBlock(W, Pos))
            CIR.setReturnValue(false);
    }

    /**
    * Prevent hoppers from accessing protected inventories.
    * <p>
    * Thanks to whatever FUCKING MORON designed the part of the fabric
    * API that overrides the hopper code TO STILL PERFORM THE TRANSFER
    * inside of the functions THAT RETRIEVE THE INVENTORIES to transfer
    * from/to, we need to perform this check early, instead of doing the
    * SENSIBLE thing and simply returning null for the input and output
    * inventories.
    * <p>
    * TODO: Allow locking hoppers and allow locked hoppers to
    *       access protected inventories with the same key.
    */
    @Inject(
        method = "tryMoveItems",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void inject$insertAndExtract(
        Level W,
        BlockPos Pos,
        BlockState St,
        HopperBlockEntity BE,
        BooleanSupplier BS,
        CallbackInfoReturnable<Boolean> CIR
    ) {
        // I could not care less about being granular here and allowing inserting
        // and not extracting after having to deal with fabric’s asinine API design;
        // if a hopper is touching a protected block, then fuck you, nothing is going
        // to move anywhere.
        Direction Facing = ((HopperBlockEntityAccessor) BE).getFacing();
        BlockPos ToPos = Pos.relative(Facing);
        BlockPos FromPos = Pos.above();
        if (ProtectionManager.IsProtectedBlock(W, FromPos) || ProtectionManager.IsProtectedBlock(W, ToPos))
            CIR.setReturnValue(false);
    }
}
