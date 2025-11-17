package org.nguh.nguhcraft.mixin.server;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.Level;
import org.nguh.nguhcraft.TridentUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TridentItem.class)
public abstract class TridentItemMixin {
    /** Implement multishot for tridents. */
    @Inject(
        method = "releaseUsing",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/projectile/Projectile;spawnProjectileFromRotation(Lnet/minecraft/world/entity/projectile/Projectile$ProjectileFactory;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;FFF)Lnet/minecraft/world/entity/projectile/Projectile;",
            shift =  At.Shift.AFTER
        )
    )
    private void inject$onStoppedUsing$0(
        ItemStack Stack,
        Level World,
        LivingEntity User,
        int Ticks,
        CallbackInfoReturnable<Boolean> CI
    ) {
        TridentUtils.ActOnTridentThrown(
            World,
            (Player) User,
            Stack,
            0
        );
    }
}
