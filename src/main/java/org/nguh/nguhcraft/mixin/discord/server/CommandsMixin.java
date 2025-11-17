package org.nguh.nguhcraft.mixin.discord.server;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.nguh.nguhcraft.server.ServerUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Commands.class)
public abstract class CommandsMixin {
    @Shadow @Final private static ClientboundCommandsPacket.NodeInspector<CommandSourceStack> COMMAND_NODE_INSPECTOR;
    @Unique static private final LiteralCommandNode<CommandSourceStack> UNLINKED_DISCORD_COMMAND =
        LiteralArgumentBuilder.<CommandSourceStack>literal("discord")
        .then(LiteralArgumentBuilder.literal("link"))
        .then(RequiredArgumentBuilder.argument("id", LongArgumentType.longArg()))
        .executes(context -> 0) // Dummy. Never executed.
        .build();

    /**
    * Hijack the command tree sending code to only send '/discord link'
    * if the player is not linked and not an operator.
    */
    @Inject(
        method = "sendCommands(Lnet/minecraft/server/level/ServerPlayer;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void inject$sendCommandTree(ServerPlayer SP, CallbackInfo CI) {
        if (!ServerUtils.IsLinkedOrOperator(SP)) {
            var Root = new RootCommandNode<CommandSourceStack>();
            Root.addChild(UNLINKED_DISCORD_COMMAND);
            SP.connection.send(new ClientboundCommandsPacket(Root, COMMAND_NODE_INSPECTOR));
            CI.cancel();
        }
    }
}
