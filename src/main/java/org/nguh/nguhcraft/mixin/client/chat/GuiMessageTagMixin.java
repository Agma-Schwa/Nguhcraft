package org.nguh.nguhcraft.mixin.client.chat;

import net.minecraft.client.GuiMessageTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(GuiMessageTag.class)
public abstract class GuiMessageTagMixin {
    /**
    * Remove the system message indicator.
    *
    * @author Sirraide
    * @reason The message indicator is useless. All messages are now system messages.
    */
    @Overwrite
    public static GuiMessageTag system() { return null; }

    /**
     * Remove the single player message indicator.
     *
     * @author Sirraide
     * @reason The message indicator is useless. All messages are now system messages.
     */
    @Overwrite
    public static GuiMessageTag systemSinglePlayer() { return null; }

}
