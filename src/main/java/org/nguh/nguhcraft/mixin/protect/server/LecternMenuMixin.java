package org.nguh.nguhcraft.mixin.protect.server;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.LecternMenu;
import net.minecraft.core.BlockPos;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.nguh.nguhcraft.server.accessors.LecternScreenHandlerAccessor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LecternMenu.class)
public abstract class LecternMenuMixin implements LecternScreenHandlerAccessor {
    @Shadow @Final public static int BUTTON_TAKE_BOOK;
    @Unique private BlockPos LecternPos;

    @Override public void Nguhcraft$SetLecternPos(BlockPos Pos) {
        LecternPos = Pos;
    }

    @Inject(method = "clickMenuButton", at = @At("HEAD"), cancellable = true)
    private void inject$onButtonClick(Player PE, int Button, CallbackInfoReturnable<Boolean> CIR) {
        if (Button == BUTTON_TAKE_BOOK && !ProtectionManager.AllowBlockModify(PE, PE.level(), LecternPos))
            CIR.setReturnValue(false);
    }
}
