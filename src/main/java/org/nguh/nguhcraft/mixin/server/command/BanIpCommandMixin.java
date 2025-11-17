package org.nguh.nguhcraft.mixin.server.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.BanPlayerCommands;
import net.minecraft.server.commands.BanIpCommands;
import org.nguh.nguhcraft.server.ServerUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Predicate;

/** Set moderator permissions for a bunch of commands.*/
@Mixin(BanIpCommands.class)
public abstract class BanIpCommandMixin {
    @Redirect(
        method = "register",
        at = @At(
            value = "INVOKE",
            remap = false,
            target = "Lcom/mojang/brigadier/builder/LiteralArgumentBuilder;requires(Ljava/util/function/Predicate;)Lcom/mojang/brigadier/builder/ArgumentBuilder;"
        )
    )
    private static ArgumentBuilder inject$register(LiteralArgumentBuilder<CommandSourceStack> I, Predicate Unused) {
        Predicate<CommandSourceStack> Pred = ServerUtils::IsModerator;
        return I.requires(Pred);
    }
}
