package org.nguh.nguhcraft.mixin.server.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.KickCommand;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.dedicated.command.BanCommand;
import net.minecraft.server.dedicated.command.BanIpCommand;
import net.minecraft.server.dedicated.command.PardonCommand;
import net.minecraft.server.dedicated.command.PardonIpCommand;
import org.nguh.nguhcraft.server.ServerUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Predicate;

/** Set moderator permissions for a bunch of commands.*/
@Mixin({BanCommand.class, BanIpCommand.class, KickCommand.class, PardonCommand.class, PardonIpCommand.class})
public abstract class ModerationCommandsMixin {
    @Redirect(
        method = "register",
        remap = false,
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/brigadier/builder/LiteralArgumentBuilder;requires(Ljava/util/function/Predicate;)Lcom/mojang/brigadier/builder/ArgumentBuilder;"
        )
    )
    private static ArgumentBuilder inject$register(LiteralArgumentBuilder<ServerCommandSource> I, Predicate Unused) {
        Predicate<ServerCommandSource> Pred = ServerUtils::IsModerator;
        return I.requires(Pred);
    }
}
