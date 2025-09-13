package org.nguh.nguhcraft.mixin.common;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.Item;
import org.nguh.nguhcraft.NguhDamageTypes;
import org.nguh.nguhcraft.enchantment.NguhcraftEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static org.nguh.nguhcraft.Utils.EnchantLvl;

@Mixin(Item.class)
public abstract class ItemMixin {
    /** Implement the ‘arcane’ enchantment / damage type. */
    @Inject(method = "getDamageSource", at = @At("HEAD"), cancellable = true)
    private void inject$getDamageSource(LivingEntity User, CallbackInfoReturnable<DamageSource> CIR) {
        var W = User.getWorld();
        var Weapon = User.getWeaponStack();
        if (EnchantLvl(User.getWorld(), Weapon, NguhcraftEnchantments.ARCANE) > 0)
            CIR.setReturnValue(NguhDamageTypes.Arcane(W, User));
    }
}
