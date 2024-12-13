package org.nguh.nguhcraft.item

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.minecraft.item.Item
import net.minecraft.item.ItemGroups
import net.minecraft.recipe.SpecialCraftingRecipe.SpecialRecipeSerializer
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.util.Identifier
import net.minecraft.util.Rarity
import org.nguh.nguhcraft.Nguhcraft.Companion.Id

object NguhItems {
    val LOCK: Item = CreateItem(LockItem.ID, LockItem())
    val KEY: Item = CreateItem(KeyItem.ID, KeyItem())
    val SLABLET_1: Item = CreateItem(Id("slablet_1"), Item.Settings().maxCount(64).rarity(Rarity.RARE).fireproof())
    val SLABLET_2: Item = CreateItem(Id("slablet_2"), Item.Settings().maxCount(64).rarity(Rarity.RARE).fireproof())
    val SLABLET_4: Item = CreateItem(Id("slablet_4"), Item.Settings().maxCount(64).rarity(Rarity.RARE).fireproof())
    val SLABLET_8: Item = CreateItem(Id("slablet_8"), Item.Settings().maxCount(64).rarity(Rarity.RARE).fireproof())
    val SLABLET_16: Item = CreateItem(Id("slablet_16"), Item.Settings().maxCount(64).rarity(Rarity.RARE).fireproof())
    val NGUHROVISION_2024_DISC: Item = CreateItem(
        Id("music_disc_nguhrovision_2024"),
        Item.Settings()
            .maxCount(1)
            .rarity(Rarity.EPIC)
            .jukeboxPlayable(RegistryKey.of(RegistryKeys.JUKEBOX_SONG, Id("nguhrovision_2024")))
    )

    fun Init() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register {
            it.add(LOCK)
            it.add(KEY)
            it.add(SLABLET_1)
            it.add(SLABLET_2)
            it.add(SLABLET_4)
            it.add(SLABLET_8)
            it.add(SLABLET_16)
        }

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register {
            it.add(NGUHROVISION_2024_DISC)
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
}