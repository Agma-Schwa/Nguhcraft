package org.nguh.nguhcraft.mixin.discord.client;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.nguh.nguhcraft.client.accessors.AbstractClientPlayerEntityAccessor;
import org.nguh.nguhcraft.client.accessors.ClientPlayerListEntryAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerMixin extends PlayerEntity implements AbstractClientPlayerEntityAccessor {
    @Shadow private @Nullable PlayerListEntry playerListEntry;

    public AbstractClientPlayerMixin(World world, GameProfile profile) {
        super(world, profile);
    }

    /**
    * Note: If we’re connected to the integrated server, then we never receive
    * a player list entry update, hence this will always fall through to the
    * default value.
    */
    @Override
    public Text getDisplayName() {
        // Overridden for linked players.
        if (playerListEntry instanceof ClientPlayerListEntryAccessor PLE) {
            Text name = PLE.getNameAboveHead();
            if (name != null) return name;
        }

        // Default behaviour.
        return super.getDisplayName();
    }

    @Override
    public boolean isLinked() {
        if (MinecraftClient.getInstance().getServer() != null) return true;
        return playerListEntry instanceof ClientPlayerListEntryAccessor PLE && PLE.isLinked();
    }
}
