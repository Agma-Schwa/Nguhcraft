package org.nguh.nguhcraft.mixin.client.chat;

import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
    /** A mere 100 messages of scrollback is rather sad. */
    @Unique private static final int NGUH_MAX_MESSAGES = 10000;
    @ModifyConstant(
        method = {
            "<init>",
            "addMessageToDisplayQueue",
            "addMessageToQueue(Lnet/minecraft/client/GuiMessage;)V",
            "addRecentChat",
        },
        constant = @Constant(intValue = 100)
    )
    public int inject$addVisibleMessage(int value) {
        return NGUH_MAX_MESSAGES;
    }
}
