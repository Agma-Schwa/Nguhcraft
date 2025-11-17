package org.nguh.nguhcraft.mixin.server.command;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraft.world.level.Level;
import org.nguh.nguhcraft.server.Chat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BaseCommandBlock.class)
public abstract class CommandBlockExecutorMixin {
    @Shadow public abstract String getCommand();
    @Shadow public abstract Vec3 getPosition();

    @Inject(
        method = "performCommand",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/BaseCommandBlock;createCommandSourceStack()Lnet/minecraft/commands/CommandSourceStack;",
            ordinal = 0
        )
    )
    private void inject$onExecute(Level W, CallbackInfoReturnable<Boolean> CIR) {
        Chat.LogCommandBlock(getCommand(), (ServerLevel)W, BlockPos.containing(getPosition()));
    }
}
