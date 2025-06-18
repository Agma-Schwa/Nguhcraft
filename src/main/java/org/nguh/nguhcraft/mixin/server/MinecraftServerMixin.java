package org.nguh.nguhcraft.mixin.server;

import com.google.common.collect.ImmutableList;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;
import org.nguh.nguhcraft.SyncedGameRule;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.nguh.nguhcraft.server.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin implements ServerManagerInterface {
    @Unique private final Map<Class<? extends Manager>, Manager> MANAGERS = Map.of(
        ProtectionManager.class, new ServerProtectionManager((MinecraftServer)(Object)this),
        MCBASIC.ProcedureManager.class, new MCBASIC.ProcedureManager((MinecraftServer)(Object)this),
        DisplayManager.class, new DisplayManager((MinecraftServer)(Object)(this)),
        SyncedGameRule.ManagerImpl.class, new SyncedGameRule.ManagerImpl(),
        WarpManager.class, new WarpManager()
    );

    @Unique private final List<Manager> ALL_MANAGERS = ImmutableList.copyOf(MANAGERS.values());

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Manager> @NotNull T Nguhcraft$UnsafeGetManager(@NotNull Class<T> Class) {
        return (T) Objects.requireNonNull(MANAGERS.get(Class));
    }

    @Override
    public @NotNull List<@NotNull Manager> Nguhcraft$UnsafeGetAllManagers() {
        return ALL_MANAGERS;
    }
}
