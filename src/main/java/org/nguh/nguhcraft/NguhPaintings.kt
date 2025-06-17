package org.nguh.nguhcraft

import net.minecraft.entity.decoration.painting.PaintingVariant
import net.minecraft.registry.Registerable
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys.PAINTING_VARIANT
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.nguh.nguhcraft.Nguhcraft.Companion.RKey
import java.util.Optional

object NguhPaintings {
    // All paintings that can be obtained randomly by placing them.
    var PLACEABLE = listOf<RegistryKey<PaintingVariant>>()

    // Generate definitions for all paintings.
    fun Bootstrap(R: Registerable<PaintingVariant>) {
        // Reset the list.
        PLACEABLE = mutableListOf<RegistryKey<PaintingVariant>>()

        // Register a single variant.
        fun Register(Name: String, Width: Int, Height: Int, Placeable: Boolean = true) {
            val K = RKey(PAINTING_VARIANT, Name)
            R.register(
                K, PaintingVariant(
                    Width,
                    Height,
                    K.value,
                    Optional.of(Text.translatable(K.value.toTranslationKey("painting", "title")).formatted(Formatting.YELLOW)),
                    Optional.of(Text.translatable(K.value.toTranslationKey("painting", "author")).formatted(Formatting.GRAY))
                )
            )

            if (Placeable) (PLACEABLE as MutableList).add(K)
        }

        Register("chillvana_metro", 3, 2)
        Register("gambianholiday", 1, 1)
        Register("gold_nguh", 1, 1)
        Register("gold_nguh_small", 1, 1)
        Register("kozdenen_rail_diagram", 3, 2)
        Register("leshrail_diagram", 2, 2)
        Register("map", 5, 5)
        Register("rabo", 1, 4)
        Register("rail_diagram", 3, 3)
        Register("rails_of_eras", 3, 2)
        Register("rauratoshan_loop", 4, 3)
    }
}
