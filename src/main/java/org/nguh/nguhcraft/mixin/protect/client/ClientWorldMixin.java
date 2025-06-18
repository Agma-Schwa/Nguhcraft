package org.nguh.nguhcraft.mixin.protect.client;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import org.jetbrains.annotations.NotNull;
import org.nguh.nguhcraft.protect.ProtectionManagerAccess;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ClientWorld.class)
public abstract class ClientWorldMixin implements ProtectionManagerAccess {
    @Shadow @Final private ClientPlayNetworkHandler networkHandler;
    @Override public @NotNull ProtectionManager Nguhcraft$GetProtectionManager() {
        return ((ProtectionManagerAccess)(Object)networkHandler).Nguhcraft$GetProtectionManager();
    }

    @Override
    public void Nguhcraft$SetProtectionManager(@NotNull ProtectionManager Mgr) {
        ((ProtectionManagerAccess)(Object)networkHandler).Nguhcraft$SetProtectionManager(Mgr);
    }
}
