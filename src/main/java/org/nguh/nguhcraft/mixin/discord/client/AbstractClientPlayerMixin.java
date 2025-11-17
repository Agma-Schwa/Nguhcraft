package org.nguh.nguhcraft.mixin.discord.client;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.nguh.nguhcraft.client.accessors.AbstractClientPlayerEntityAccessor;
import org.nguh.nguhcraft.client.accessors.ClientPlayerListEntryAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin extends Player implements AbstractClientPlayerEntityAccessor {
    @Shadow private @Nullable PlayerInfo playerInfo;

    public AbstractClientPlayerMixin(Level world, GameProfile profile) {
        super(world, profile);
    }

    /**
    * Note: If we’re connected to the integrated server, then we never receive
    * a player list entry update, hence this will always fall through to the
    * default value.
    */
    @Override
    public Component getDisplayName() {
        // Overridden for linked players.
        if (playerInfo instanceof ClientPlayerListEntryAccessor PLE) {
            Component name = PLE.getNameAboveHead();
            if (name != null) return name;
        }

        // Default behaviour.
        return super.getDisplayName();
    }

    @Override
    public boolean isLinked() {
        if (Minecraft.getInstance().getSingleplayerServer() != null) return true;
        return playerInfo instanceof ClientPlayerListEntryAccessor PLE && PLE.isLinked();
    }
}
