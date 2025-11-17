package org.nguh.nguhcraft.mixin.client.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PauseScreen.class)
public abstract class PauseMixin {
    @Unique static private final Tooltip LAN_DISABLED = Tooltip.create(Component.nullToEmpty("LAN multiplayer is not supported by Nguhcraft!"));
    @Unique static private final Tooltip REPORT_SCREEN_DISABLED = Tooltip.create(Component.nullToEmpty("Social interactions screen is not supported by Nguhcraft!"));

    @Unique private static LayoutElement MakeDisabledWidget(
            GridLayout.RowHelper Instance,
            LayoutElement W,
            Operation<LayoutElement> Orig,
            Tooltip Message
    ) {
        Orig.call(Instance, W);
        var BW = ((Button)W);
        BW.active = false;
        BW.setTooltip(Message);
        return W;
    }

    /** We don’t support LAN multiplayer, so disable the button. */
    @WrapOperation(
        method = "createPauseMenu",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/layouts/GridLayout$RowHelper;addChild(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;"
        )
    )
    private LayoutElement inject$initWidgets$0(
            GridLayout.RowHelper Instance,
            LayoutElement W,
            Operation<LayoutElement> Orig
    ) {
        if (W instanceof Button BW && BW.getMessage().getContents() instanceof TranslatableContents TTC) {
            if (TTC.getKey().equals("menu.shareToLan")) return MakeDisabledWidget(Instance, W, Orig, LAN_DISABLED);
            if (TTC.getKey().equals("menu.playerReporting")) return MakeDisabledWidget(Instance, W, Orig, REPORT_SCREEN_DISABLED);
        }
        return Orig.call(Instance, W);
    }
}
