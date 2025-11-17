package org.nguh.nguhcraft.mixin.server;

import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
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
        method = "doPostAttackEffectsWithItemSourceOnBreak(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/item/ItemStack;Ljava/util/function/Consumer;)V",
        at = @At("HEAD")
    )
    private static void inject$onTargetDamaged(
        ServerLevel SW,
        Entity E,
        DamageSource DS,
        @Nullable ItemStack Weapon,
        Consumer<Item> BreakCB,
        CallbackInfo CI
    ) {
        if (
            DS.getEntity() instanceof LivingEntity &&
            E instanceof LivingEntity LE &&
            Weapon != null &&
            EnchantLvl(SW, Weapon, Enchantments.CHANNELING) >= 2
        ) {
            LE.invulnerableTime = 0; // Make sure this can deal damage.
            ServerUtils.StrikeLightning(SW, E.position());
        }
    }
}
