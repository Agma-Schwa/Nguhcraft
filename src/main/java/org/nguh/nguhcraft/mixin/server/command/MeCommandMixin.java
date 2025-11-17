package org.nguh.nguhcraft.mixin.server.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.commands.EmoteCommands;
import net.minecraft.commands.CommandSourceStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(EmoteCommands.class)
public abstract class MeCommandMixin {
    /**
    * @author Sirraide
    * @reason This command is pointless and uses chat signing.
    */
    @Overwrite
    public static void register(CommandDispatcher<CommandSourceStack> S) { }
}
