package org.nguh.nguhcraft.mixin.datafixer;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.OptionalDynamic;
import net.minecraft.util.datafix.fixes.ItemStackComponentizationFix;
import org.nguh.nguhcraft.PaperDataFixer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = ItemStackComponentizationFix.class, priority = 1)
public abstract class ItemStackComponentizationFixMixin {
    /** Yeet the HideFlags. */
    @Redirect(
        method = "fixItemStack",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/serialization/OptionalDynamic;asInt(I)I",
            remap = false, // Mojang’s serialisation library is separate and not obfuscated.
            ordinal = 0
        )
    )
    private static int inject$fixStack$0(OptionalDynamic<?> Instance, int i) { return 0; }

    /** Run lore data fix. */
    @Inject(method = "fixItemStack", at = @At("HEAD"))
    private static void inject$fixStack$1(
        ItemStackComponentizationFix.ItemStackData Data,
        Dynamic<?> Dyn,
        CallbackInfo CI
    ) {
        PaperDataFixer.FixSavedLore(Data);
    }

    /** Run enchantments namespace migration fix. */
    @Inject(
        method = "fixEnchantments",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/List;isEmpty()Z"
        )
    )
    private static void inject$fixEnchantments(
        ItemStackComponentizationFix.ItemStackData Data,
        Dynamic<?> dynamic,
        String nbtKey,
        String componentId,
        boolean hideInTooltip,
        CallbackInfo ci,
        @Local LocalRef<List<Pair<String, Integer>>> Enchantments
    ) {
        Enchantments.set(PaperDataFixer.FixEnchantments(Enchantments.get()));
    }
}
