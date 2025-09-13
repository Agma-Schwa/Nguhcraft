package org.nguh.nguhcraft.mixin.common;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import static org.nguh.nguhcraft.Utils.RomanNumeral;

@Mixin(net.minecraft.component.type.PotionContentsComponent.class)
public abstract class PotionContentsComponentMixin {
    /**
     * It’s easier to just replace this entirely.
     * @author Sirraide
     * @reason See above.
     */
    @Overwrite
    public static MutableText getEffectText(RegistryEntry<StatusEffect> Effect, int A) {
        var Name = Text.translatable(Effect.value().getTranslationKey());
        return A > 0 ? Text.translatable("potion.withAmplifier", Name, Text.of(RomanNumeral(A + 1))) : Name;
    }
}
