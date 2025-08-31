package org.nguh.nguhcraft.mixin.common;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.EnchantmentTags;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
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
    private static Text FormatLevel(int Lvl) {
        return Text.literal(Lvl >= 255 || Lvl < 0 ? "∞" : Utils.RomanNumeral(Lvl));
    }

    /**
     * Render large enchantment levels properly.
     *
     * @author Sirraide
     * @reason Easier to rewrite the entire thing.
     */
    @Overwrite
    public static Text getName(RegistryEntry<Enchantment> Key, int Lvl) {
        var E = Key.value();
        var Name = Text.empty().append(E.description());
        if (E.getMaxLevel() > 1 || Lvl > 1) Name.append(ScreenTexts.SPACE).append(FormatLevel(Lvl));
        Name.formatted(Key.isIn(EnchantmentTags.CURSE) ? Formatting.RED : Formatting.GRAY);
        return Name;
    }

    /** Save the initial damage so we can check whether it was modified. */
    @Inject(method = "modifyDamage", at = @At("HEAD"))
    private void inject$modifyDamage$0(
        ServerWorld W,
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
        ServerWorld W,
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
