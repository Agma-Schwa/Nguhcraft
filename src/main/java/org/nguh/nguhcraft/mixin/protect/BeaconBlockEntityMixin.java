package org.nguh.nguhcraft.mixin.protect;

import net.minecraft.block.entity.BeaconBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.ContainerLock;
import net.minecraft.text.Text;
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
    @Unique private static final Text BEACON_NAME = Text.translatable("block.minecraft.beacon");
    @Unique private String Lock = null;

    @Override public @Nullable String Nguhcraft$GetLock() { return Lock; }
    @Override public void Nguhcraft$SetLockInternal(@Nullable String NewLock) { Lock = NewLock; }
    @Override public @NotNull Text Nguhcraft$GetName() { return BEACON_NAME; }

    @Redirect(
        method = "createMenu",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/entity/LockableContainerBlockEntity;checkUnlocked(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/inventory/ContainerLock;Lnet/minecraft/text/Text;)Z"
        )
    )
    private boolean inject$createMenu$checkUnlocked(PlayerEntity PE, ContainerLock Unused1, Text Unused2) {
        return CheckCanOpen(this, PE, PE.getMainHandStack());
    }
}
