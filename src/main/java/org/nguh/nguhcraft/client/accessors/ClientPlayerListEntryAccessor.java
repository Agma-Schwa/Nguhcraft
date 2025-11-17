package org.nguh.nguhcraft.client.accessors;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public interface ClientPlayerListEntryAccessor {
    void setNameAboveHead(Component name);
    Component getNameAboveHead();
    void setLinked(boolean linked);
    boolean isLinked();
    void setBypassesRegionProtection(boolean bypassesProtection);
    boolean getBypassesRegionProtection();
}
