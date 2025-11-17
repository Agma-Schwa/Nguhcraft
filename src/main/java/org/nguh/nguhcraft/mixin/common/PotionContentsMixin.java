package org.nguh.nguhcraft.mixin.common;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import static org.nguh.nguhcraft.Utils.RomanNumeral;

@Mixin(net.minecraft.world.item.alchemy.PotionContents.class)
public abstract class PotionContentsMixin {
    /**
     * It’s easier to just replace this entirely.
     * @author Sirraide
     * @reason See above.
     */
    @Overwrite
    public static MutableComponent getPotionDescription(Holder<MobEffect> Effect, int A) {
        var Name = Component.translatable(Effect.value().getDescriptionId());
        return A > 0 ? Component.translatable("potion.withAmplifier", Name, Component.nullToEmpty(RomanNumeral(A + 1))) : Name;
    }
}
