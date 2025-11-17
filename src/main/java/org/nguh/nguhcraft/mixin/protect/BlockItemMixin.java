package org.nguh.nguhcraft.mixin.protect;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.InteractionResult;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class BlockItemMixin {
    /** Prevent placing a block inside a region. */
    @Inject(
        method = "place(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/InteractionResult;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void inject$place(BlockPlaceContext Context, CallbackInfoReturnable<InteractionResult> CIR) {
        var Player = Context.getPlayer();
        if (Player == null) return;
        if (!ProtectionManager.AllowBlockModify(Context.getPlayer(), Context.getLevel(), Context.getClickedPos()))
            CIR.setReturnValue(InteractionResult.FAIL);
    }
}
