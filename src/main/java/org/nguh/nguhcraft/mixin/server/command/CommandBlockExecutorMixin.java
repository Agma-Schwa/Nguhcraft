package org.nguh.nguhcraft.mixin.server.command;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BaseCommandBlock;
import org.nguh.nguhcraft.server.Chat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BaseCommandBlock.class)
public abstract class CommandBlockExecutorMixin {
    @Shadow public abstract String getCommand();

    @WrapOperation(
        method = "performCommand",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/BaseCommandBlock;createCommandSourceStack(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/commands/CommandSource;)Lnet/minecraft/commands/CommandSourceStack;",
            ordinal = 0
        )
    )
    private CommandSourceStack inject$onExecute(
        BaseCommandBlock Self,
        ServerLevel SL,
        CommandSource Source,
        Operation<CommandSourceStack> Op
    ) {
        var Stack = Op.call(Self, SL, Source);
        Chat.LogCommandBlock(getCommand(), SL, BlockPos.containing(Stack.getPosition()));
        return Stack;
    }
}
