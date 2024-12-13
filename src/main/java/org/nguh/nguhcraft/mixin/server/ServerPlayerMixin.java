package org.nguh.nguhcraft.mixin.server;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SentMessage;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nguh.nguhcraft.server.Home;
import org.nguh.nguhcraft.server.ServerUtils;
import org.nguh.nguhcraft.server.accessors.ServerPlayerAccessor;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerMixin extends PlayerEntity implements ServerPlayerAccessor {
    public ServerPlayerMixin(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(world, pos, yaw, gameProfile);
    }

    @Unique private boolean Vanished = false;
    @Unique private boolean IsModerator = false;
    @Unique private boolean BypassesRegionProtection = false;
    @Unique private boolean IsSubscribedToConsole = false;
    @Unique private TeleportTarget LastPositionBeforeTeleport = null;
    @Unique private List<Home> Homes = new ArrayList<>();

    @Unique static private final Logger LOGGER = LogUtils.getLogger();

    @Shadow public abstract void sendMessage(Text message);

    @Override public boolean getVanished() { return Vanished; }
    @Override public void setVanished(boolean vanished) { Vanished = vanished; }

    @Override public boolean isModerator() { return IsModerator; }
    @Override public void setIsModerator(boolean IsModerator) { this.IsModerator = IsModerator; }

    @Override public boolean getBypassesRegionProtection() { return BypassesRegionProtection; }
    @Override public void setBypassesRegionProtection(boolean bypassesProtection) {
        BypassesRegionProtection = bypassesProtection;
    }

    @Override public boolean isSubscribedToConsole() { return IsSubscribedToConsole; }
    @Override public void setIsSubscribedToConsole(boolean IsSubscribedToConsole) { this.IsSubscribedToConsole = IsSubscribedToConsole; }

    @Override @Nullable public TeleportTarget getLastPositionBeforeTeleport() { return LastPositionBeforeTeleport; }
    @Override public void setLastPositionBeforeTeleport(TeleportTarget target) { LastPositionBeforeTeleport = target; }

    @Override
    public List<Home> Homes() { return Homes; }

    /** Load custom data from Nbt. */
    @Override
    public void LoadGeneralNguhcraftNbt(@NotNull NbtCompound Nbt) {
        if (Nbt.contains(TAG_ROOT)) {
            var Nguh = Nbt.getCompound(TAG_ROOT);
            Vanished = Nguh.getBoolean(TAG_VANISHED);
            IsModerator = Nguh.getBoolean(TAG_IS_MODERATOR);
            BypassesRegionProtection = Nguh.getBoolean(TAG_BYPASSES_REGION_PROTECTION);
            IsSubscribedToConsole = Nguh.getBoolean(TAG_IS_SUBSCRIBED_TO_CONSOLE);
            LastPositionBeforeTeleport = Nguh.contains(TAG_LAST_POSITION_BEFORE_TELEPORT)
                ? ServerUtils.TeleportTargetFromNbt(getServer(), Nguh.getCompound(TAG_LAST_POSITION_BEFORE_TELEPORT))
                : null;
            if (Nguh.contains(TAG_HOMES)) {
                var HomesTag = Nguh.getList(TAG_HOMES, NbtElement.COMPOUND_TYPE);
                for (var H : HomesTag) Homes.add(Home.Load((NbtCompound) H));
            }
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
        var OldNSP = (ServerPlayerAccessor) Old;
        Vanished = OldNSP.getVanished();
        IsModerator = OldNSP.isModerator();
        BypassesRegionProtection = OldNSP.getBypassesRegionProtection();
        Homes = OldNSP.Homes();
        LastPositionBeforeTeleport = OldNSP.getLastPositionBeforeTeleport();
        IsSubscribedToConsole = OldNSP.isSubscribedToConsole();
    }

    /** Save Nbt data to the player file. */
    @Inject(method = "writeCustomDataToNbt(Lnet/minecraft/nbt/NbtCompound;)V", at = @At("TAIL"))
    private void inject$saveData(@NotNull NbtCompound Nbt, CallbackInfo ci) {
        // Save data.
        var Nguh = Nbt.getCompound(TAG_ROOT);
        Nguh.putBoolean(TAG_VANISHED, Vanished);
        Nguh.putBoolean(TAG_IS_MODERATOR, IsModerator);
        Nguh.putBoolean(TAG_BYPASSES_REGION_PROTECTION, BypassesRegionProtection);
        Nguh.putBoolean(TAG_IS_SUBSCRIBED_TO_CONSOLE, IsSubscribedToConsole);
        if (LastPositionBeforeTeleport != null)
            Nguh.put(TAG_LAST_POSITION_BEFORE_TELEPORT, ServerUtils.TeleportTargetToNbt(LastPositionBeforeTeleport));

        // Save homes.
        var HomesTag = new NbtList();
        for (var H : Homes) HomesTag.add(H.Save());

        // Store tags.
        Nguh.put(TAG_HOMES, HomesTag);
        Nbt.put(TAG_ROOT, Nguh);
    }

    /**
     * Make sure we *never* send signed messages under any circumstances.
     * <p>
     * @author Sirraide
     * @reason Last line of defence. This should *never* be called in the first place. If
     *         it ever is, that means we forgot to override something else.
     */
     @Overwrite
     public void sendChatMessage(@NotNull SentMessage message, boolean filterMaskEnabled, MessageType.Parameters params) {
         LOGGER.error("Refusing to send signed message to '{}': {}", getNameForScoreboard(), message.content());
     }
}
