package org.nguh.nguhcraft

import net.minecraft.entity.decoration.painting.PaintingVariant
import net.minecraft.registry.Registerable
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.nguh.nguhcraft.Nguhcraft.Companion.Id
import org.nguh.nguhcraft.Nguhcraft.Companion.RKey
import java.util.Optional

object NguhPaintings {
    val CHILLVANA_METRO = of("chillvana_metro")
    val GAMBIANHOLIDAY = of("gambianholiday")
    val GOLD_NGUH = of("gold_nguh")
    val GOLD_NGUH_SMALL = of("gold_nguh_small")
    val MAP = of("map")
    val RABO = of("rabo")
    val RAIL_DIAGRAM = of("rail_diagram")
    val RAILS_OF_ERAS = of("rails_of_eras")

    // All paintings that can be obtained randomly by placing them.
    val PLACEABLE = arrayOf(CHILLVANA_METRO, GAMBIANHOLIDAY, GOLD_NGUH, GOLD_NGUH_SMALL, MAP, RABO, RAIL_DIAGRAM)

    fun Bootstrap(R: Registerable<PaintingVariant>) {
        fun Register(K: RegistryKey<PaintingVariant>, Width: Int, Height: Int) {
            R.register(K, PaintingVariant(
                Width,
                Height,
                K.value,
                Optional.of(Text.translatable(K.value.toTranslationKey("painting", "title")).formatted(Formatting.YELLOW)),
                Optional.of(Text.translatable(K.value.toTranslationKey("painting", "author")).formatted(Formatting.GRAY))
            ))
        }

        Register(CHILLVANA_METRO, 3, 2)
        Register(GAMBIANHOLIDAY, 1, 1)
        Register(GOLD_NGUH, 1, 1)
        Register(GOLD_NGUH_SMALL, 1, 1)
        Register(MAP, 5, 5)
        Register(RABO, 1, 4)
        Register(RAIL_DIAGRAM, 3, 3)
        Register(RAILS_OF_ERAS, 3, 2)
    }

    fun of(Key: String) = RKey(RegistryKeys.PAINTING_VARIANT, Key)
}
