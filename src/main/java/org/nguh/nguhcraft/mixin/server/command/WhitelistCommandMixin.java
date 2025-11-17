package org.nguh.nguhcraft.mixin.server.command;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.WhitelistCommand;
import net.minecraft.network.chat.Component;
import org.nguh.nguhcraft.server.Chat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Supplier;

@Mixin(WhitelistCommand.class)
public abstract class WhitelistCommandMixin {
    /** Forward 'on' message to discord. */
    @Redirect(
        method = "enableWhitelist",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/commands/CommandSourceStack;sendSuccess(Ljava/util/function/Supplier;Z)V"
        )
    )
    private static void inject$executeOn$sendFeedback(
            CommandSourceStack S,
            Supplier<Component> Feedback,
            boolean Broadcast
    ) {
        Chat.SendServerMessage(S.getServer(), Feedback.get().getString());
    }

    /** Forward 'off' message to discord. */
    @Redirect(
        method = "disableWhitelist",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/commands/CommandSourceStack;sendSuccess(Ljava/util/function/Supplier;Z)V"
        )
    )
    private static void inject$executeOff$sendFeedback(
            CommandSourceStack S,
            Supplier<Component> Feedback,
            boolean Broadcast
    ) {
        Chat.SendServerMessage(S.getServer(), Feedback.get().getString());
    }
}
