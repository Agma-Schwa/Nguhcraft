package org.nguh.nguhcraft.item

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.minecraft.block.Block
import net.minecraft.client.data.ItemModelGenerator
import net.minecraft.client.data.Model
import net.minecraft.client.data.Models
import net.minecraft.component.DataComponentTypes
import net.minecraft.item.*
import net.minecraft.item.equipment.ArmorMaterial
import net.minecraft.item.equipment.EquipmentAsset
import net.minecraft.item.equipment.EquipmentAssetKeys
import net.minecraft.item.equipment.EquipmentType
import net.minecraft.item.equipment.trim.ArmorTrimPattern
import net.minecraft.recipe.SpecialCraftingRecipe.SpecialRecipeSerializer
import net.minecraft.registry.*
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.registry.tag.TagKey
import net.minecraft.sound.SoundEvent
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.Rarity
import net.minecraft.util.Util
import org.nguh.nguhcraft.Nguhcraft.Companion.Id
import org.nguh.nguhcraft.Nguhcraft.Companion.RKey
import org.nguh.nguhcraft.Utils
import org.nguh.nguhcraft.block.ChestVariant
import org.nguh.nguhcraft.block.NguhBlocks
import java.util.*

object NguhItems {
    // =========================================================================
    //  Item Tags
    // =========================================================================
    val TINTED_LOGS: TagKey<Item> = TagKey.of(RegistryKeys.ITEM, Id("tinted_oak_logs"))

    // =========================================================================
    //  Items
    // =========================================================================
    val LOCK: Item = CreateItem(LockItem.ID, LockItem())
    val KEY: Item = CreateItem(KeyItem.ID, KeyItem())
    val MASTER_KEY: Item = CreateItem(MasterKeyItem.ID, MasterKeyItem())
    val KEY_CHAIN: Item = CreateItem(KeyChainItem.ID, KeyChainItem())
    val SLABLET_1: Item = CreateItem(Id("slablet_1"), Item.Settings().maxCount(64).rarity(Rarity.UNCOMMON).fireproof())
    val SLABLET_2: Item = CreateItem(Id("slablet_2"), Item.Settings().maxCount(64).rarity(Rarity.UNCOMMON).fireproof())
    val SLABLET_4: Item = CreateItem(Id("slablet_4"), Item.Settings().maxCount(64).rarity(Rarity.UNCOMMON).fireproof())
    val SLABLET_8: Item = CreateItem(Id("slablet_8"), Item.Settings().maxCount(64).rarity(Rarity.UNCOMMON).fireproof())
    val SLABLET_16: Item = CreateItem(Id("slablet_16"), Item.Settings().maxCount(64).rarity(Rarity.UNCOMMON).fireproof())
    val SLAB_SHAVINGS_1: Item = CreateItem(Id("slab_shavings_1"), Item.Settings().maxCount(64).rarity(Rarity.UNCOMMON).fireproof())
    val SLAB_SHAVINGS_8: Item = CreateItem(Id("slab_shavings_8"), Item.Settings().maxCount(64).rarity(Rarity.UNCOMMON).fireproof())
    val NGUHROVISION_2024_DISC: Item = CreateItem(
        Id("music_disc_nguhrovision_2024"),
        Item.Settings()
            .maxCount(1)
            .rarity(Rarity.EPIC)
            .jukeboxPlayable(RKey(RegistryKeys.JUKEBOX_SONG, "nguhrovision_2024"))
    )

    // =========================================================================
    //  Armour Trims
    // =========================================================================
    class ArmourTrim(Name: String) : ItemConvertible {
        val Template: Item = CreateSmithingTemplate("${Name}_armour_trim_smithing_template", Item.Settings().rarity(Rarity.RARE))
        val Trim = RKey(RegistryKeys.TRIM_PATTERN, Name)
        override fun asItem(): Item = Template
    }

    val ATLANTIC_ARMOUR_TRIM = ArmourTrim("atlantic")
    val CENRAIL_ARMOUR_TRIM = ArmourTrim("cenrail")
    val ICE_COLD_ARMOUR_TRIM = ArmourTrim("ice_cold")
    val VENEFICIUM_ARMOUR_TRIM = ArmourTrim("veneficium")
    val ALL_NGUHCRAFT_ARMOUR_TRIMS = arrayOf(
        ATLANTIC_ARMOUR_TRIM,
        CENRAIL_ARMOUR_TRIM,
        ICE_COLD_ARMOUR_TRIM,
        VENEFICIUM_ARMOUR_TRIM
    )

    // =========================================================================
    //  Amethyst Armour & Tools
    // =========================================================================
    // TODO: Darker colour palette for trimming and corresponding overrides.

    /** Items that can be used to repair amethyst armour. */
    val REPAIRS_AMETHYST_ARMOUR: TagKey<Item> = TagKey.of(RegistryKeys.ITEM, Id("repairs_amethyst_armour"))

    /** Items that cannot be mined by amethyst tools. */
    val INCORRECT_FOR_AMETHYST_TOOL: TagKey<Block> = TagKey.of(RegistryKeys.BLOCK, Id("incorrect_for_amethyst_tool"))

    /** Sound events for amethyst armour. */
    val AMETHYST_ARMOUR_SOUNDS: RegistryEntry.Reference<SoundEvent> = Registries.SOUND_EVENT.getOrThrow(
        RegistryKey.of(RegistryKeys.SOUND_EVENT, SoundEvents.BLOCK_AMETHYST_CLUSTER_PLACE.id)
    )

    /** Asset key and armour material. */
    val AMETHYST_EQUIPMENT_ASSET_KEY: RegistryKey<EquipmentAsset> = RegistryKey.of(EquipmentAssetKeys.REGISTRY_KEY, Id("amethyst"))
    val AMETHYST_ARMOUR_MATERIAL = ArmorMaterial(
        45,
        Util.make(EnumMap(EquipmentType::class.java)) {
            it[EquipmentType.BOOTS] = 4
            it[EquipmentType.LEGGINGS] = 8
            it[EquipmentType.CHESTPLATE] = 14
            it[EquipmentType.HELMET] = 4
            it[EquipmentType.BODY] = 15
        },
        40,
        AMETHYST_ARMOUR_SOUNDS,
        4.0F,
        .3F,
        REPAIRS_AMETHYST_ARMOUR,
        AMETHYST_EQUIPMENT_ASSET_KEY
    )

    /** Tool material. */
    val AMETHYST_TOOL_MATERIAL = ToolMaterial(
        INCORRECT_FOR_AMETHYST_TOOL,
        6071,
        14.0F,
        6.0F,
        40,
        REPAIRS_AMETHYST_ARMOUR
    )

    val AMETHYST_HELMET = CreateItem(
        Id("amethyst_helmet"),
        Item.Settings()
            .fireproof()
            .rarity(Rarity.EPIC)
            .armor(AMETHYST_ARMOUR_MATERIAL, EquipmentType.HELMET)
    )

    val AMETHYST_CHESTPLATE = CreateItem(
        Id("amethyst_chestplate"),
        Item.Settings()
            .fireproof()
            .rarity(Rarity.EPIC)
            .armor(AMETHYST_ARMOUR_MATERIAL, EquipmentType.CHESTPLATE)
    )

    val AMETHYST_LEGGINGS = CreateItem(
        Id("amethyst_leggings"),
        Item.Settings()
            .fireproof()
            .rarity(Rarity.EPIC)
            .armor(AMETHYST_ARMOUR_MATERIAL, EquipmentType.LEGGINGS)
    )

    val AMETHYST_BOOTS = CreateItem(
        Id("amethyst_boots"),
        Item.Settings()
            .fireproof()
            .rarity(Rarity.EPIC)
            .armor(AMETHYST_ARMOUR_MATERIAL, EquipmentType.BOOTS)
    )

    val AMETHYST_SWORD = CreateItem(
        Id("amethyst_sword"),
        Item.Settings()
            .fireproof()
            .rarity(Rarity.EPIC)
            .sword(AMETHYST_TOOL_MATERIAL, 5.0F, -1.2F)
    )

    val AMETHYST_PICKAXE = CreateItem(
        Id("amethyst_pickaxe"),
        Item.Settings()
            .fireproof()
            .rarity(Rarity.EPIC)
            .pickaxe(AMETHYST_TOOL_MATERIAL, 2.0F, -2.0F)
    )

    val AMETHYST_SHOVEL = CreateItem(
        Id("amethyst_shovel"),
        { ShovelItem(AMETHYST_TOOL_MATERIAL, 3.0F, -2.4F, it) },
        Item.Settings().fireproof().rarity(Rarity.EPIC)
    )

    val AMETHYST_AXE = CreateItem(
        Id("amethyst_axe"),
        { AxeItem(AMETHYST_TOOL_MATERIAL, 8.0F, -2.4F, it) },
        Item.Settings().fireproof().rarity(Rarity.EPIC)
    )

    val AMETHYST_HOE = CreateItem(
        Id("amethyst_hoe"),
        { HoeItem(AMETHYST_TOOL_MATERIAL, 0.0F, 0.0F, it) },
        Item.Settings().fireproof().rarity(Rarity.EPIC)
    )

    // =========================================================================
    //  Initialisation
    // =========================================================================
    fun BootstrapArmourTrims(R: Registerable<ArmorTrimPattern>) {
        for (T in ALL_NGUHCRAFT_ARMOUR_TRIMS) {
            R.register(T.Trim, ArmorTrimPattern(
                T.Trim.value,
                Text.translatable(Util.createTranslationKey("trim_pattern", T.Trim.value)),
                false
            ))
        }
    }

    fun BootstrapModels(G: ItemModelGenerator) {
        fun Register(I: Item, M : Model = Models.GENERATED) {
            G.register(I, M)
        }

        Register(LOCK)
        Register(KEY)
        Register(KEY_CHAIN)
        Register(MASTER_KEY)
        Register(SLABLET_1)
        Register(SLABLET_2)
        Register(SLABLET_4)
        Register(SLABLET_8)
        Register(SLABLET_16)
        Register(SLAB_SHAVINGS_1)
        Register(SLAB_SHAVINGS_8)
        Register(NGUHROVISION_2024_DISC, Models.TEMPLATE_MUSIC_DISC)
        ALL_NGUHCRAFT_ARMOUR_TRIMS.forEach { Register(it.Template) }

        Register(AMETHYST_SWORD, Models.HANDHELD)
        Register(AMETHYST_SHOVEL, Models.HANDHELD)
        Register(AMETHYST_PICKAXE, Models.HANDHELD)
        Register(AMETHYST_AXE, Models.HANDHELD)
        Register(AMETHYST_HOE, Models.HANDHELD)

        G.registerArmor(AMETHYST_HELMET, AMETHYST_EQUIPMENT_ASSET_KEY, ItemModelGenerator.HELMET_TRIM_ID_PREFIX, false)
        G.registerArmor(AMETHYST_CHESTPLATE, AMETHYST_EQUIPMENT_ASSET_KEY, ItemModelGenerator.CHESTPLATE_TRIM_ID_PREFIX, false)
        G.registerArmor(AMETHYST_LEGGINGS, AMETHYST_EQUIPMENT_ASSET_KEY, ItemModelGenerator.LEGGINGS_TRIM_ID_PREFIX, false)
        G.registerArmor(AMETHYST_BOOTS, AMETHYST_EQUIPMENT_ASSET_KEY, ItemModelGenerator.BOOTS_TRIM_ID_PREFIX, false)
    }

    fun Init() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register {
            it.add(LOCK)
            it.add(KEY)
            it.add(KEY_CHAIN)
            it.add(MASTER_KEY)
            it.add(SLABLET_1)
            it.add(SLABLET_2)
            it.add(SLABLET_4)
            it.add(SLABLET_8)
            it.add(SLABLET_16)
            it.add(SLAB_SHAVINGS_1)
            it.add(SLAB_SHAVINGS_8)

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
            for (T in ALL_NGUHCRAFT_ARMOUR_TRIMS) it.add(T)
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

    private fun CreateItem(Id: Identifier, I: Item): Item =
        Registry.register(Registries.ITEM, Id, I)

    private fun CreateItem(Id: Identifier, I: (Item.Settings) -> Item, S: Item.Settings): Item =
        Registry.register(Registries.ITEM, Id, I(S.registryKey(Key(Id))))

    private fun CreateItem(Id: Identifier, S: Item.Settings): Item =
        Registry.register(Registries.ITEM, Id, Item(S.registryKey(Key(Id))))

    private fun CreateSmithingTemplate(S: String, I: Item.Settings): Item {
        val Id = Id(S)
        return Registry.register(
            Registries.ITEM,
            Id,
            SmithingTemplateItem.of(I.registryKey(Key(Id)))
        )
    }

    private fun Key(Id: Identifier) = RegistryKey.of(RegistryKeys.ITEM, Id)
}
