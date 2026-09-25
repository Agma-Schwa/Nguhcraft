package org.nguh.nguhcraft.mixin.client.chat;

import com.mojang.authlib.minecraft.TelemetrySession;
import com.mojang.authlib.services.MinecraftServicesUserApiService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.concurrent.Executor;

@Mixin(MinecraftServicesUserApiService.class)
public abstract class YggdrasilUserApiServiceMixin {
    /**
    * Disable telemetry because why not.
    *
    * @author Sirraide
    * @reason No reason to keep this enabled.
    */
    @Overwrite(remap = false)
    public TelemetrySession newTelemetrySession(Executor executor) {
        return TelemetrySession.DISABLED;
    }
}
