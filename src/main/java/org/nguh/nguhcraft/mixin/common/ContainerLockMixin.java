package org.nguh.nguhcraft.mixin.common;

import net.minecraft.inventory.ContainerLock;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import org.nguh.nguhcraft.item.LockItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ContainerLock.class)
public abstract class ContainerLockMixin {
    @Inject(method = "fromNbt", at = @At("RETURN"), cancellable = true)
    private static void inject$fromNbt(
        NbtCompound Tag,
        RegistryWrapper.WrapperLookup WL,
        CallbackInfoReturnable<ContainerLock> CIR
    ) {
        CIR.setReturnValue(LockItem.UpgradeContainerLock(CIR.getReturnValue()));
    }
}
