package org.nguh.nguhcraft.mixin.server.command;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(CommandSourceStack.class)
public abstract class ServerCommandSourceMixin {
    /**
    * Do not broadcast command feedback to ops.
    *
    * @author Sirraide
    * @reason Annoying and pointless. Also horrible if we ever do adventure streams again.
    */
    @Overwrite
    private void broadcastToAdmins(Component T) {}
}
