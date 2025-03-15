package org.nguh.nguhcraft.mixin.server;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;
import org.nguh.nguhcraft.server.ServerUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

import static org.nguh.nguhcraft.Utils.EnchantLvl;

@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {
    /** Make channeling work with melee weapons. */
    @Inject(
        method = "onTargetDamaged(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/damage/DamageSource;Lnet/minecraft/item/ItemStack;Ljava/util/function/Consumer;)V",
        at = @At("HEAD")
    )
    private static void inject$onTargetDamaged(
        ServerWorld SW,
        Entity E,
        DamageSource DS,
        @Nullable ItemStack Weapon,
        Consumer<Item> BreakCB,
        CallbackInfo CI
    ) {
        if (
            DS.getAttacker() instanceof LivingEntity &&
            E instanceof LivingEntity LE &&
            Weapon != null &&
            EnchantLvl(SW, Weapon, Enchantments.CHANNELING) >= 2
        ) {
            LE.timeUntilRegen = 0; // Make sure this can deal damage.
            ServerUtils.StrikeLightning(SW, E.getPos());
        }
    }
}
