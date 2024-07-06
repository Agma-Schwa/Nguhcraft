package org.nguh.nguhcraft.mixin.server.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.SayCommand;
import net.minecraft.server.command.ServerCommandSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(SayCommand.class)
public abstract class SayCommandMixin {
    /**
     * @author Sirraide
     * @reason Command is replaced by a custom implementation.
     */
    @Overwrite
    public static void register(CommandDispatcher<ServerCommandSource> S) { }
}
