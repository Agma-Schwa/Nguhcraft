package org.nguh.nguhcraft.mixin.protect.client;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ClientLevel;
import org.jetbrains.annotations.NotNull;
import org.nguh.nguhcraft.protect.ProtectionManagerAccess;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin implements ProtectionManagerAccess {
    @Shadow @Final private ClientPacketListener connection;
    @Override public @NotNull ProtectionManager Nguhcraft$GetProtectionManager() {
        return ((ProtectionManagerAccess)(Object) connection).Nguhcraft$GetProtectionManager();
    }

    @Override
    public void Nguhcraft$SetProtectionManager(@NotNull ProtectionManager Mgr) {
        ((ProtectionManagerAccess)(Object) connection).Nguhcraft$SetProtectionManager(Mgr);
    }
}
