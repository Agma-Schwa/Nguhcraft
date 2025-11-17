package org.nguh.nguhcraft.mixin.common;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BundleItem;
import net.minecraft.sounds.SoundEvents;
import org.nguh.nguhcraft.item.KeyChainItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BundleItem.class)
public abstract class BundleItemMixin {
    @Shadow private static void playInsertSound(Entity entity) {}
    @Shadow private static void playRemoveOneSound(Entity entity) {}

    @Redirect(
        method = {"overrideStackedOnOther", "overrideOtherStackedOnMe"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/BundleItem;playInsertSound(Lnet/minecraft/world/entity/Entity;)V"
        )
    )
    private void inject$playInsertSound(Entity E) {
        if (((Object)this) instanceof KeyChainItem)
            E.playSound(SoundEvents.CHAIN_PLACE, 0.8F, 0.8F + E.level().getRandom().nextFloat() * 0.4F);
        else
            playInsertSound(E);
    }

    @Redirect(
        method = {"overrideStackedOnOther", "overrideOtherStackedOnMe"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/BundleItem;playRemoveOneSound(Lnet/minecraft/world/entity/Entity;)V"
        )
    )
    private void inject$playRemoveOneSound(Entity E) {
        if (((Object)this) instanceof KeyChainItem)
            E.playSound(SoundEvents.CHAIN_PLACE, 0.8F, 0.8F + E.level().getRandom().nextFloat() * 0.4F);
        else
            playRemoveOneSound(E);
    }
}
