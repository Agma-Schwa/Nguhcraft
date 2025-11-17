package org.nguh.nguhcraft.mixin.protect;

import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.LockCode;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nguh.nguhcraft.item.LockableBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static org.nguh.nguhcraft.item.LockableBlockEntityKt.CheckCanOpen;

@Mixin(BeaconBlockEntity.class)
public abstract class BeaconBlockEntityMixin implements LockableBlockEntity {
    @Unique private static final Component BEACON_NAME = Component.translatable("block.minecraft.beacon");
    @Unique private String Lock = null;

    @Override public @Nullable String Nguhcraft$GetLock() { return Lock; }
    @Override public void Nguhcraft$SetLockInternal(@Nullable String NewLock) { Lock = NewLock; }
    @Override public @NotNull Component Nguhcraft$GetName() { return BEACON_NAME; }

    @Redirect(
        method = "createMenu",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/entity/BaseContainerBlockEntity;canUnlock(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/LockCode;Lnet/minecraft/network/chat/Component;)Z"
        )
    )
    private boolean inject$createMenu$checkUnlocked(Player PE, LockCode Unused1, Component Unused2) {
        return CheckCanOpen(this, PE, PE.getMainHandItem());
    }
}
