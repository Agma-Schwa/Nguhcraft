package org.nguh.nguhcraft.server.accessors;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.TeleportTarget;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nguh.nguhcraft.server.Home;

import java.util.List;

public interface ServerPlayerAccessor {
    String TAG_ROOT = "Nguhcraft";
    String TAG_HOMES = "Homes";
    String TAG_VANISHED = "Vanished";
    String TAG_IS_MODERATOR = "IsModerator";
    String TAG_IS_SUBSCRIBED_TO_CONSOLE = "IsSubscribedToConsole";
    String TAG_BYPASSES_REGION_PROTECTION = "BypassesRegionProtection";
    String TAG_LAST_POSITION_BEFORE_TELEPORT = "LastPositionBeforeTeleport";

    boolean getVanished();
    void setVanished(boolean vanished);

    boolean isModerator();
    void setIsModerator(boolean IsModerator);

    boolean isSubscribedToConsole();
    void setIsSubscribedToConsole(boolean IsSubscribedToConsole);

    boolean getBypassesRegionProtection();
    void setBypassesRegionProtection(boolean bypassesProtection);

    @Nullable TeleportTarget getLastPositionBeforeTeleport();
    void setLastPositionBeforeTeleport(TeleportTarget target);

    List<Home> Homes();

    void LoadGeneralNguhcraftNbt(@NotNull NbtCompound nbt);
}
