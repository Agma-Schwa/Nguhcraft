package org.nguh.nguhcraft.item

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.minecraft.client.data.ItemModelGenerator
import net.minecraft.client.data.Model
import net.minecraft.client.data.Models
import net.minecraft.component.ComponentChanges
import net.minecraft.component.DataComponentTypes
import net.minecraft.item.Item
import net.minecraft.item.ItemGroups
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.item.SmithingTemplateItem
import net.minecraft.item.equipment.trim.ArmorTrimPattern
import net.minecraft.recipe.SpecialCraftingRecipe.SpecialRecipeSerializer
import net.minecraft.registry.Registerable
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.Rarity
import net.minecraft.util.Util
import org.nguh.nguhcraft.Nguhcraft.Companion.Id
import org.nguh.nguhcraft.Nguhcraft.Companion.RKey
import org.nguh.nguhcraft.Utils
import org.nguh.nguhcraft.block.ChestVariant
import org.nguh.nguhcraft.block.NguhBlocks

object NguhItems {
    // =========================================================================
    //  Items
    // =========================================================================
    val LOCK: Item = CreateItem(LockItem.ID, LockItem())
    val KEY: Item = CreateItem(KeyItem.ID, KeyItem())
    val SLABLET_1: Item = CreateItem(Id("slablet_1"), Item.Settings().maxCount(64).rarity(Rarity.UNCOMMON).fireproof())
    val SLABLET_2: Item = CreateItem(Id("slablet_2"), Item.Settings().maxCount(64).rarity(Rarity.UNCOMMON).fireproof())
    val SLABLET_4: Item = CreateItem(Id("slablet_4"), Item.Settings().maxCount(64).rarity(Rarity.UNCOMMON).fireproof())
    val SLABLET_8: Item = CreateItem(Id("slablet_8"), Item.Settings().maxCount(64).rarity(Rarity.UNCOMMON).fireproof())
    val SLABLET_16: Item = CreateItem(Id("slablet_16"), Item.Settings().maxCount(64).rarity(Rarity.UNCOMMON).fireproof())
    val NGUHROVISION_2024_DISC: Item = CreateItem(
        Id("music_disc_nguhrovision_2024"),
        Item.Settings()
            .maxCount(1)
            .rarity(Rarity.EPIC)
            .jukeboxPlayable(RKey(RegistryKeys.JUKEBOX_SONG, "nguhrovision_2024"))
    )

    val ATLANTIC_ARMOUR_TRIM: Item = CreateSmithingTemplate("atlantic_armour_trim_smithing_template", Item.Settings().rarity(Rarity.RARE))
    val CENRAIL_ARMOUR_TRIM: Item = CreateSmithingTemplate("cenrail_armour_trim_smithing_template", Item.Settings().rarity(Rarity.RARE))
    val ICE_COLD_ARMOUR_TRIM: Item = CreateSmithingTemplate("ice_cold_armour_trim_smithing_template", Item.Settings().rarity(Rarity.RARE))
    val VENEFICIUM_ARMOUR_TRIM: Item = CreateSmithingTemplate("veneficium_armour_trim_smithing_template", Item.Settings().rarity(Rarity.RARE))

    // =========================================================================
    //  Item Data
    // =========================================================================
    val SMITHING_TEMPLATES = arrayOf(
        ATLANTIC_ARMOUR_TRIM,
        CENRAIL_ARMOUR_TRIM,
        ICE_COLD_ARMOUR_TRIM,
        VENEFICIUM_ARMOUR_TRIM
    )

    // =========================================================================
    //  Initialisation
    // =========================================================================
    fun BootstrapArmourTrims(R: Registerable<ArmorTrimPattern>) {
        fun Register(Key: String, I: Item) {
            val K = RKey(RegistryKeys.TRIM_PATTERN, Key)
            R.register(K, ArmorTrimPattern(
                K.value,
                Registries.ITEM.getEntry(I),
                Text.translatable(Util.createTranslationKey("trim_pattern", K.value)),
                false
            ))
        }

        Register("atlantic", ATLANTIC_ARMOUR_TRIM)
        Register("cenrail", CENRAIL_ARMOUR_TRIM)
        Register("ice_cold", ICE_COLD_ARMOUR_TRIM)
        Register("veneficium", VENEFICIUM_ARMOUR_TRIM)
    }

    fun BootstrapModels(G: ItemModelGenerator) {
        fun Register(I: Item, M : Model = Models.GENERATED) {
            G.register(I, M)
        }

        Register(LOCK)
        Register(KEY)
        Register(SLABLET_1)
        Register(SLABLET_2)
        Register(SLABLET_4)
        Register(SLABLET_8)
        Register(SLABLET_16)
        Register(NGUHROVISION_2024_DISC, Models.TEMPLATE_MUSIC_DISC)
        SMITHING_TEMPLATES.forEach { Register(it) }
    }

    fun Init() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register {
            it.add(LOCK)
            it.add(KEY)
            it.add(SLABLET_1)
            it.add(SLABLET_2)
            it.add(SLABLET_4)
            it.add(SLABLET_8)
            it.add(SLABLET_16)

            ChestVariant.entries.forEach { CV ->
                it.add(Utils.BuildItemStack(Items.CHEST) {
                    add(DataComponentTypes.CUSTOM_NAME, CV.DefaultName)
                    add(NguhBlocks.CHEST_VARIANT_COMPONENT, CV)
                })
            }
        }

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register {
            it.add(NGUHROVISION_2024_DISC)
        }

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register {
            for (T in SMITHING_TEMPLATES) it.add(T)
        }

        KeyLockPairingRecipe.SERIALISER = Registry.register(
            Registries.RECIPE_SERIALIZER,
            Id("crafting_special_key_lock_pairing"),
            SpecialRecipeSerializer(::KeyLockPairingRecipe)
        )

        KeyDuplicationRecipe.SERIALISER = Registry.register(
            Registries.RECIPE_SERIALIZER,
            Id("crafting_special_key_duplication"),
            SpecialRecipeSerializer(::KeyDuplicationRecipe)
        )
    }

    private fun CreateItem(S: Identifier, I: Item): Item =
        Registry.register(Registries.ITEM, S, I)

    private fun CreateItem(S: Identifier, I: Item.Settings): Item =
        Registry.register(Registries.ITEM, S, Item(I.registryKey(RegistryKey.of(RegistryKeys.ITEM, S))))

    private fun CreateSmithingTemplate(S: String, I: Item.Settings): Item {
        val Id = Id(S)
        return Registry.register(
            Registries.ITEM,
            Id,
            SmithingTemplateItem.of(
                I.registryKey(RegistryKey.of(RegistryKeys.ITEM, Id))
            )
        )
    }
}