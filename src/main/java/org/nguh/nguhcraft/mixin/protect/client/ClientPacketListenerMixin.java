package org.nguh.nguhcraft.mixin.protect.client;

import net.minecraft.client.multiplayer.ClientPacketListener;
import org.jetbrains.annotations.NotNull;
import org.nguh.nguhcraft.client.ClientProtectionManager;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.nguh.nguhcraft.protect.ProtectionManagerAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin implements ProtectionManagerAccess {
    @Unique private ProtectionManager Manager = ClientProtectionManager.EMPTY;
    @Override public @NotNull ProtectionManager Nguhcraft$GetProtectionManager() { return Manager; }
    @Override public void Nguhcraft$SetProtectionManager(@NotNull ProtectionManager Mgr) { Manager = Mgr; }
}
