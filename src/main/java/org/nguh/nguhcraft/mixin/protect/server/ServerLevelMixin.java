package org.nguh.nguhcraft.mixin.protect.server;

import net.minecraft.world.entity.Entity;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import org.jetbrains.annotations.NotNull;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.nguh.nguhcraft.protect.ProtectionManagerAccess;
import org.nguh.nguhcraft.server.ServerManagerInterface;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin extends Level implements ProtectionManagerAccess {
    @Shadow @Final private MinecraftServer server;

    protected ServerLevelMixin(WritableLevelData properties, ResourceKey<Level> registryRef, RegistryAccess registryManager, Holder<DimensionType> dimensionEntry, boolean isClient, boolean debugWorld, long seed, int maxChainedNeighborUpdates) {
        super(properties, registryRef, registryManager, dimensionEntry, isClient, debugWorld, seed, maxChainedNeighborUpdates);
    }

    @Override public @NotNull ProtectionManager Nguhcraft$GetProtectionManager() {
        return ((ServerManagerInterface)(Object)server).Nguhcraft$UnsafeGetManager(ProtectionManager.class);
    }

    @Override public void Nguhcraft$SetProtectionManager(ProtectionManager M) {
        throw new UnsupportedOperationException();
    }

    /** Disallow adding hostile entities to protected regions. */
    @Inject(method = "addEntity", at = @At("HEAD"), cancellable = true)
    private void inject$addEntity(Entity E, CallbackInfoReturnable<Boolean> CIR) {
        if (!ProtectionManager.IsSpawningAllowed(E)) {
            E.discard();
            CIR.setReturnValue(false);
        }
    }

    /** Prevent snow fall, freezing, etc. in protected regions. */
    @Inject(method = "tickPrecipitation", at = @At("HEAD"), cancellable = true)
    private void inject$tickIceAndSnow(BlockPos Pos, CallbackInfo CI) {
        if (ProtectionManager.IsProtectedBlock(this, Pos))
            CI.cancel();
    }
}
