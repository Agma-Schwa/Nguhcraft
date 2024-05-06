package org.nguh.nguhcraft.mixin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldSaveHandler;
import net.minecraft.world.level.storage.LevelStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MinecraftServer.class)
public interface MinecraftServerAccessor {
    @Accessor
    WorldSaveHandler getSaveHandler();

    @Accessor
    LevelStorage.Session getSession();
}
