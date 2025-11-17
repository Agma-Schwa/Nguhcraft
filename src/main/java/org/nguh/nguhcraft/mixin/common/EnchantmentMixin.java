package org.nguh.nguhcraft.mixin.common;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Holder;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.nguh.nguhcraft.Constants;
import org.nguh.nguhcraft.Utils;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Enchantment.class)
public abstract class EnchantmentMixin {
    @Shadow @Final public static int MAX_LEVEL;

    @Unique
    private static Component FormatLevel(int Lvl) {
        return Component.literal(Lvl >= 255 || Lvl < 0 ? "∞" : Utils.RomanNumeral(Lvl));
    }

    /**
     * Render large enchantment levels properly.
     *
     * @author Sirraide
     * @reason Easier to rewrite the entire thing.
     */
    @Overwrite
    public static Component getFullname(Holder<Enchantment> Key, int Lvl) {
        var E = Key.value();
        var Name = Component.empty().append(E.description());
        if (E.getMaxLevel() > 1 || Lvl > 1) Name.append(CommonComponents.SPACE).append(FormatLevel(Lvl));
        Name.withStyle(Key.is(EnchantmentTags.CURSE) ? ChatFormatting.RED : ChatFormatting.GRAY);
        return Name;
    }

    /** Save the initial damage so we can check whether it was modified. */
    @Inject(method = "modifyDamage", at = @At("HEAD"))
    private void inject$modifyDamage$0(
        ServerLevel W,
        int Lvl,
        ItemStack S,
        Entity User,
        DamageSource DS,
        MutableFloat Damage,
        CallbackInfo CI,
        @Share("BaseDamage") LocalRef<Float> BaseDamage
    ) {
        BaseDamage.set(Damage.floatValue());
    }

    /** And set it to ∞ if it was and we’re at max level. */
    @Inject(method = "modifyDamage", at = @At("TAIL"))
    private void inject$modifyDamage$1(
        ServerLevel W,
        int Lvl,
        ItemStack S,
        Entity User,
        DamageSource DS,
        MutableFloat Damage,
        CallbackInfo CI,
        @Share("BaseDamage") LocalRef<Float> BaseDamage
    ) {
        // The damage was modified; apply our override.
        if (Damage.floatValue() > BaseDamage.get() && Lvl == MAX_LEVEL)
            Damage.setValue(Constants.BIG_VALUE_FLOAT);
    }
}
