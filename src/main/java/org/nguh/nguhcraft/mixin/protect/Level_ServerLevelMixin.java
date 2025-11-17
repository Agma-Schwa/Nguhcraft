package org.nguh.nguhcraft.mixin.protect;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = {Level.class, ServerLevel.class})
public abstract class Level_ServerLevelMixin {
    /** Blanket fix for a bunch of random stuff (e.g. picking up water w/ a bucket etc.). */
    @Inject(method = "mayInteract", at = @At("HEAD"), cancellable = true)
    private void inject$canEntityModifyAt(
        Entity E,
        BlockPos Pos,
        CallbackInfoReturnable<Boolean> CIR
    ) {
        // Handle players separately.
        if (E instanceof Player PE) {
            var Res = ProtectionManager.HandleBlockInteract(PE, (Level) (Object) this, Pos, PE.getMainHandItem());
            if (Res != InteractionResult.SUCCESS)
                CIR.setReturnValue(false);
        }

        // Blanket modification ban for protected blocks.
        else if (ProtectionManager.IsProtectedBlock((Level)(Object)this, Pos))
            CIR.setReturnValue(false);
    }
}
