package org.nguh.nguhcraft.mixin.protect;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = {World.class, ServerWorld.class})
public abstract class World_ServerWorldMixin {
    /** Blanket fix for a bunch of random stuff (e.g. picking up water w/ a bucket etc.). */
    @Inject(method = "canEntityModifyAt", at = @At("HEAD"), cancellable = true)
    private void inject$canEntityModifyAt(
        Entity E,
        BlockPos Pos,
        CallbackInfoReturnable<Boolean> CIR
    ) {
        // Handle players separately.
        if (E instanceof PlayerEntity PE) {
            var Res = ProtectionManager.HandleBlockInteract(PE, (World) (Object) this, Pos, PE.getMainHandStack());
            if (Res != ActionResult.SUCCESS)
                CIR.setReturnValue(false);
        }

        // Blanket modification ban for protected blocks.
        else if (ProtectionManager.IsProtectedBlock((World)(Object)this, Pos))
            CIR.setReturnValue(false);
    }
}
