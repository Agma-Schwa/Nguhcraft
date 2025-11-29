package org.nguh.nguhcraft.item

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.fabricmc.fabric.api.registry.CompostingChanceRegistry
import net.minecraft.Util
import net.minecraft.client.data.models.ItemModelGenerators
import net.minecraft.client.data.models.model.ModelTemplate
import net.minecraft.client.data.models.model.ModelTemplates
import net.minecraft.core.Holder
import net.minecraft.core.Registry
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.data.worldgen.BootstrapContext
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.tags.TagKey
import net.minecraft.world.food.FoodProperties
import net.minecraft.world.item.*
import net.minecraft.world.item.component.Consumable
import net.minecraft.world.item.crafting.CustomRecipe.Serializer
import net.minecraft.world.item.equipment.ArmorMaterial
import net.minecraft.world.item.equipment.ArmorType
import net.minecraft.world.item.equipment.EquipmentAsset
import net.minecraft.world.item.equipment.EquipmentAssets
import net.minecraft.world.item.equipment.trim.TrimPattern
import net.minecraft.world.level.ItemLike
import net.minecraft.world.level.block.Block
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
    val TINTED_LOGS: TagKey<Item> = TagKey.create(Registries.ITEM, Id("tinted_oak_logs"))

    // =========================================================================
    //  Items
    // =========================================================================
    val LOCK: Item = CreateItem(LockItem.ID, LockItem())
    val KEY: Item = CreateItem(KeyItem.ID, KeyItem())
    val MASTER_KEY: Item = CreateItem(MasterKeyItem.ID, MasterKeyItem())
    val KEY_CHAIN: Item = CreateItem(KeyChainItem.ID, KeyChainItem())
    val SLABLET_1: Item = CreateItem(Id("slablet_1"), Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON).fireResistant())
    val SLABLET_2: Item = CreateItem(Id("slablet_2"), Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON).fireResistant())
    val SLABLET_4: Item = CreateItem(Id("slablet_4"), Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON).fireResistant())
    val SLABLET_8: Item = CreateItem(Id("slablet_8"), Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON).fireResistant())
    val SLABLET_16: Item = CreateItem(Id("slablet_16"), Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON).fireResistant())
    val SLAB_SHAVINGS_1: Item = CreateItem(Id("slab_shavings_1"), Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON).fireResistant())
    val SLAB_SHAVINGS_8: Item = CreateItem(Id("slab_shavings_8"), Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON).fireResistant())
    val NGUHROVISION_2024_DISC: Item = CreateItem(
        Id("music_disc_nguhrovision_2024"),
        Item.Properties()
            .stacksTo(1)
            .rarity(Rarity.EPIC)
            .jukeboxPlayable(RKey(Registries.JUKEBOX_SONG, "nguhrovision_2024"))
    )

    // =========================================================================
    //  Armour Trims
    // =========================================================================
    class ArmourTrim(Name: String) : ItemLike {
        val Template: Item = CreateSmithingTemplate("${Name}_armour_trim_smithing_template", Item.Properties().rarity(Rarity.RARE))
        val Trim = RKey(Registries.TRIM_PATTERN, Name)
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
    val REPAIRS_AMETHYST_ARMOUR: TagKey<Item> = TagKey.create(Registries.ITEM, Id("repairs_amethyst_armour"))

    /** Items that cannot be mined by amethyst tools. */
    val INCORRECT_FOR_AMETHYST_TOOL: TagKey<Block> = TagKey.create(Registries.BLOCK, Id("incorrect_for_amethyst_tool"))

    /** Sound events for amethyst armour. */
    val AMETHYST_ARMOUR_SOUNDS: Holder.Reference<SoundEvent> = BuiltInRegistries.SOUND_EVENT.getOrThrow(
        ResourceKey.create(Registries.SOUND_EVENT, SoundEvents.AMETHYST_CLUSTER_PLACE.location)
    )

    /** Asset key and armour material. */
    val AMETHYST_EQUIPMENT_ASSET_KEY: ResourceKey<EquipmentAsset> = ResourceKey.create(EquipmentAssets.ROOT_ID, Id("amethyst"))
    val AMETHYST_ARMOUR_MATERIAL = ArmorMaterial(
        45,
        Util.make(EnumMap(ArmorType::class.java)) {
            it[ArmorType.BOOTS] = 4
            it[ArmorType.LEGGINGS] = 8
            it[ArmorType.CHESTPLATE] = 14
            it[ArmorType.HELMET] = 4
            it[ArmorType.BODY] = 15
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
        Item.Properties()
            .fireResistant()
            .rarity(Rarity.EPIC)
            .humanoidArmor(AMETHYST_ARMOUR_MATERIAL, ArmorType.HELMET)
    )

    val AMETHYST_CHESTPLATE = CreateItem(
        Id("amethyst_chestplate"),
        Item.Properties()
            .fireResistant()
            .rarity(Rarity.EPIC)
            .humanoidArmor(AMETHYST_ARMOUR_MATERIAL, ArmorType.CHESTPLATE)
    )

    val AMETHYST_LEGGINGS = CreateItem(
        Id("amethyst_leggings"),
        Item.Properties()
            .fireResistant()
            .rarity(Rarity.EPIC)
            .humanoidArmor(AMETHYST_ARMOUR_MATERIAL, ArmorType.LEGGINGS)
    )

    val AMETHYST_BOOTS = CreateItem(
        Id("amethyst_boots"),
        Item.Properties()
            .fireResistant()
            .rarity(Rarity.EPIC)
            .humanoidArmor(AMETHYST_ARMOUR_MATERIAL, ArmorType.BOOTS)
    )

    val AMETHYST_SWORD = CreateItem(
        Id("amethyst_sword"),
        Item.Properties()
            .fireResistant()
            .rarity(Rarity.EPIC)
            .sword(AMETHYST_TOOL_MATERIAL, 5.0F, -1.2F)
    )

    val AMETHYST_PICKAXE = CreateItem(
        Id("amethyst_pickaxe"),
        Item.Properties()
            .fireResistant()
            .rarity(Rarity.EPIC)
            .pickaxe(AMETHYST_TOOL_MATERIAL, 2.0F, -2.0F)
    )

    val AMETHYST_SHOVEL = CreateItem(
        Id("amethyst_shovel"),
        { ShovelItem(AMETHYST_TOOL_MATERIAL, 3.0F, -2.4F, it) },
        Item.Properties().fireResistant().rarity(Rarity.EPIC)
    )

    val AMETHYST_AXE = CreateItem(
        Id("amethyst_axe"),
        { AxeItem(AMETHYST_TOOL_MATERIAL, 8.0F, -2.4F, it) },
        Item.Properties().fireResistant().rarity(Rarity.EPIC)
    )

    val AMETHYST_HOE = CreateItem(
        Id("amethyst_hoe"),
        { HoeItem(AMETHYST_TOOL_MATERIAL, 0.0F, 0.0F, it) },
        Item.Properties().fireResistant().rarity(Rarity.EPIC)
    )

    // =========================================================================
    //  Farming and Crops
    // =========================================================================
    var GRAPE_SEEDS = CreateItem(
        Id("grape_seeds"),
        { BlockItem(NguhBlocks.GRAPE_CROP, it) },
        Item.Properties().useItemDescriptionPrefix()
    )

    var GRAPES = CreateItem(
        Id("grapes"),
        Item.Properties().food(FoodProperties(1, 0.1F, false))
    )

    var GRAPE_LEAF = CreateItem(
        Id("grape_leaf"),
        Item.Properties()
    )

    var GRAPE_JUICE = CreateItem(
        Id("grape_juice"),
        Item.Properties()
            .craftRemainder(Items.GLASS_BOTTLE)
            .usingConvertsTo(Items.GLASS_BOTTLE)
            .stacksTo(16)
            .food(
                FoodProperties(4, 0.1F, false),
                Consumable.builder()
                    .sound(SoundEvents.GENERIC_DRINK)
                    .hasConsumeParticles(false)
                    .build()
            )
    )

    var STUFFED_GRAPE_LEAVES = CreateItem(
        Id("stuffed_grape_leaves"),
        Item.Properties().food(FoodProperties(5, 0.5F, false))
    )

    var PEANUTS = CreateItem(
        Id("peanuts"),
        { BlockItem(NguhBlocks.PEANUT_CROP, it) },
        Item.Properties()
            .food(FoodProperties(3, 0.3F, false))
            .stacksTo(64)
    )

    // =========================================================================
    //  Initialisation
    // =========================================================================
    fun BootstrapArmourTrims(R: BootstrapContext<TrimPattern>) {
        for (T in ALL_NGUHCRAFT_ARMOUR_TRIMS) {
            R.register(T.Trim, TrimPattern(
                T.Trim.location(),
                Component.translatable(Util.makeDescriptionId("trim_pattern", T.Trim.location())),
                false
            ))
        }
    }

    fun BootstrapModels(G: ItemModelGenerators) {
        fun Register(I: Item, M : ModelTemplate = ModelTemplates.FLAT_ITEM) {
            G.generateFlatItem(I, M)
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
        Register(NGUHROVISION_2024_DISC, ModelTemplates.MUSIC_DISC)
        ALL_NGUHCRAFT_ARMOUR_TRIMS.forEach { Register(it.Template) }

        Register(AMETHYST_SWORD, ModelTemplates.FLAT_HANDHELD_ITEM)
        Register(AMETHYST_SHOVEL, ModelTemplates.FLAT_HANDHELD_ITEM)
        Register(AMETHYST_PICKAXE, ModelTemplates.FLAT_HANDHELD_ITEM)
        Register(AMETHYST_AXE, ModelTemplates.FLAT_HANDHELD_ITEM)
        Register(AMETHYST_HOE, ModelTemplates.FLAT_HANDHELD_ITEM)

        G.generateTrimmableItem(AMETHYST_HELMET, AMETHYST_EQUIPMENT_ASSET_KEY, ItemModelGenerators.TRIM_PREFIX_HELMET, false)
        G.generateTrimmableItem(AMETHYST_CHESTPLATE, AMETHYST_EQUIPMENT_ASSET_KEY, ItemModelGenerators.TRIM_PREFIX_CHESTPLATE, false)
        G.generateTrimmableItem(AMETHYST_LEGGINGS, AMETHYST_EQUIPMENT_ASSET_KEY, ItemModelGenerators.TRIM_PREFIX_LEGGINGS, false)
        G.generateTrimmableItem(AMETHYST_BOOTS, AMETHYST_EQUIPMENT_ASSET_KEY, ItemModelGenerators.TRIM_PREFIX_BOOTS, false)

        Register(GRAPES)
        Register(GRAPE_LEAF)
        Register(GRAPE_JUICE)
        Register(STUFFED_GRAPE_LEAVES)
    }

    fun Init() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register {
            it.accept(LOCK)
            it.accept(KEY)
            it.accept(KEY_CHAIN)
            it.accept(MASTER_KEY)
            it.accept(SLABLET_1)
            it.accept(SLABLET_2)
            it.accept(SLABLET_4)
            it.accept(SLABLET_8)
            it.accept(SLABLET_16)
            it.accept(SLAB_SHAVINGS_1)
            it.accept(SLAB_SHAVINGS_8)

            ChestVariant.entries.forEach { CV ->
                it.accept(Utils.BuildItemStack(Items.CHEST) {
                    set(DataComponents.CUSTOM_NAME, CV.DefaultName)
                    set(NguhBlocks.CHEST_VARIANT_COMPONENT, CV)
                })
            }
        }

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register {
            it.accept(NGUHROVISION_2024_DISC)
        }

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS).register {
            for (T in ALL_NGUHCRAFT_ARMOUR_TRIMS) it.accept(T)
            it.accept(GRAPE_LEAF)
        }

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.NATURAL_BLOCKS).register {
            it.accept(GRAPE_SEEDS)
        }

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FOOD_AND_DRINKS).register {
            it.accept(GRAPES)
            it.accept(GRAPE_JUICE)
            it.accept(STUFFED_GRAPE_LEAVES)
            it.accept(PEANUTS)
        }

        KeyLockPairingRecipe.SERIALISER = Registry.register(
            BuiltInRegistries.RECIPE_SERIALIZER,
            Id("crafting_special_key_lock_pairing"),
            Serializer(::KeyLockPairingRecipe)
        )

        KeyDuplicationRecipe.SERIALISER = Registry.register(
            BuiltInRegistries.RECIPE_SERIALIZER,
            Id("crafting_special_key_duplication"),
            Serializer(::KeyDuplicationRecipe)
        )

        CompostingChanceRegistry.INSTANCE.add(GRAPES, 0.5F)
        CompostingChanceRegistry.INSTANCE.add(GRAPE_SEEDS, 0.3F)
        CompostingChanceRegistry.INSTANCE.add(GRAPE_LEAF, 0.3F)
        CompostingChanceRegistry.INSTANCE.add(PEANUTS, 0.5F)
    }

    private fun CreateItem(Id: ResourceLocation, I: Item): Item =
        Registry.register(BuiltInRegistries.ITEM, Id, I)

    private fun CreateItem(Id: ResourceLocation, I: (Item.Properties) -> Item, S: Item.Properties): Item =
        Registry.register(BuiltInRegistries.ITEM, Id, I(S.setId(Key(Id))))

    private fun CreateItem(Id: ResourceLocation, S: Item.Properties): Item =
        Registry.register(BuiltInRegistries.ITEM, Id, Item(S.setId(Key(Id))))

    private fun CreateSmithingTemplate(S: String, I: Item.Properties): Item {
        val Id = Id(S)
        return Registry.register(
            BuiltInRegistries.ITEM,
            Id,
            SmithingTemplateItem.createArmorTrimTemplate(I.setId(Key(Id)))
        )
    }

    private fun Key(Id: ResourceLocation) = ResourceKey.create(Registries.ITEM, Id)
}
