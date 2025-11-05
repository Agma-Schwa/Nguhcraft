package org.nguh.nguhcraft.mixin.server.dedicated;

import net.minecraft.server.rcon.RconCommandOutput;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RconCommandOutput.class)
public abstract class RConCommandOutputMixin {
    @Shadow @Final private StringBuffer buffer;

    // Ensure every message part is \n terminated.
    @Inject(method = "sendMessage", at = @At("TAIL"))
    private void inject$sendMessage(Text Msg, CallbackInfo CI) {
        if (!buffer.isEmpty() && buffer.charAt(buffer.length() - 1) != '\n')
            buffer.append('\n');
    }
}
