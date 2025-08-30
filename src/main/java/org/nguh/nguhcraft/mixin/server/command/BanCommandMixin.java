package org.nguh.nguhcraft.mixin.server.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.dedicated.command.BanCommand;
import org.nguh.nguhcraft.server.ServerUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Predicate;

/** Set moderator permissions for a bunch of commands.*/
@Mixin(BanCommand.class)
public abstract class BanCommandMixin {
    @Redirect(
        method = "register",
        at = @At(
            value = "INVOKE",
            remap = false,
            target = "Lcom/mojang/brigadier/builder/LiteralArgumentBuilder;requires(Ljava/util/function/Predicate;)Lcom/mojang/brigadier/builder/ArgumentBuilder;"
        )
    )
    private static ArgumentBuilder inject$register(LiteralArgumentBuilder<ServerCommandSource> I, Predicate Unused) {
        Predicate<ServerCommandSource> Pred = ServerUtils::IsModerator;
        return I.requires(Pred);
    }
}
