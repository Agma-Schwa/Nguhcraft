package org.nguh.nguhcraft.mixin.protect;

import net.minecraft.block.entity.BeaconBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.ContainerLock;
import net.minecraft.text.Text;
import org.nguh.nguhcraft.item.LockableBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static org.nguh.nguhcraft.item.LockableBlockEntityKt.CheckCanOpen;

@Mixin(BeaconBlockEntity.class)
public abstract class BeaconBlockEntityMixin {
    @Redirect(
        method = "createMenu",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/entity/LockableContainerBlockEntity;checkUnlocked(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/inventory/ContainerLock;Lnet/minecraft/text/Text;)Z"
        )
    )
    private boolean inject$createMenu$checkUnlocked(PlayerEntity PE, ContainerLock Unused1, Text Unused2) {
        return CheckCanOpen(((LockableBlockEntity)this), PE, PE.getMainHandStack());
    }
}
