package org.nguh.nguhcraft.mixin.discord.server;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.nguh.nguhcraft.server.dedicated.Discord;
import org.nguh.nguhcraft.server.accessors.ServerPlayerAccessor;
import org.nguh.nguhcraft.server.accessors.ServerPlayerDiscordAccessor;
import org.nguh.nguhcraft.server.dedicated.Vanish;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerMixin extends PlayerEntity implements ServerPlayerDiscordAccessor {
    public ServerPlayerMixin(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(world, pos, yaw, gameProfile);
    }

    @Unique static private final Logger LOGGER = LogUtils.getLogger();

    @Unique private long DiscordId = 0;
    @Unique private int DiscordColour = 0;
    @Unique private String DiscordName = "";
    @Unique private String DiscordAvatar = "";
    @Unique private Text NguhcraftDisplayName = null;
    @Unique private boolean Muted = false;

    @Override public long getDiscordId() { return DiscordId; }
    @Override public void setDiscordId(long id) { DiscordId = id; }

    @Override public String getDiscordName() { return DiscordName; }
    @Override public void setDiscordName(String name) { DiscordName = name; }

    @Override public int getDiscordColour() { return DiscordColour; }
    @Override public void setDiscordColour(int colour) { DiscordColour = colour; }

    @Override public Text getNguhcraftDisplayName() { return NguhcraftDisplayName; }
    @Override public void setNguhcraftDisplayName(Text name) { NguhcraftDisplayName = name; }

    @Override public String getDiscordAvatarURL() { return DiscordAvatar; }
    @Override public void setDiscordAvatarURL(String url) { DiscordAvatar = url; }

    @Override public boolean isLinked() { return DiscordId != 0; }
    @Override public boolean isLinkedOrOperator() { return isLinked() || hasPermissionLevel(4); }

    @Override public boolean getMuted() { return Muted; }
    @Override public void setMuted(boolean muted) { Muted = muted; }

    /** Get a player’s display name. Used in death messages etc. */
    @Override
    public Text getDisplayName() {
        if (NguhcraftDisplayName == null) {
            LOGGER.error("Attempted to get display name for player '{}' before it was set!", getNameForScoreboard());
            return Text.literal("<name was null: " + getNameForScoreboard() + ">");
        }

        return NguhcraftDisplayName;
    }

    /** Load custom data from Nbt. */
    @Override
    public void LoadDiscordNguhcraftNbt(@NotNull NbtCompound nbt) {
        if (nbt.contains(ServerPlayerAccessor.TAG_ROOT)) {
            var nguh = nbt.getCompound(ServerPlayerAccessor.TAG_ROOT);
            DiscordId = nguh.getLong(TAG_DISCORD_ID);
            DiscordColour = nguh.getInt(TAG_DISCORD_COLOUR);
            DiscordName = nguh.getString(TAG_DISCORD_NAME);
            DiscordAvatar = nguh.getString(TAG_DISCORD_AVATAR);
            Muted = nguh.getBoolean(TAG_MUTED);
        }
    }

    /**
     * Copy over player data from a to-be-deleted instance.
     * <p>
     * For SOME UNGODLY REASON, Minecraft creates a NEW PLAYER ENTITY when the player
     * dies and basically does the equivalent of reconnecting the player, but on the
     * same network connexion etc.
     * <p>
     * This function copies our custom data over to the new player entity.
     */
    @Inject(
        method = "copyFrom(Lnet/minecraft/server/network/ServerPlayerEntity;Z)V",
        at = @At("TAIL")
    )
    private void inject$copyFrom(ServerPlayerEntity Old, boolean Alive, CallbackInfo CI) {
        var OldNSP = (ServerPlayerDiscordAccessor) Old;
        DiscordId = OldNSP.getDiscordId();
        DiscordColour = OldNSP.getDiscordColour();
        DiscordName = OldNSP.getDiscordName();
        DiscordAvatar = OldNSP.getDiscordAvatarURL();
        NguhcraftDisplayName = OldNSP.getNguhcraftDisplayName();
        Muted = OldNSP.getMuted();
    }

    /**
     * Inject code to send a death message to discord (and for custom death messages.)
     * <p>
     * The intended injection point for this mixin is directly before the death message
     * packet is broadcast; this is mainly so we only forward the death message if it is
     * sent in the first place and so we get the right death message.
     */
    @Inject(
        method = "onDeath(Lnet/minecraft/entity/damage/DamageSource;)V",
        at = @At(
            value = "INVOKE",
            target = "net/minecraft/server/network/ServerPlayNetworkHandler.send (Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/PacketCallbacks;)V",
            ordinal = 0
        )
    )
    private void inject$onDeath(DamageSource Source, CallbackInfo CI, @Local Text DeathMessage) {
        Discord.BroadcastDeathMessage((ServerPlayerEntity) (Object) this, DeathMessage);
    }

    /** Save Nbt data to the player file. */
    @Inject(method = "writeCustomDataToNbt(Lnet/minecraft/nbt/NbtCompound;)V", at = @At("TAIL"))
    private void inject$saveData(@NotNull NbtCompound nbt, CallbackInfo ci) {
        var tag = nbt.getCompound(ServerPlayerAccessor.TAG_ROOT);
        tag.putLong(TAG_DISCORD_ID, DiscordId);
        tag.putInt(TAG_DISCORD_COLOUR, DiscordColour);
        tag.putString(TAG_DISCORD_NAME, DiscordName);
        tag.putString(TAG_DISCORD_AVATAR, DiscordAvatar);
        tag.putString(TAG_LAST_KNOWN_NAME, getNameForScoreboard());
        tag.putBoolean(TAG_MUTED, Muted);
        nbt.put(ServerPlayerAccessor.TAG_ROOT, tag);
    }

    /** Put player in adventure mode if they are unlinked. */
    @SuppressWarnings("UnreachableCode")
    @Inject(method = "tick()V", at = @At("HEAD"))
    private void inject$tick(CallbackInfo ci) {
        if (!isLinkedOrOperator()) {
            var SP = (ServerPlayerEntity) (Object) this;
            SP.changeGameMode(GameMode.ADVENTURE);
        }
    }
}
