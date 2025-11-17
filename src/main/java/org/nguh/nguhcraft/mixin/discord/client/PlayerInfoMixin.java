package org.nguh.nguhcraft.mixin.discord.client;

import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.nguh.nguhcraft.client.accessors.ClientPlayerListEntryAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(PlayerInfo.class)
public abstract class PlayerInfoMixin implements ClientPlayerListEntryAccessor {
    @Unique private Component NameAboveHead = null;
    @Unique private boolean Linked = false;

    @Override public void setNameAboveHead(Component Name) { NameAboveHead = Name; }
    @Override public Component getNameAboveHead() { return NameAboveHead; }
    @Override public void setLinked(boolean Linked) { this.Linked = Linked; }
    @Override public boolean isLinked() { return Linked; }
}
