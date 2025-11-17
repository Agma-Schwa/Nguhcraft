package org.nguh.nguhcraft.mixin.server.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.TriggerCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(TriggerCommand.class)
public abstract class TriggerCommandMixin {
    /**
    * @author Sirraide
    * @reason It’s unnecessary, and removing it cleans up the command list a bit.
    */
    @Overwrite
    public static void register(CommandDispatcher<CommandSourceStack> D) {}
}
