package org.nguh.nguhcraft.mixin.discord.server;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import net.minecraft.command.CommandSource;
import net.minecraft.network.packet.s2c.play.CommandTreeS2CPacket;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.nguh.nguhcraft.server.ServerUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CommandManager.class)
public abstract class CommandManagerMixin {
    @Shadow @Final private static CommandTreeS2CPacket.CommandNodeInspector<ServerCommandSource> INSPECTOR;
    @Unique static private final LiteralCommandNode<ServerCommandSource> UNLINKED_DISCORD_COMMAND =
        LiteralArgumentBuilder.<ServerCommandSource>literal("discord")
        .then(LiteralArgumentBuilder.literal("link"))
        .then(RequiredArgumentBuilder.argument("id", LongArgumentType.longArg()))
        .executes(context -> 0) // Dummy. Never executed.
        .build();

    /**
    * Hijack the command tree sending code to only send '/discord link'
    * if the player is not linked and not an operator.
    */
    @Inject(
        method = "sendCommandTree(Lnet/minecraft/server/network/ServerPlayerEntity;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void inject$sendCommandTree(ServerPlayerEntity SP, CallbackInfo CI) {
        if (!ServerUtils.IsLinkedOrOperator(SP)) {
            var Root = new RootCommandNode<ServerCommandSource>();
            Root.addChild(UNLINKED_DISCORD_COMMAND);
            SP.networkHandler.sendPacket(new CommandTreeS2CPacket(Root, INSPECTOR));
            CI.cancel();
        }
    }
}
