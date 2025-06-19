package org.nguh.nguhcraft.mixin.server.command;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.CommandBlockExecutor;
import net.minecraft.world.World;
import org.nguh.nguhcraft.server.Chat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CommandBlockExecutor.class)
public abstract class CommandBlockExecutorMixin {
    @Shadow public abstract String getCommand();
    @Shadow public abstract Vec3d getPos();

    @Inject(
        method = "execute",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/CommandBlockExecutor;getSource()Lnet/minecraft/server/command/ServerCommandSource;",
            ordinal = 0
        )
    )
    private void inject$onExecute(World W, CallbackInfoReturnable<Boolean> CIR) {
        Chat.LogCommandBlock(getCommand(), (ServerWorld)W, BlockPos.ofFloored(getPos()));
    }
}
