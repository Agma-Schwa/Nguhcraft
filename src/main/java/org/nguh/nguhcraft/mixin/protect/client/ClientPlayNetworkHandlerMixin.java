package org.nguh.nguhcraft.mixin.protect.client;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.nguh.nguhcraft.client.ClientProtectionManager;
import org.nguh.nguhcraft.protect.ProtectionManagerAccessor;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin implements ProtectionManagerAccessor {
    @Unique private ProtectionManager Manager = ClientProtectionManager.EMPTY;

    @Override public ProtectionManager Nguhcraft$GetProtectionManager() { return Manager; }
    @Override public void Nguhcraft$SetProtectionManager(ProtectionManager manager) { Manager = manager; }
}
