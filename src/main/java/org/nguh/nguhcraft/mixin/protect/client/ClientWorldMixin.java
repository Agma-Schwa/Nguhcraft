package org.nguh.nguhcraft.mixin.protect.client;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import org.nguh.nguhcraft.protect.ProtectionManagerAccessor;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ClientWorld.class)
public abstract class ClientWorldMixin implements ProtectionManagerAccessor {
    @Shadow @Final private ClientPlayNetworkHandler networkHandler;

    @Override public ProtectionManager Nguhcraft$GetProtectionManager() {
        return ((ProtectionManagerAccessor)networkHandler).Nguhcraft$GetProtectionManager();
    }

    @Override public void Nguhcraft$SetProtectionManager(ProtectionManager M) {
        ((ProtectionManagerAccessor)networkHandler).Nguhcraft$SetProtectionManager(M);
    }
}
