package org.nguh.nguhcraft.mixin.server.dedicated;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import org.nguh.nguhcraft.server.Chat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DedicatedServer.class)
public abstract class DedicatedServerMixin {
    /**
    * Disable enforcing secure profiles.
    * @author Sirraide
    * @reason We disable chat signing anyway.
    */
    @Overwrite
    public boolean enforceSecureProfile() { return false; }

    /** Log RCon commands. */
    @Inject(method = "runCommand", at = @At("HEAD"))
    private void executeRconCommand(String Command, CallbackInfoReturnable<Boolean> CIR) {
        Chat.LogRConCommand((MinecraftServer)(Object)this, Command);
    }
}
