package org.nguh.nguhcraft

import net.minecraft.world.entity.decoration.PaintingVariant
import net.minecraft.data.worldgen.BootstrapContext
import net.minecraft.resources.ResourceKey
import net.minecraft.core.registries.Registries.PAINTING_VARIANT
import net.minecraft.network.chat.Component
import net.minecraft.ChatFormatting
import org.nguh.nguhcraft.Nguhcraft.Companion.RKey
import java.util.Optional

object NguhPaintings {
    // All paintings that can be obtained randomly by placing them.
    var PLACEABLE = listOf<ResourceKey<PaintingVariant>>()

    // Generate definitions for all paintings.
    fun Bootstrap(R: BootstrapContext<PaintingVariant>) {
        // Reset the list.
        PLACEABLE = mutableListOf()

        // Register a single variant.
        fun Register(Name: String, Width: Int, Height: Int, Placeable: Boolean = true) {
            val K = RKey(PAINTING_VARIANT, Name)
            R.register(
                K, PaintingVariant(
                    Width,
                    Height,
                    K.location(),
                    Optional.of(Component.translatable(K.location().toLanguageKey("painting", "title")).withStyle(ChatFormatting.YELLOW)),
                    Optional.of(Component.translatable(K.location().toLanguageKey("painting", "author")).withStyle(ChatFormatting.GRAY))
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
        Register("great_wave", 3, 2)
    }
}
