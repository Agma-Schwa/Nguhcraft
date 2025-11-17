package org.nguh.nguhcraft.mixin.server.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.commands.MsgCommand;
import net.minecraft.commands.CommandSourceStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(MsgCommand.class)
public abstract class MsgCommandMixin {
    /**
     * This command is quite useful, but patch it to disable signing. We
     * add our own implementation of this where we register the rest of
     * our commands.
     *
     * @author Sirraide
     * @reason This command uses chat signing.
     */
    @Overwrite
    public static void register(CommandDispatcher<CommandSourceStack> D) { }
}
