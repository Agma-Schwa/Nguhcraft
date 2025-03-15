package org.nguh.nguhcraft.mixin.client.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.GridWidget;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin {
    @Unique static private final Tooltip LAN_DISABLED = Tooltip.of(Text.of("LAN multiplayer is not supported by Nguhcraft!"));
    @Unique static private final Tooltip REPORT_SCREEN_DISABLED = Tooltip.of(Text.of("Social interactions screen is not supported by Nguhcraft!"));

    @Unique private static Widget MakeDisabledWidget(
        GridWidget.Adder Instance,
        Widget W,
        Operation<Widget> Orig,
        Tooltip Message
    ) {
        Orig.call(Instance, W);
        var BW = ((ButtonWidget)W);
        BW.active = false;
        BW.setTooltip(Message);
        return W;
    }

    /** We don’t support LAN multiplayer, so disable the button. */
    @WrapOperation(
        method = "initWidgets",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/widget/GridWidget$Adder;add(Lnet/minecraft/client/gui/widget/Widget;)Lnet/minecraft/client/gui/widget/Widget;",
            ordinal = 5
        )
    )
    private Widget inject$initWidgets$0(
        GridWidget.Adder Instance,
        Widget W,
        Operation<Widget> Orig
    ) { return MakeDisabledWidget(Instance, W, Orig, LAN_DISABLED); }

    /** Likewise, disable the social interactions screen. */
    @WrapOperation(
        method = "initWidgets",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/widget/GridWidget$Adder;add(Lnet/minecraft/client/gui/widget/Widget;)Lnet/minecraft/client/gui/widget/Widget;",
            ordinal = 6
        )
    )
    private Widget inject$initWidgets$1(
        GridWidget.Adder Instance,
        Widget W,
        Operation<Widget> Orig
    ) { return MakeDisabledWidget(Instance, W, Orig, REPORT_SCREEN_DISABLED); }
}
