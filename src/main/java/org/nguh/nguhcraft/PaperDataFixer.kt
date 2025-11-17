package org.nguh.nguhcraft

import net.minecraft.util.datafix.fixes.ItemStackComponentizationFix
import org.nguh.nguhcraft.Nguhcraft.Companion.MOD_ID
import kotlin.jvm.optionals.getOrNull

object PaperDataFixer {
    /**
    * Fix Enchantments
    *
    * In the paper server, our custom enchantments were added to the
    * 'minecraft' namespace; migrate them to 'nguhcraft' instead and
    * remove any that were never actually implemented.
    */
    @JvmStatic
    fun FixEnchantments(Enchantments: List<MojangPair<String, Int>>) : List<MojangPair<String, Int>> {
        val NewList = mutableListOf<MojangPair<String, Int>>()
        for ((Name, Level) in Enchantments) {
            val NewName = when (Name) {
                "minecraft:health" -> "$MOD_ID:health"
                "minecraft:homing" -> "$MOD_ID:homing"
                "minecraft:hypershot" -> "$MOD_ID:hypershot"
                "minecraft:saturation" -> "$MOD_ID:saturation"
                "minecraft:smelting" -> "$MOD_ID:smelting"
                "minecraft:seek" -> continue
                else -> Name
            }

            NewList += MojangPair(NewName, Level)
        }
        return NewList
    }

    /**
    * Import SavedLore as Lore.
    *
    * Once upon a time, this was a paper server, and we wanted to
    * be able to display enchantments with levels greater than 10.
    *
    * The problem with that was that, since we couldn’t change the
    * client because we were using a plugin, we had to resort to
    * rather horrible hacks to accomplish that: hiding the actual
    * enchantments using hide flags and formatting the enchantments
    * as lore. At the same time, some items also had *actual* lore,
    * so we had to find a way to deal with the generated lore that
    * should be shown to the client, as well as the actual lore that
    * needed to be appended every time we would generate the lore.
    *
    * To that end, a new tag was introduced to item stacks, called
    * ‘SavedLore’, which would store the actual lore of the item,
    * and be used to generate the lore to be sent to the client.
    *
    * This is no longer necessary, but we still need to clean up the
    * mess this left behind. Specifically, if there is a 'display' tag
    * that contains a 'SavedLore' entry, we need to
    *
    *    1. Delete the 'Lore' entry, if present.
    *    2. Rename 'SavedLore' to 'Lore'.
    *    3. Remove the 'HideFlags'.
    */
    @JvmStatic
    fun FixSavedLore(Data: ItemStackComponentizationFix.ItemStackData) {
        Data.fixSubTag("display", false) fixer@{ DisplayDyn ->
            val SLRes = DisplayDyn.get("SavedLore").result()
            if (SLRes.isPresent) {
                var SL = SLRes.get()
                val Name = DisplayDyn.get("Name").asString().result()

                // Unfortunately, it seems that potion effects have made their
                // way into the SavedLore. Fortunately, there is only a single
                // affected potion.
                if (Name.isPresent && Name.get().contains("Ancient Drop of Cherry")) {
                    SL = DisplayDyn.createList(SL.asStream().filter {
                        it.asString("").endsWith("\"color\":\"#AAAAAA\"}")
                    })
                }

                // Some items have a string as their SavedLore rather than a
                // component. Make sure to repair that too.
                else {
                    val Str = SL.asStream().findFirst().getOrNull()?.asString("")
                    if (Str != null && !Str.startsWith("{")) {
                        SL = DisplayDyn.createList(SL.asStream().map { DisplayDyn.createString(
                            """{"text":"${it.asString("<???>")}","italic":true,"color":"gray"}"""
                        )})
                    }
                }

                // At last, we can yeet all of this nonsense...
                DisplayDyn.remove("Lore")
                   .remove("SavedLore")
                   .set("Lore", SL)
            }

            // If something has lore, but not saved lore, yeet it.
            else {
                DisplayDyn.remove("Lore")
            }
        }
    }
}
