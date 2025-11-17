package org.nguh.nguhcraft.mixin.server.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.TeamMsgCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(TeamMsgCommand.class)
public abstract class TeamMsgCommandMixin {
    /**
     * @author Sirraide
     * @reason This command is pointless and uses chat signing.
     */
    @Overwrite
    public static void register(CommandDispatcher<CommandSourceStack> S) { }
}
