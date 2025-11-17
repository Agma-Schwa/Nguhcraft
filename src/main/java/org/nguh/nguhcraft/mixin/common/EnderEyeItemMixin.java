package org.nguh.nguhcraft.mixin.common;

import net.minecraft.world.item.EnderEyeItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.ChatFormatting;
import org.nguh.nguhcraft.SyncedGameRule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderEyeItem.class)
public class EnderEyeItemMixin {
    @Unique static private final Component END_DISABLED_MESSAGE
        = Component.literal("You’re not allowed to place this here.").withStyle(ChatFormatting.RED);

    /** Disallow placing an eye of ender on an end portal frame. */
    @Inject(
        method = "useOn(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void inject$useOnBlock(UseOnContext C, CallbackInfoReturnable<InteractionResult> CI) {
        var W = C.getLevel();
        if (!SyncedGameRule.END_ENABLED.IsSet()) {
            if (W.isClientSide) C.getPlayer().displayClientMessage(END_DISABLED_MESSAGE, true);
            CI.setReturnValue(InteractionResult.FAIL);
        }
    }
}
