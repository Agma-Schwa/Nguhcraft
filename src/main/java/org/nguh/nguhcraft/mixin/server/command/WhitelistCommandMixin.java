package org.nguh.nguhcraft.mixin.server.command;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.dedicated.command.WhitelistCommand;
import net.minecraft.text.Text;
import org.nguh.nguhcraft.server.Chat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Supplier;

@Mixin(WhitelistCommand.class)
public abstract class WhitelistCommandMixin {
    /** Forward 'on' message to discord. */
    @Redirect(
        method = "executeOn",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/command/ServerCommandSource;sendFeedback(Ljava/util/function/Supplier;Z)V"
        )
    )
    private static void inject$executeOn$sendFeedback(
            ServerCommandSource S,
            Supplier<Text> Feedback,
            boolean Broadcast
    ) {
        Chat.SendServerMessage(S.getServer(), Feedback.get().getString());
    }

    /** Forward 'off' message to discord. */
    @Redirect(
        method = "executeOff",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/command/ServerCommandSource;sendFeedback(Ljava/util/function/Supplier;Z)V"
        )
    )
    private static void inject$executeOff$sendFeedback(
        ServerCommandSource S,
        Supplier<Text> Feedback,
        boolean Broadcast
    ) {
        Chat.SendServerMessage(S.getServer(), Feedback.get().getString());
    }
}
